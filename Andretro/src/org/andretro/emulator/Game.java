package org.andretro.emulator;
import org.andretro.system.*;
import org.libretro.*;

import java.io.*;
import java.util.concurrent.*;

import android.content.*;
import android.os.*;


public final class Game extends Thread
{
    // Singleton
    public final static Game I = new Game();
    private Game()
    {
    	System.loadLibrary("retro_wrap");
    }
 
    // Game Info
    final LibRetro.SystemInfo systemInfo = new LibRetro.SystemInfo();
    final LibRetro.AVInfo avInfo = new LibRetro.AVInfo();
    
    // Package private
    int pauseDepth;
    volatile boolean isAlive;
    Runnable prePresent;
    File moduleDirectory;
    
    public String getModuleName()
    {
    	return systemInfo.libraryName;
    }
    
    public String getGameDataName(String aExtension)
    {
        return moduleDirectory.getAbsolutePath() + "/test." + aExtension;
    }
    
    // Input
    Doodads.Set inputs;
    public Doodads.Set getInputs()
    {
    	return inputs;
    }
    
    // Data
    private boolean initialized = false;
    
    public synchronized static boolean initialize(Context aContext, String aLibrary)
    {
    	if(!I.initialized)
    	{
    		if(!LibRetro.loadLibrary(aLibrary))
    		{
    			return false;
    		}
    		
    		LibRetro.init();
    		LibRetro.getSystemInfo(Game.I.systemInfo);
    		
    		Game.I.moduleDirectory = new File(Environment.getExternalStorageDirectory().getPath() + "/andretro/" + Game.I.systemInfo.libraryName);
    		Game.I.moduleDirectory.mkdirs();
    		
    		Game.I.inputs = new Doodads.Set(aContext.getSharedPreferences(Game.I.systemInfo.libraryName, 0));
    		
	    	// Go
	    	I.start();
    	}
    	
    	I.initialized = true;
    	return true;
    }    
    
    // Game data
    
    public boolean isValid()
    {
        return isAlive;
    }
        
    // Main thread function
    @Override public void run()
    {
    	// Sanity
    	if(!initialized)
    	{
    		throw new RuntimeException("Game thead must be started via Game.initialize()");
    	}
    	
    	if(Thread.currentThread() != I)
    	{
    		throw new RuntimeException("Game's run method must only be invoked by its thread.");
    	}
    	
    	// Buffer for audio samples
    	final short[] audioSamples = new short[48000];
    	
    	try
    	{        	
	    	// Run until interrupted
	    	while(!isInterrupted())
	    	{
	    		pumpEventQueue();
	    		
	    		// Execute any commands
	    		if(isAlive && 0 == pauseDepth && null != prePresent)
	    		{
	    			prePresent.run();
	
	                //Emulate
	    			Present.VideoFrame frame = Present.getFrameBuffer();
    				int len = LibRetro.run(frame.pixels, frame.size, audioSamples, Input.getBits(inputs.getDevice(0, 0)));
    				Present.putNextBuffer(frame);
    				
    				Audio.write((int)avInfo.sampleRate, audioSamples, len);
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
	    
    // Command queue
    private final BlockingQueue<Commands.BaseCommand> eventQueue = new ArrayBlockingQueue<Commands.BaseCommand>(8);

    private void pumpEventQueue()
    {
    	// Sanity
    	if(Thread.currentThread() != I)
    	{
    		throw new RuntimeException("pumpEventQueue must be called only by the Game thread.");
    	}

    	// Run all events
    	for(Commands.BaseCommand i = eventQueue.poll(); null != i; i = eventQueue.poll())
    	{
    		i.run();
    	}
    }
    
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
}

