package org.andretro.emulator;

import java.util.*;

import android.content.*;

public final class Doodads
{
	private static abstract class NamedInput
	{
		/**
		 * The short name of the input. Best used for setting keys.
		 */
		public final String name;
		
		/**
		 * The friendly name of the input. Best used for display.
		 */
		public final String fullName;
		
		/**
		 * Construct a new NamedInput.
		 * @param aShort The setting name of the input. May not be null.
		 * @param aFull The display name of the input. May not be null.
		 */
		NamedInput(String aShort, String aFull)
		{
			if(null == aShort || null == aFull)
			{
				throw new IllegalArgumentException("Neither name of a NamedInput may be null.");
			}
			
			name = aShort;
			fullName = aFull;
		}
	}

	private static abstract class GroupedInput <E extends NamedInput> extends NamedInput
	{
		/**
		 * List of input objects.
		 */
		protected final ArrayList<E> inputs;
		
		/**
		 * The index of this input object in its parent's list.
		 */
		public final int index;
		
		/**
		 * The number of children in this object.
		 */
		public final int count;
		
		/**
		 * Create a new GroupedInput.
		 * @param aShort Setting name for the input. May not be null.
		 * @param aFull Display name for the input. May not be null.
		 * @param aCount Number of devices in this set.
		 * @param aIndex Index of this item in its parent's set.
		 */
		GroupedInput(String aShort, String aFull, int aCount, int aIndex)
		{
			super(aShort, aFull);
			
			if(aIndex < 0)
			{
				throw new IllegalArgumentException("Index must be greater than or equal to 0");
			}
			
			index = aIndex;
			count = aCount;
			inputs = new ArrayList<E>(aCount);
			
			for(int i = 0; i != aCount; i ++)
			{
				inputs.add(null);
			}
		}
			
		public ArrayList<E> getAll()
		{
			return inputs;
		}
		
		public E getItem(int aIndex)
		{
			return inputs.get(aIndex);
		}
		
		public E getItem(String aName)
		{
			for(E i: inputs)
			{
				if(aName.equals(i.name))
				{	
					return i;
				}
			}
			
			return null;
		}
	}
	
	public final static class Button extends NamedInput
	{
		public final String configKey;
		public final int order;
		public final int type;
		public final int bitOffset;
	    private int keyCode;

	    Button(SharedPreferences aPreferences, final Port aPort, final Device aDevice, int aButton)
	    {
	    	super("button" + aButton, "Button " + aButton);

	    	// Get configKey
	    	configKey = aPort.name + "_" + aDevice.name + "_" + name;
	    	
	    	// Get value
	    	keyCode = aPreferences.getInt(configKey, 0);
	    	
	    	// Grab native data
	    	order = aButton;
	    	type = 0;
	    	bitOffset = aButton;
	    }
	    
	    public int getKeyCode()
	    {
	    	return keyCode;
	    }
	    
	    public void setKeyCode(int aKeyCode)
	    {
	    	keyCode = aKeyCode;
	    }
	}
	
	public final static class Device extends GroupedInput<Button>
	{        
	    Device(SharedPreferences aPreferences, final Port aPort, int aDevice)
	    {
	    	super("device" + aDevice, "Device " + aDevice, 16, aDevice);

	    	for(int i = 0; i != 16; i ++)
	    	{
	    		inputs.set(i, new Button(aPreferences, aPort, this, i));
	    	}
	    }    
	}
	
	public final static class Port extends GroupedInput<Device>
	{
		public final String configKey;
	    public final String defaultDevice;
	    private final String currentDevice;
	    
	    Port(SharedPreferences aPreferences, int aPort)
	    {
	    	super("port" + aPort, "Port " + aPort, 1, aPort);
	    
	    	configKey = "port_" + aPort;
	    	defaultDevice = "device0";
	    	currentDevice = aPreferences.getString(configKey, defaultDevice);
	    	
	    	for(int i = 0; i != count; i ++)
	    	{
	    		inputs.set(i, new Device(aPreferences, this, i));
	    	}
	    }
	    
	    public String getCurrentDevice()
	    {
	    	return currentDevice;
	    }
	 }
	
	public final static class Set extends GroupedInput<Port>
	{
	    Set(SharedPreferences aPreferences)
	    {
	    	super("root", "root", 1, 0);
	    	
	    	for(int i = 0; i != count; i ++)
	    	{
	    		inputs.set(i, new Port(aPreferences, i));
	    	}
	    }
	        
		// Implement
	    public Doodads.Port getPort(int aPort)
	    {
	    	return getItem(aPort);
	    }
	    
	    public Doodads.Device getDevice(int aPort, int aDevice)
	    {
	    	return getItem(aPort).getItem(aDevice);    	
	    }
	    
	    public Doodads.Button getButton(int aPort, int aDevice, int aIndex)
	    {
	    	return getItem(aPort).getItem(aDevice).getItem(aIndex);
	    }
	}
}
