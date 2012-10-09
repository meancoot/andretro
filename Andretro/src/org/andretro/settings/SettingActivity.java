package org.andretro.settings;

import org.andretro.*;
import org.andretro.emulator.*;

import android.preference.*;
import android.os.*;

import java.io.*;

@SuppressWarnings("deprecation")
public class SettingActivity extends PreferenceActivity
{
	private String moduleName;
	private ModuleInfo moduleInfo; 
	
	@Override public void onCreate(Bundle aState)
	{
		super.onCreate(aState);
		
		moduleName = getIntent().getStringExtra("moduleName");
		moduleInfo = ModuleInfo.getInfoAbout(getAssets(), new File(moduleName));
		
		getPreferenceManager().setSharedPreferencesName(moduleInfo.getDataName());
        addPreferencesFromResource(R.xml.preferences);
	}
	
	@Override public void onPause()
	{
		super.onPause();

		if(Game.hasGame())
		{
			Game.queueCommand(new Commands.RefreshSettings(getPreferenceManager().getSharedPreferences()));
		}
	}
}
