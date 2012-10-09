package org.andretro.emulator;

import org.andretro.system.*;
import org.libretro.*;

import java.io.*;

import android.content.*;

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
        
    public static String getGameDataName(String aExtension)
    {
    	return dataName + "." + aExtension;
    }
            
    // LIBRARY
    private static ModuleInfo moduleInfo;
    private static boolean gameLoaded;
    private static boolean gameClosed;
    private static LibRetro.SystemInfo systemInfo = new LibRetro.SystemInfo();
    private static LibRetro.AVInfo avInfo = new LibRetro.AVInfo();
    private static Doodads.Set inputs;
    private static String dataName;

    static void loadGame(Context aContext, String aLibrary, File aFile)
    {
    	eventQueue.assertThread();
    	
    	if(!gameLoaded && !gameClosed && null != aFile && aFile.isFile())
    	{
    		moduleInfo = ModuleInfo.getInfoAbout(aContext.getAssets(), new File(aLibrary));
    		
    		if(LibRetro.loadLibrary(aLibrary))
    		{
    			LibRetro.init();
    			
    			if(LibRetro.loadGame(aFile.getAbsolutePath()))
    			{
    				// System info
    				LibRetro.getSystemInfo(systemInfo);
    				LibRetro.getSystemAVInfo(avInfo);

    				inputs = new Doodads.Set(aContext.getSharedPreferences("retropad", 0), moduleInfo.getDataName(), moduleInfo.getInputData());
    				
    				// Filesystem stuff    				
    	        	dataName = moduleInfo.getDataPath() + "/" + aFile.getName().split("\\.(?=[^\\.]+$)")[0];
    	        	LibRetro.readMemoryRegion(LibRetro.RETRO_MEMORY_SAVE_RAM, getGameDataName("srm"));
    	        	LibRetro.readMemoryRegion(LibRetro.RETRO_MEMORY_RTC, getGameDataName("rtc"));

    	        	// Load settings
    				new Commands.RefreshSettings(aContext.getSharedPreferences(moduleInfo.getDataName(), 0)).run();
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
        	LibRetro.writeMemoryRegion(LibRetro.RETRO_MEMORY_SAVE_RAM, getGameDataName("srm"));
        	LibRetro.writeMemoryRegion(LibRetro.RETRO_MEMORY_RTC, getGameDataName("rtc"));

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

