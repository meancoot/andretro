package org.andretro.settings;

import org.andretro.*;
import org.andretro.emulator.*;

import android.annotation.*;
import android.content.*;
import android.os.*;
import android.preference.*;
import android.view.*;
import android.widget.*;

final class Settings
{
	public static class Boolean extends CheckBoxPreference
	{		
		public Boolean(Context aContext, String aKey, String aName, String aDescription, boolean aDefault)
		{
			super(aContext);
			
			setKey(aKey);
			setTitle(aName);
			setSummary(aDescription);
			setPersistent(true);
			setDefaultValue(aDefault);
		}
	}

	
	public static class Text extends EditTextPreference
	{		
		public Text(Context aContext, String aKey, String aName, String aDescription, String aDefault)
		{
			super(aContext);
			
			setKey(aKey);
			setTitle(aName);
			setSummary(aDescription);
			setPersistent(true);
			setDefaultValue(aDefault);
		}
	}
	
	public static class GenericButton extends DialogPreference
	{	
		@TargetApi(12) public GenericButton(Context aContext, String aKey, String aName, int aDefault)
		{
			super(aContext, null);

			setKey(aKey);
			setTitle(aName);
			setPersistent(true);
			setDefaultValue(aDefault);
			
			// HACK: Set a layout that forces the dialog to get key focus
			// TODO: Make the layout better looking!
			setDialogLayoutResource(R.layout.dialog_focus_hack);
			
			setDialogTitle("Waiting for input");
			setDialogMessage(aName);
			refreshSummary(aDefault);
		}
				
		@Override @TargetApi(12) protected void showDialog(Bundle aState)
		{
			super.showDialog(aState);
		
			// HACK: Set the message in the hacked layout
			((EditText)getDialog().findViewById(R.id.hack_message)).setText(getDialogMessage());
			
			getDialog().setOnKeyListener(new DialogInterface.OnKeyListener()
			{	
				@Override public boolean onKey(DialogInterface aDialog, int aKeyCode, KeyEvent aEvent)
				{
					persistInt(aKeyCode);
					refreshSummary(0);					
					getDialog().dismiss();
					return false;
				}
			});
		}
		
		@TargetApi(12) private void refreshSummary(int aDefault)
		{
	        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR1)
	        {
	        	setSummary(KeyEvent.keyCodeToString(getPersistedInt(aDefault)));
	        }
	        else
	        {
	        	setSummary(Integer.toString(getPersistedInt(aDefault)));
	        }			
		}
	}

	public static class Button extends DialogPreference
	{
		final Doodads.Button button;
		
		@TargetApi(12) public Button(Context aContext, final Doodads.Button aButton)
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
			
	        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR1)
	        {
	        	setSummary(KeyEvent.keyCodeToString(button.getKeyCode()));
	        }
	        else
	        {
	        	setSummary(Integer.toString(button.getKeyCode()));
	        }
		}
				
		@Override @TargetApi(12) protected void showDialog(Bundle aState)
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
			        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR1)
			        {
			        	setSummary(KeyEvent.keyCodeToString(button.getKeyCode()));
			        }
			        else
			        {
			        	setSummary(Integer.toString(button.getKeyCode()));
			        }
					
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
