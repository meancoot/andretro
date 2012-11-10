package org.andretro.settings;

import java.io.*;

import org.andretro.emulator.*;

import android.preference.*;
import android.content.*;
import android.os.*;

@SuppressWarnings("deprecation")
public class InputActivity extends PreferenceActivity
{
	@Override public void onCreate(Bundle aState)
	{
		super.onCreate(aState);
		
		String moduleName = getIntent().getStringExtra("moduleName");
		ModuleInfo moduleInfo = ModuleInfo.getInfoAbout(this, new File(moduleName));

		getPreferenceManager().setSharedPreferencesName(moduleInfo.getDataName());

		// Setup the preference list
		PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(this);
		
		// Add port 1's pad
		Doodads.Device device = moduleInfo.inputData.getDevice(0,  0);
		for(Doodads.Button i: device.getAll())
		{
			screen.addPreference(new Button(this, i));
		}
		
		setPreferenceScreen(screen);
	}
	
	@Override public void onPause()
	{
		super.onPause();

		if(Game.hasGame())
		{
			Game.queueCommand(new Commands.RefreshInput());
		}
	}
	
	private static class Button extends ButtonSetting
	{
		final Doodads.Button button;
		
		public Button(Context aContext, final Doodads.Button aButton)
		{
			super(aContext, aButton.configKey, aButton.fullName, 0);
			
			button = aButton;
		}
				
		@Override protected void valueChanged(int aKeyCode)
		{
			button.setKeyCode(aKeyCode);
		}
	}
}
