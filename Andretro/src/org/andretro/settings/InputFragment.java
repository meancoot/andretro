package org.andretro.settings;

import org.andretro.emulator.*;

import android.preference.*;
import android.os.*;

public class InputFragment extends PreferenceFragment
{	
	@Override public void onCreate(Bundle aState)
	{
		super.onCreate(aState);

		getPreferenceManager().setSharedPreferencesName(Game.I.getModuleName());

		// Setup the preference list
		PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(getActivity());
		PreferenceCategory category = new PreferenceCategory(getActivity());
		category.setOrder(1000);

		screen.addPreference(category);
		screen.addPreference(new Settings.Port(getActivity(), getArguments().getInt("port"), category));
		setPreferenceScreen(screen);
	}
}
