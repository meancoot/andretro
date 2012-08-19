package org.andretro.emulator;

import java.io.*;
import android.app.*;
import android.content.*;
import org.libretro.LibRetro;

public final class Commands
{
    public static class Callback
    {
        private final Runnable callback;
        private final Activity activity;
        
        public Callback(Activity aActivity, Runnable aCallback)
        {
            callback = aCallback;
            activity = aActivity;
            
            if(null == callback || null == activity)
            {
                throw new RuntimeException("Neither aCallback nor aActivity may be null.");
            }
        }
        
        public void perform()
        {
            activity.runOnUiThread(callback);
        }
    }

	static abstract class BaseCommand implements Runnable
	{
		private final Callback finishedCallback;
		
		BaseCommand(Callback aFinished)
		{
			finishedCallback = aFinished;
		}
		
		@Override public final void run()
		{
		    if(Thread.currentThread() != Game.I)
		    {
		        throw new RuntimeException("Emulator Commands must only be run from the emulator thread.");
		    }
		
			perform();
			
			if(null != finishedCallback)
			{
				finishedCallback.perform();
			}
		}
		
		abstract protected void perform();
	}
	
	public static final class Initialize extends BaseCommand
	{
		private final Context context;
		private final String library;
		
		public Initialize(Context aContext, String aLibrary, Callback aCallback)
		{
			super(aCallback);
			context = aContext;
			library = aLibrary;
			
			if(null == context || null == library)
			{
				throw new NullPointerException("Neither aContext nor aLibrary may be null");
			}
		}
		
		@Override protected void perform()
		{
			Game.I.loadLibrary(context, library);
		}
	}
	
	public static final class ShutDown extends BaseCommand
	{
		public ShutDown(Callback aCallback)
		{
			super(aCallback);
		}
		
		@Override protected void perform()
		{
			Game.I.closeLibrary();
		}
	}
	
	public static final class LoadGame extends BaseCommand
	{
		private final String path;
		
		public LoadGame(String aPath, Callback aCallback)
		{
			super(aCallback);
			
			path = aPath;
		}
		
		@Override protected void perform()
		{
            // Check file
            if(null == path || !new File(path).isFile())
            {
                throw new IllegalArgumentException("File not found.");
            }

            // Load
            if(LibRetro.loadGame(path))
            {	
                LibRetro.getSystemAVInfo(Game.I.avInfo);
                Game.I.isAlive = true;
                
                new RefreshInput(null).run();
            }
            else
            {
                throw new RuntimeException("Failed to load game.");
            }			
		}
	}
	
	public static final class CloseGame extends BaseCommand
	{
		public CloseGame(Callback aCallback)
		{
			super(aCallback);
		}
		
		@Override protected void perform()
		{
			LibRetro.unloadGame();
			Game.I.isAlive = false;
		}
	}
	
	public static final class Reset extends BaseCommand
	{
	    public Reset(Callback aCallback)
	    {
	        super(aCallback);
	    }
	    
	    @Override protected void perform()
	    {
	        LibRetro.reset();
	    }
	}
	
	public static final class StateAction extends BaseCommand
	{
	    private final boolean load;
	    private final int slot;
	
        public StateAction(boolean aLoad, int aSlot, Callback aCallback)
        {
            super(aCallback);
            load = aLoad;
            
            slot = aSlot;
            if(slot < 0 || slot > 9)
            {
                throw new RuntimeException("Slot must be in the range of 0-9");
            }
        }
        
        @Override protected void perform()
        {
            if(load)
            {
                LibRetro.unserializeFromFile(Game.I.getGameDataName("st" + slot));
            }
            else
            {
                LibRetro.serializeToFile(Game.I.getGameDataName("st" + slot));
            }
        }
	}
	
	public static final class Pause extends BaseCommand
	{
		private final boolean pause;
		
		public Pause(boolean aPause, Callback aCallback)
		{
			super(aCallback);
			pause = aPause;
		}
		
		@Override protected void perform()
		{
			if(0 == Game.I.pauseDepth && !pause)
			{
				throw new RuntimeException("Internal Error: Emulator was unpaused too many times (Please Report).");
			}
			
			Game.I.pauseDepth += pause ? 1 : -1;			
		}
	}
	
	public static final class SetPrepresent extends BaseCommand
	{
		private final Runnable prePresent;
		
		public SetPrepresent(Runnable aPrePresent, Callback aCallback)
		{
			super(aCallback);
			prePresent = aPrePresent;
		}
		
		@Override protected void perform()
		{
			Game.I.prePresent = prePresent;
		}
	}
	
	public static final class RefreshInput extends BaseCommand
	{
		public RefreshInput(Callback aCallback)
		{
			super(aCallback);
		}
		
		@Override protected void perform()
		{
			// TODO
			LibRetro.setControllerPortDevice(0, LibRetro.RETRO_DEVICE_JOYPAD);
		}
	}
}
