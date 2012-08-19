package org.andretro.settings;

import org.andretro.*;
import org.andretro.emulator.*;

import android.content.*;
import android.os.*;
import android.preference.*;
import android.view.*;
import android.widget.*;

final class Settings
{	
/*	public static class Boolean extends SwitchPreference
	{
		private final Setting setting;
		
		public Boolean(Context aContext, final Setting aSetting)
		{
			super(aContext);
			
			setting = aSetting;
			
			setTitle(setting.getName());
			setSummary(setting.getDescription());
			
			super.setChecked(setting.getBooleanValue());
			setSwitchTextOff("NO");
			setSwitchTextOn("YES");
		}
		
		@Override public void setChecked(boolean aChecked)
		{
			super.setChecked(aChecked);
			setting.setBooleanValue(aChecked);
		}
	}

	public static class Text extends EditTextPreference
	{
		private final Setting setting;
		
		public Text(Context aContext, Setting aSetting)
		{
			super(aContext);
			
			setting = aSetting;

			// Set text type based on input setting
			switch(aSetting.getType())
			{
				case 0: getEditText().setInputType(InputType.TYPE_CLASS_NUMBER); break;
				case 1: getEditText().setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED); break;
				case 3: getEditText().setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL); break;
				case 4: getEditText().setInputType(InputType.TYPE_CLASS_TEXT); break;
				case 5: getEditText().setInputType(InputType.TYPE_CLASS_TEXT); break;
				default: throw new IllegalArgumentException("Setting type not valid for TextSetting");
			}
			
			// Set strings
			setTitle(setting.getName());
			setSummary(setting.getDescription());
			
			super.setText(setting.getStringValue());
		}
		
		@Override public void setText(String aText)
		{
			super.setText(aText);		
			setting.setStringValue(aText);
		}
	}*/

	public static class Button extends DialogPreference
	{
		final Doodads.Button button;
		
		public Button(Context aContext, final Doodads.Button aButton)
		{
			super(aContext, null);

			button = aButton;
			
			setTitle(aButton.fullName);
			setKey(aButton.configKey);
			setPersistent(true);
			
			// HACK: Set a layout that forces the dialog to get key focus
			// TODO: Make the layout better looking!
			setDialogLayoutResource(R.layout.dialog_focus_hack);
			
			setDialogTitle("Waiting for input");
			setDialogMessage(aButton.fullName);
			
			setSummary(KeyEvent.keyCodeToString(button.getKeyCode()));
		}
				
		@Override protected void showDialog(Bundle aState)
		{
			super.showDialog(aState);
		
			// HACK: Set the message in the hacked layout
			((EditText)getDialog().findViewById(R.id.hack_message)).setText(getDialogMessage());
			
			getDialog().setOnKeyListener(new DialogInterface.OnKeyListener()
			{	
				@Override public boolean onKey(DialogInterface aDialog, int aKeyCode, KeyEvent aEvent)
				{
					persistInt(aKeyCode);
					button.setKeyCode(aKeyCode);
					setSummary(KeyEvent.keyCodeToString(aKeyCode));
					
					getDialog().dismiss();
					return false;
				}
			});
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
			port = Game.I.getInputs().getItem(aPort);
			
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
