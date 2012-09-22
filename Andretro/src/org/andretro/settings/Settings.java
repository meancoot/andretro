package org.andretro.settings;

import org.andretro.emulator.*;

import android.content.*;
import android.preference.*;

final class Settings
{
	public static class Button extends ButtonSetting
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

	public static class Port extends ListPreference
	{
		private final PreferenceCategory buttonGroup; 
		private final Doodads.Port port;
		
		public Port(Context aContext, int aPort, final PreferenceCategory aGroup)
		{
			super(aContext);
			
			buttonGroup = aGroup;
			port = Game.getInputs().getItem(aPort);
			
			setKey(port.configKey);
			setPersistent(true);
			
			// Build list
			final String[] deviceShort = new String[port.count];
			final String[] deviceLong = new String[port.count];
			
			for(int i = 0; i != port.count; i ++)
			{
				Doodads.Device dev = port.getItem(i);
				deviceShort[i] = dev.name;
				deviceLong[i] = dev.fullName;
			}
			
			setEntries(deviceLong);
			setEntryValues(deviceShort);
			
			setSummary("Current Device");
			setValue(port.getCurrentDevice());
		}
		
		@Override public void setValue(String aValue)
		{
			super.setValue(aValue);
			
			persistString(getValue());

			Doodads.Device device = port.getItem(getValue());			
			setTitle(device.fullName);

			// Clear old preferences
			buttonGroup.removeAll();
					
			// Add new ones
			for(final Doodads.Button i: device.getAll())
			{
				buttonGroup.addPreference(new Button(getContext(), i));
			}

			// Set title
			buttonGroup.setTitle(device.fullName + " Inputs");
		}
	}
}
