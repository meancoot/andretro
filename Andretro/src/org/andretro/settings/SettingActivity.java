package org.andretro.settings;
import org.andretro.R;

import org.andretro.emulator.*;

import android.preference.*;
import android.os.*;

@SuppressWarnings("deprecation")
public class SettingActivity extends PreferenceActivity
{
	@Override public void onCreate(Bundle aState)
	{
		super.onCreate(aState);
		
		if(!Game.hasLibrary())
		{
			throw new RuntimeException("No library is loaded");
		}
		
		getPreferenceManager().setSharedPreferencesName(Game.getModuleName());
        addPreferencesFromResource(R.xml.preferences);
/*
		fastForward.addPreference(new Settings.GenericButton(this, "fast_forward_key", "Key held to toggle fast forward.", KeyEvent.KEYCODE_BUTTON_R2));
		rewind.addPreference(new Settings.GenericButton(this, "rewind_key", "Key held to activate rewind.", KeyEvent.KEYCODE_BUTTON_L2));
*/
	}
	
	@Override public void onPause()
	{
		super.onPause();

		Game.queueCommand(new Commands.RefreshSettings(getPreferenceManager().getSharedPreferences(), null));
	}
}
