package org.andretro.settings;

import org.andretro.emulator.*;

import android.preference.*;
import android.os.*;


/**
 * PreferenceFragment which lists all ports for a particular system.
 * @author jason
 *
 */
public class InputActivity extends PreferenceActivity
{
	@Override public void onCreate(Bundle aState)
	{
		super.onCreate(aState);
		
		getPreferenceManager().setSharedPreferencesName("retropad");

		// Setup the preference list
		PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(this);
		
		// Add port 1's pad
		Doodads.Device device = Game.I.getInputs().getDevice(0,  0);
		for(Doodads.Button i: device.getAll())
		{
			screen.addPreference(new Settings.Button(this, i));
		}
		
		setPreferenceScreen(screen);
	}
	
	@Override public void onPause()
	{
		super.onPause();

		Game.I.queueCommand(new Commands.RefreshInput(null));
	}
}
