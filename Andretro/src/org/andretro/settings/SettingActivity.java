package org.andretro.settings;

import org.andretro.emulator.*;

import android.preference.*;
import android.view.*;
import android.os.*;

public class SettingActivity extends PreferenceActivity
{
	@Override public void onCreate(Bundle aState)
	{
		super.onCreate(aState);
		
		if(!Game.I.isRunning())
		{
			throw new RuntimeException("No Game is loaded");
		}
		
		getPreferenceManager().setSharedPreferencesName(Game.I.getModuleName());

		// Setup the preference list
		PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(this);
		
		// Add fast forward setting
		PreferenceCategory fastForward = new PreferenceCategory(this);
		fastForward.setTitle("Fast Forward");
		screen.addPreference(fastForward);
		fastForward.addPreference(new Settings.Text(this, "fast_forward_speed", "Fast Forward Speed", "Rate for fastforward mode.", "4"));
		fastForward.addPreference(new Settings.Boolean(this, "fast_forward_default", "Use fast forward mode by default", "", false));
		fastForward.addPreference(new Settings.GenericButton(this, "fast_forward_key", "Key held to toggle fast forward.", KeyEvent.KEYCODE_BUTTON_R2));
		
		setPreferenceScreen(screen);
	}
	
	@Override public void onPause()
	{
		super.onPause();

		Game.I.queueCommand(new Commands.RefreshSettings(getPreferenceManager().getSharedPreferences(), null));
	}
}
