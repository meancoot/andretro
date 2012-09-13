package org.andretro.settings;
import org.andretro.R;

import org.andretro.emulator.*;
import org.andretro.settings.Settings.*;

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
        addPreferencesFromResource(R.xml.preferences);
/*
		fastForward.addPreference(new Settings.GenericButton(this, "fast_forward_key", "Key held to toggle fast forward.", KeyEvent.KEYCODE_BUTTON_R2));
		rewind.addPreference(new Settings.GenericButton(this, "rewind_key", "Key held to activate rewind.", KeyEvent.KEYCODE_BUTTON_L2));
*/
	}
	
	@Override public void onPause()
	{
		super.onPause();

		Game.I.queueCommand(new Commands.RefreshSettings(getPreferenceManager().getSharedPreferences(), null));
	}
}
