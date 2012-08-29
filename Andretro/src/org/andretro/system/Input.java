package org.andretro.system;

import org.andretro.input.view.*;
import android.view.*;
import java.util.*;

import org.andretro.emulator.Doodads;

/**
 * Static class to manage input state for the emulator view.
 * @author jason
 *
 */
public final class Input
{
	private static Set<Integer> keys = new HashSet<Integer>();
	private static InputHandler onscreenInput;
	
    // Called by UI
	public synchronized static void processEvent(KeyEvent aEvent)
	{
		if(aEvent.getAction() == KeyEvent.ACTION_DOWN)
		{
			keys.add(aEvent.getKeyCode());
		}
		else if(aEvent.getAction() == KeyEvent.ACTION_UP)
		{
			keys.remove(aEvent.getKeyCode());
		}
	}
	
	public synchronized static void clear()
	{
		keys.clear();
	}

	public synchronized static void setOnScreenInput(InputHandler aInput)
	{
		onscreenInput = aInput;
	}
	
	// Called by emulator
	public static synchronized int getBits(Doodads.Device aDevice)
	{
		int result = 0;
		
		if(null != onscreenInput)
		{
			result |= onscreenInput.getBits();
		}
		
		for(Doodads.Button i: aDevice.getAll())
		{
			if(keys.contains(i.getKeyCode()))
			{
				result |= (1 << i.bitOffset);
			}	
		}
		
		return result;
	}
}
