package org.andretro.emulator;

import org.andretro.system.*;
import org.libretro.*;

import java.io.*;

import android.app.*;

public final class Game implements Runnable
{
    // Singleton
    static
    {
    	System.loadLibrary("retroiface");
    	
    	if(!LibRetro.nativeInit())
    	{
    		throw new RuntimeException("Failed to initialize JNI classes.");
    	}
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
    static String screenShotName;

    // Functions to retrieve game data, careful as the data may be null, or outdated!            
    public static String getGameDataName(String aSubDirectory, String aExtension)
    {
    	final File dir = new File(moduleInfo.getDataPath() + "/" + aSubDirectory);
    	if(!dir.exists() && !dir.mkdirs())
    	{
    		throw new RuntimeException("Failed to make data directory");
    	}
    	
    	return moduleInfo.getDataPath() + "/" + aSubDirectory + "/" + dataName + "." + aExtension;
    }
            
    // LIBRARY
    static Activity loadingActivity;
    private static ModuleInfo moduleInfo;
    private static boolean gameLoaded;
    private static boolean gameClosed;
    private static LibRetro.SystemInfo systemInfo = new LibRetro.SystemInfo();
    private static LibRetro.AVInfo avInfo = new LibRetro.AVInfo();
    private static String dataName;

    static void loadGame(Activity aActivity, String aLibrary, File aFile)
    {
    	eventQueue.assertThread();
    	
    	if(!gameLoaded && !gameClosed && null != aFile && aFile.isFile())
    	{
    		moduleInfo = ModuleInfo.getInfoAbout(aActivity, new File(aLibrary));
    		
    		if(LibRetro.loadLibrary(aLibrary, moduleInfo.getDataPath()))
    		{
    			LibRetro.init();
    			
    			if(LibRetro.loadGame(aFile.getAbsolutePath()))
    			{
    				// System info
    				LibRetro.getSystemInfo(systemInfo);
    				LibRetro.getSystemAVInfo(avInfo);
    				
    				// Filesystem stuff    				
    	        	dataName = aFile.getName().split("\\.(?=[^\\.]+$)")[0];
    	        	LibRetro.readMemoryRegion(LibRetro.RETRO_MEMORY_SAVE_RAM, getGameDataName("SaveRAM", "srm"));
    	        	LibRetro.readMemoryRegion(LibRetro.RETRO_MEMORY_RTC, getGameDataName("SaveRAM", "rtc"));

    	        	// Load settings
    	        	loadingActivity = aActivity;
    				new Commands.RefreshSettings(aActivity.getSharedPreferences(moduleInfo.getDataName(), 0)).run();
    	            new Commands.RefreshInput().run();
    	            
    	            gameLoaded = true;
    			}
    		}
    	}
    	else
    	{
    		gameLoaded = true;
    		gameClosed = true;
    		throw new RuntimeException("Failed to load game");
    	}
    }
    
    static void closeGame()
    {
    	eventQueue.assertThread();
    	
    	if(gameLoaded && !gameClosed)
    	{
        	LibRetro.writeMemoryRegion(LibRetro.RETRO_MEMORY_SAVE_RAM, getGameDataName("SaveRAM", "srm"));
        	LibRetro.writeMemoryRegion(LibRetro.RETRO_MEMORY_RTC, getGameDataName("SaveRAM", "rtc"));

   			LibRetro.unloadGame();
    		LibRetro.deinit();
    		LibRetro.unloadLibrary();
   			
    		// Shutdown any audio device
    		Audio.close();
    		
    		gameClosed = true;
    	}    	
    }
        
    public static boolean hasGame()
    {
        return gameLoaded && !gameClosed;
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
	    		if(gameLoaded && !gameClosed && 0 == pauseDepth && null != presentNotify)
	    		{	
	    			// Fast forward
	    			final boolean rewindKeyPressed = Input.isPressed(rewindKey);
	    			final boolean fastKeyPressed = Input.isPressed(fastForwardKey);
	    			final int frameToggle = fastForwardDefault ? 1 : fastForwardSpeed;
	    			int frameTarget = fastForwardDefault ? fastForwardSpeed : 1;
	    			frameTarget = (fastKeyPressed) ? frameToggle : frameTarget;
	    				    				    			
	                //Emulate   			
	    			LibRetro.VideoFrame frame = Present.FrameQueue.getEmpty();
    				int len = LibRetro.run(frame, audioSamples, Input.getBits(moduleInfo.inputData.getDevice(0, 0)), rewindKeyPressed);
    				
    				// Write any pending screen shots
    				if(null != screenShotName)
    				{
    					PngWriter.write(screenShotName, frame.pixels, frame.width, frame.height, frame.pixelFormat);
    					screenShotName = null;
    				}
    				
    				// Present
    				if(++frameCounter >= frameTarget)
    				{
    					Present.FrameQueue.putFull(frame);
    					presentNotify.run();
    					
        				if(0 != len)
        				{
        					Audio.write((int)avInfo.sampleRate, audioSamples, len);
        				}
        				
        				frameCounter = 0;
    				}
    				else
    				{
    					Present.FrameQueue.putEmpty(frame);
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

