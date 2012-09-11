package org.andretro.emulator;

import org.andretro.system.*;
import org.libretro.*;

import java.io.*;
import java.util.concurrent.*;

import android.content.*;
import android.os.*;
import android.view.*;


public final class Game extends Thread
{
    // Singleton
    public final static Game I = new Game();
    private Game()
    {
    	System.loadLibrary("retroiface");
    	start();
    }
 
    // Game Info
    private final BlockingQueue<Commands.BaseCommand> eventQueue = new ArrayBlockingQueue<Commands.BaseCommand>(8);    
    
    private LibRetro.SystemInfo systemInfo;
    private LibRetro.AVInfo avInfo;    
    
    int pauseDepth;
    Runnable presentNotify;
    private File moduleDirectory;
    private String dataName;
    private Doodads.Set inputs;
    
    // Fast forward
    private int frameCounter;
    int fastForwardKey;
    int fastForwardSpeed = 1;
    boolean fastForwardDefault;
    
    private boolean initialized = false;
    private boolean loaded = false;
    
    public void queueCommand(final Commands.BaseCommand aCommand)
    {
    	// Sanity
    	if(Thread.currentThread() == I)
    	{
    		throw new RuntimeException("The Game thread must not place objects in the command queue.");
    	}

    	if(null == aCommand)
    	{
    		throw new NullPointerException("Command may not be null.");
    	}
    	
		// Put the event in the queue and notify any waiting clients that it's present. (This will wake the waiting emulator if needed.)
		try
		{
			eventQueue.put(aCommand);
			
			synchronized(this)
			{
				this.notifyAll();
			}
		}
		catch(InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
    }

    // Functions to retrieve game data, careful as the data may be null, or outdated!
    public boolean isInitiailized()
    {
    	return initialized;
    }
    
    public boolean isRunning()
    {
        return loaded;
    }

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
            
    /*
     * PRIVATE INTERFACE; ONLY CALL LOCALLY OR FROM Cammands.*
     */
    private void assertThread()
    {
    	if(Thread.currentThread() != I)
    	{
    		throw new RuntimeException("Parent function must only be called on the Game thread.");
    	}
    }
    
    boolean loadLibrary(Context aContext, String aLibrary)
    {
    	assertThread();
    	// Sanity
    	if(initialized)
    	{
    		throw new RuntimeException("Game module is already loaded");
    	}
    	
		if(LibRetro.loadLibrary(aLibrary))
		{
			LibRetro.init();
			
			systemInfo = new LibRetro.SystemInfo();
			LibRetro.getSystemInfo(systemInfo);
			
			moduleDirectory = new File(Environment.getExternalStorageDirectory().getPath() + "/andretro/" + systemInfo.libraryName);
			moduleDirectory.mkdirs();
			
			new File(moduleDirectory.getAbsolutePath() + "/Games").mkdirs();
			
			inputs = new Doodads.Set(aContext.getSharedPreferences("retropad", 0));
			
			initialized = true;
		}
		
		return initialized;
    }
    
    void closeLibrary()
    {
    	assertThread();
    	
    	if(initialized)
    	{
    		closeFile();
    		
    		LibRetro.unloadGame();
    		LibRetro.deinit();
    		LibRetro.unloadLibrary();
    		
    		pauseDepth = 0;
    		systemInfo = null;
    		moduleDirectory = null;
    		inputs = null;
    		initialized = false;
    	}    	
    }
	    
    void loadFile(String aFile)
    {
    	assertThread();
    	
    	File loadedFile = new File(aFile);
    	
        // Check file
        if(null == aFile || !loadedFile.isFile())
        {
            throw new IllegalArgumentException("File not found.");
        }

        // Unload
        if(loaded)
        {
        	closeFile();
        }
        
        // Load
        if(LibRetro.loadGame(aFile))
        {	
        	dataName = getModuleSystemDirectory() + "/" + loadedFile.getName().split("\\.(?=[^\\.]+$)")[0];
        	
        	LibRetro.readMemoryRegion(LibRetro.RETRO_MEMORY_SAVE_RAM, getGameDataName("srm"));
        	LibRetro.readMemoryRegion(LibRetro.RETRO_MEMORY_RTC, getGameDataName("rtc"));
        	
        	avInfo = new LibRetro.AVInfo();
            LibRetro.getSystemAVInfo(avInfo);

            new Commands.RefreshInput(null).run();            
            
            loaded = true;
        }
        else
        {
            throw new RuntimeException("Failed to load game.");
        }
    }
    
    void closeFile()
    {
    	assertThread();
    	
    	if(loaded)
    	{
        	LibRetro.writeMemoryRegion(LibRetro.RETRO_MEMORY_SAVE_RAM, getGameDataName("srm"));
        	LibRetro.writeMemoryRegion(LibRetro.RETRO_MEMORY_RTC, getGameDataName("rtc"));

   			LibRetro.unloadGame();
			
			avInfo = null;
    		loaded = false;
    		
    		// Shutdown any audio device
    		Audio.close();
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
	    		if(loaded && 0 == pauseDepth && null != presentNotify)
	    		{	
	    			// Fast forward
	    			final boolean fastKeyPressed = Input.isPressed(fastForwardKey);
	    			final int frameToggle = fastForwardDefault ? 1 : fastForwardSpeed;
	    			int frameTarget = fastForwardDefault ? fastForwardSpeed : 1;
	    			frameTarget = (fastKeyPressed) ? frameToggle : frameTarget;
	    			
	                //Emulate	    			
	    			Present.VideoFrame frame = Present.getFrameBuffer();
    				int len = LibRetro.run(frame.pixels, frame.size, audioSamples, Input.getBits(inputs.getDevice(0, 0)));
    				
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

