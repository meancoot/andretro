package org.andretro.emulator;
import java.io.*;

import android.content.*;
import android.view.*;

import org.andretro.system.*;
import org.libretro.LibRetro;

public final class Commands
{	
	public static final class LoadGame extends CommandQueue.BaseCommand
	{
		private final Context context;
		private final String library;
		private final File file;
		
		public LoadGame(Context aContext, String aLibrary, File aFile)
		{
			context = aContext;
			library = aLibrary;
			file = aFile;
			
			if(null == context || null == library || null == aFile)
			{
				throw new NullPointerException("Neither aContext, aLibrary nor aFile may be null");
			}
		}
		
		@Override protected void perform()
		{
			Game.loadGame(context, library, file);
		}
	}
	
	public static final class CloseGame extends CommandQueue.BaseCommand
	{
		@Override protected void perform()
		{
			Game.closeGame();
		}
	}
	
	public static final class Reset extends CommandQueue.BaseCommand
	{
	    @Override protected void perform()
	    {
	        LibRetro.reset();
	    }
	}
	
	public static final class StateAction extends CommandQueue.BaseCommand
	{
	    private final boolean load;
	    private final int slot;
	
        public StateAction(boolean aLoad, int aSlot)
        {
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
                LibRetro.unserializeFromFile(Game.getGameDataName("st" + slot));
            }
            else
            {
                LibRetro.serializeToFile(Game.getGameDataName("st" + slot));
            }
        }
	}
	
	public static final class Pause extends CommandQueue.BaseCommand
	{
		private final boolean pause;
		
		public Pause(boolean aPause)
		{
			pause = aPause;
		}
		
		@Override protected void perform()
		{
			if(0 == Game.pauseDepth && !pause)
			{
				throw new RuntimeException("Internal Error: Emulator was unpaused too many times (Please Report).");
			}
			
			Game.pauseDepth += pause ? 1 : -1;			
		}
	}
	
	public static final class SetPresentNotify extends CommandQueue.BaseCommand
	{
		private final Runnable presentNotify;
		
		public SetPresentNotify(Runnable aPresentNotify)
		{
			presentNotify = aPresentNotify;
		}
		
		@Override protected void perform()
		{
			Game.presentNotify = presentNotify;
		}
	}
	
	public static final class RefreshInput extends CommandQueue.BaseCommand
	{
		@Override protected void perform()
		{
			// TODO
			LibRetro.setControllerPortDevice(0, LibRetro.RETRO_DEVICE_JOYPAD);
		}
	}
	
	public static final class RefreshSettings extends CommandQueue.BaseCommand
	{
		private final SharedPreferences settings;
		
		public RefreshSettings(SharedPreferences aSettings)
		{	
			settings = aSettings;
			
			if(null == aSettings)
			{
				throw new RuntimeException("aSettings may not be null.");
			}
		}
		
		@Override protected void perform()
		{
			// Scaling
			Present.setSmoothingMode(settings.getBoolean("scaling_smooth", true));
			
			final String aspectMode = settings.getString("scaling_aspect_mode", "Default");
			if("Default".equals(aspectMode))
			{
				Present.setForcedAspect(false, 0.0f);
			}
			else if("4:3".equals(aspectMode))
			{
				Present.setForcedAspect(true, 1.3333333f);
			}
			else if("Pixel".equals(aspectMode))
			{
				Present.setForcedAspect(true, -1.0f);
			}
			else
			{
				Present.setForcedAspect(false, 0.0f);
			}
			
			// Fast forward
			Game.fastForwardDefault = settings.getBoolean("fast_forward_default", false);
			Game.fastForwardSpeed = Integer.parseInt(settings.getString("fast_forward_speed", "4"));
			Game.fastForwardKey = settings.getInt("fast_forward_key", KeyEvent.KEYCODE_BUTTON_R2);
			
			// Rewind
			final boolean rewindEnabled = settings.getBoolean("rewind_enabled", false);
			final int rewindDataSize = Integer.parseInt(settings.getString("rewind_buffer_size", "16")) * 1024 * 1024;
			LibRetro.setupRewinder(rewindEnabled ? rewindDataSize : 0);
			Game.rewindKey = settings.getInt("rewind_key", KeyEvent.KEYCODE_BUTTON_L2);
		}
	}
}
