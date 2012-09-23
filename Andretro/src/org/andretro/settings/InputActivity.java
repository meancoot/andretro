package org.andretro.settings;

import org.andretro.emulator.*;

import android.preference.*;
import android.os.*;

@SuppressWarnings("deprecation")
public class InputActivity extends PreferenceActivity
{
	@Override public void onCreate(Bundle aState)
	{
		super.onCreate(aState);
		
		getPreferenceManager().setSharedPreferencesName("retropad");

		// Setup the preference list
		PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(this);
		
		// Add port 1's pad
		Doodads.Device device = Game.getInputs().getDevice(0,  0);
		for(Doodads.Button i: device.getAll())
		{
			screen.addPreference(new Settings.Button(this, i));
		}
		
		setPreferenceScreen(screen);
	}
	
	@Override public void onPause()
	{
		super.onPause();

		Game.queueCommand(new Commands.RefreshInput(null));
	}
}
