package org.andretro.settings;

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
		
		getPreferenceManager().setSharedPreferencesName("retropad");

		// Setup the preference list
		PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(this);
		
		// Add port 1's pad
		Doodads.Device device = Game.getInputs().getDevice(0,  0);
		for(Doodads.Button i: device.getAll())
		{
			screen.addPreference(new Button(this, i));
		}
		
		setPreferenceScreen(screen);
	}
	
	@Override public void onPause()
	{
		super.onPause();

		Game.queueCommand(new Commands.RefreshInput(null));
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
