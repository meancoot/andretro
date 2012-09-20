package org.andretro.emulator;

import org.andretro.system.*;
import org.libretro.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import android.content.*;
import android.os.*;


public final class Game extends Thread
{
    // Singleton
    public final static Game I = new Game();
    private Game()
    {
    	System.loadLibrary("retroiface");
    	start();
    }
 
    // Thread
    private void assertThread()
    {
    	if(Thread.currentThread() != I)
    	{
    		throw new RuntimeException("Parent function must only be called on the Game thread.");
    	}
    }

    private void assertNotThread()
    {
    	if(Thread.currentThread() == I)
    	{
    		throw new RuntimeException("Parent function must not be called on the Game thread.");
    	}
    }
    
    // Command queue
    private final BlockingQueue<Commands.BaseCommand> eventQueue = new ArrayBlockingQueue<Commands.BaseCommand>(8);
    
    public void queueCommand(final Commands.BaseCommand aCommand)
    {
    	assertNotThread();
    	
    	// Sanity
    	if(!isAlive())
    	{
    		throw new RuntimeException("Game thread has already exited.");
    	}
    	
    	if(null == aCommand)
    	{
    		throw new NullPointerException("Command may not be null.");
    	}
    	
		// Put the event in the queue and notify any waiting clients that it's present. (This will wake the waiting emulator if needed.)
		eventQueue.add(aCommand);
		
		synchronized(this)
		{
			this.notifyAll();
		}
    }

    private void pumpEventQueue()
    {
    	assertThread();    	

    	// Run all events
    	for(Commands.BaseCommand i = eventQueue.poll(); null != i; i = eventQueue.poll())
    	{
    		i.run();
    	}
    }

    // Game Info
    
    int pauseDepth;
    Runnable presentNotify;
    
    // Fast forward
    private int frameCounter;
    int fastForwardKey;
    int fastForwardSpeed = 1;
    boolean fastForwardDefault;
    int rewindKey;

    // Functions to retrieve game data, careful as the data may be null, or outdated!    
    public Doodads.Set getInputs()
    {
    	return inputs;
    }
    
    public String getModuleName()
    {
    	return (null == systemInfo) ? null : systemInfo.libraryName;
    }
    
    public String getModuleSystemDirectory()
    {
    	return (null == moduleDirectory) ? null : moduleDirectory.getAbsolutePath();
    }
    
    public String getGameDataName(String aExtension)
    {
    	return dataName + "." + aExtension;
    }
            
    // LIBRARY
    private boolean libraryLoaded;
    private LibRetro.SystemInfo systemInfo;
    private String[] extensions;
    private File moduleDirectory;
    private Doodads.Set inputs;
    
    boolean loadLibrary(Context aContext, String aLibrary)
    {
    	assertThread();
    	
    	closeLibrary();
    	
		if(LibRetro.loadLibrary(aLibrary))
		{
			LibRetro.init();
			
			systemInfo = new LibRetro.SystemInfo();
			LibRetro.getSystemInfo(systemInfo);
			
			extensions = systemInfo.validExtensions.split("\\|");
			Arrays.sort(extensions);
			
			moduleDirectory = new File(Environment.getExternalStorageDirectory().getPath() + "/andretro/" + systemInfo.libraryName);
			moduleDirectory.mkdirs();
			
			new File(moduleDirectory.getAbsolutePath() + "/Games").mkdirs();
			
			inputs = new Doodads.Set(aContext.getSharedPreferences("retropad", 0));
			new Commands.RefreshSettings(aContext.getSharedPreferences(getModuleName(), 0), null).run();
			
			libraryLoaded = true;
		}
		
		return libraryLoaded;
    }
    
    void closeLibrary()
    {
    	assertThread();
    	
    	if(libraryLoaded)
    	{
    		closeFile();
    		
    		LibRetro.deinit();
    		LibRetro.unloadLibrary();
    		
    		pauseDepth = 0;
    		systemInfo = null;
    		moduleDirectory = null;
    		inputs = null;
    		libraryLoaded = false;
    	}    	
    }
    
    public boolean validFile(File aFile)
    {
    	final String path = aFile.getAbsolutePath(); 
        final int dot = path.lastIndexOf(".");
        return (dot < 0) ? false : (0 <= Arrays.binarySearch(extensions, path.substring(dot + 1)));
    }
    
    public boolean hasLibrary()
    {
    	return libraryLoaded;
    }
    
    // GAME
    private boolean gameLoaded;
    private LibRetro.AVInfo avInfo;
    private String dataName;
	    
    void loadFile(File aFile)
    {
    	assertThread();

    	if(!libraryLoaded)
    	{
    		throw new RuntimeException("Library isn't loaded");
    	}
    	
        // Unload
    	closeFile();
        
        // Load
        if(null != aFile && aFile.isFile() && LibRetro.loadGame(aFile.getAbsolutePath()))
        {	
        	dataName = getModuleSystemDirectory() + "/" + aFile.getName().split("\\.(?=[^\\.]+$)")[0];
        	
        	LibRetro.readMemoryRegion(LibRetro.RETRO_MEMORY_SAVE_RAM, getGameDataName("srm"));
        	LibRetro.readMemoryRegion(LibRetro.RETRO_MEMORY_RTC, getGameDataName("rtc"));
        	
        	avInfo = new LibRetro.AVInfo();
            LibRetro.getSystemAVInfo(avInfo);

            new Commands.RefreshInput(null).run();
            
            gameLoaded = true;
        }
        else
        {
            throw new RuntimeException("Failed to load game.");
        }
    }
    
    void closeFile()
    {
    	assertThread();
    	
    	if(!libraryLoaded)
    	{
    		throw new RuntimeException("Library isn't loaded");
    	}

    	
    	if(gameLoaded)
    	{
        	LibRetro.writeMemoryRegion(LibRetro.RETRO_MEMORY_SAVE_RAM, getGameDataName("srm"));
        	LibRetro.writeMemoryRegion(LibRetro.RETRO_MEMORY_RTC, getGameDataName("rtc"));

   			LibRetro.unloadGame();
			
			avInfo = null;
    		gameLoaded = false;
    		
    		// Shutdown any audio device
    		Audio.close();
    	}
    }
    
    public boolean hasGame()
    {
        return gameLoaded;
    }

    // LOOP
    @Override public void run()
    {
    	assertThread();
    	
    	// Buffer for audio samples
    	final short[] audioSamples = new short[48000];
    	
    	try
    	{        	
	    	// Run until interrupted
	    	while(!isInterrupted())
	    	{
	    		pumpEventQueue();
	    		
	    		// Execute any commands
	    		if(gameLoaded && 0 == pauseDepth && null != presentNotify)
	    		{	
	    			// Fast forward
	    			final boolean rewindKeyPressed = Input.isPressed(rewindKey);
	    			final boolean fastKeyPressed = Input.isPressed(fastForwardKey);
	    			final int frameToggle = fastForwardDefault ? 1 : fastForwardSpeed;
	    			int frameTarget = fastForwardDefault ? fastForwardSpeed : 1;
	    			frameTarget = (fastKeyPressed) ? frameToggle : frameTarget;
	    				    				    			
	                //Emulate   			
	    			Present.VideoFrame frame = Present.getFrameBuffer();
    				int len = LibRetro.run(frame.pixels, frame.size, audioSamples, Input.getBits(inputs.getDevice(0, 0)), rewindKeyPressed);
    				
    				if((++frameCounter >= frameTarget) && 0 != frame.size[0] && 0 != frame.size[1])
    				{
    	    			frame.aspect = avInfo.aspectRatio;
    					Present.putNextBuffer(frame);
    					presentNotify.run();
    					
        				if(0 != len)
        				{
        					Audio.write((int)avInfo.sampleRate, audioSamples, len);
        				}
        				
        				frameCounter = 0;
    				}
    				else
    				{
    					Present.cancel(frame);
    				}
   	    		}
	    		else
	    		{
	    			synchronized(this)
	    			{
    					this.wait();
	    			}
	    		}
	    	}
	    }
    	catch(InterruptedException e)
    	{

    	}
    }
}

