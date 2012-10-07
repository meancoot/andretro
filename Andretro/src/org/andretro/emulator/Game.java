package org.andretro.emulator;

import org.andretro.system.*;
import org.libretro.*;

import java.io.*;
import java.util.*;

import android.content.*;
import android.os.*;


public final class Game implements Runnable
{
    // Singleton
    static
    {
    	System.loadLibrary("retroiface");
    }
 
    // Thread
    private static Thread thread;
    private static final CommandQueue eventQueue = new CommandQueue();
    
    public static void queueCommand(final CommandQueue.BaseCommand aCommand)
    {
    	// Sanity
    	if(null == thread || !thread.isAlive())
    	{
    		Logger.d("No thread running, start new one");
    		thread = new Thread(new Game());
    		thread.start();
    		eventQueue.setThread(thread);
    	}
    	
		// Put the event in the queue and notify any waiting clients that it's present. (This will wake the waiting emulator if needed.)
		eventQueue.queueCommand(aCommand);
    }

    // Game Info
    
    static int pauseDepth;
    static Runnable presentNotify;
    
    // Fast forward
    private static int frameCounter;
    static int fastForwardKey;
    static int fastForwardSpeed = 1;
    static boolean fastForwardDefault;
    static int rewindKey;

    // Functions to retrieve game data, careful as the data may be null, or outdated!    
    public static Doodads.Set getInputs()
    {
    	return inputs;
    }
    
    public static String getModuleName()
    {
    	return (null == systemInfo) ? null : systemInfo.libraryName;
    }
    
    public static String getModuleSystemDirectory()
    {
    	return (null == moduleDirectory) ? null : moduleDirectory.getAbsolutePath();
    }
    
    public static String getGameDataName(String aExtension)
    {
    	return dataName + "." + aExtension;
    }
            
    // LIBRARY
    private static boolean libraryLoaded;
    private static String libraryFilename;
    private static LibRetro.SystemInfo systemInfo;
    private static String[] extensions;
    private static File moduleDirectory;
    private static Doodads.Set inputs;
    
    static boolean loadLibrary(Context aContext, String aLibrary)
    {
    	eventQueue.assertThread();
    	
    	// Don't open the library if it's already open!
    	if(!aLibrary.equals(libraryFilename))
    	{
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
				libraryFilename = aLibrary;
			}
    	}
		
		return libraryLoaded;
    }
    
    static void closeLibrary()
    {
    	eventQueue.assertThread();
    	
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
    		libraryFilename = null;
    	}    	
    }
    
    public static boolean validFile(File aFile)
    {
    	final String path = aFile.getAbsolutePath(); 
        final int dot = path.lastIndexOf(".");
        final String extension = (dot < 0) ? null : path.substring(dot + 1);
        
        // HACK: If blockExtract isn't set, don't allow zip files to be valid.
        if(!systemInfo.blockExtract && null != extension && 0 == extension.compareToIgnoreCase("zip"))
        {
        	return false;
        }

    	return (null == extension) ? false : (0 <= Arrays.binarySearch(extensions, extension));
    }
    
    public static boolean hasLibrary()
    {
    	return libraryLoaded;
    }
    
    // GAME
    private static boolean gameLoaded;
    private static LibRetro.AVInfo avInfo;
    private static String dataName;
	    
    static void loadFile(File aFile)
    {
    	eventQueue.assertThread();

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
    
    static void closeFile()
    {
    	eventQueue.assertThread();
    	
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
    
    public static boolean hasGame()
    {
        return gameLoaded;
    }

    // LOOP
    @Override public void run()
    {
    	eventQueue.assertThread();
    	
    	// Buffer for audio samples
    	final short[] audioSamples = new short[48000];
    	
    	try
    	{        	
	    	// Run until interrupted
	    	while(!thread.isInterrupted())
	    	{
	    		eventQueue.pump();
	    		
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
	    			synchronized(thread)
	    			{
	    				thread.wait();
	    			}
	    		}
	    	}
	    }
    	catch(InterruptedException e)
    	{

    	}
    }
}

