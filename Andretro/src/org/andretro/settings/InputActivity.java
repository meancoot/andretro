package org.andretro.settings;
import java.util.*;

import org.andretro.emulator.*;

import android.preference.*;
import android.os.*;


/**
 * PreferenceFragment which lists all ports for a particular system.
 * @author jason
 *
 */
public class InputActivity extends PreferenceActivity
{
	@Override public void onPause()
	{
		super.onPause();

		Game.I.queueCommand(new Commands.RefreshInput(null));
	}
	
	@Override public void onBuildHeaders(List<Header> aHeaders)
	{
		for(final Doodads.Port i: Game.I.getInputs().getAll())
		{
			Bundle arguments = new Bundle();
			arguments.putInt("port", i.index);
			
			Header header = new Header();
			header.title = i.fullName;
			header.fragment = "org.andretro.settings.InputFragment";
			header.fragmentArguments = arguments;
			
			aHeaders.add(header);
		}
	}
}
