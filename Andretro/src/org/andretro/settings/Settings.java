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
		}
		
		@Override protected void onAttachedToActivity()
		{
			super.onAttachedToActivity();
			refreshSummary();
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
					valueChanged(aKeyCode);
					refreshSummary();					
					getDialog().dismiss();
					return false;
				}
			});
		}
		
		@TargetApi(12) private void refreshSummary()
		{
	        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR1)
	        {
	        	setSummary(KeyEvent.keyCodeToString(getPersistedInt(0)));
	        }
	        else
	        {
	        	setSummary(Integer.toString(getPersistedInt(0)));
	        }			
		}
		
		protected void valueChanged(int aKeyCode)
		{
			
		}
	}

	public static class Button extends GenericButton
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
