package org.andretro.browser;
import org.andretro.*;
import org.andretro.emulator.*;

import android.content.*;
import android.app.*;
import android.os.*;
import android.widget.*;
import android.view.*;

import java.util.*;
import java.io.*;

final class ModuleWrapper implements IconAdapterItem
{
	// DATA
	private static class Module
	{
		final String name;
		final int icon;
		
		Module(String aName, int aIcon)
		{
			name = aName;
			icon = aIcon;
		}
	}
	
	private static Map<String, Module> modules;
	static
	{
		modules = new HashMap<String, Module>();
		modules.put("libretro_fceu.so", new Module("Ninteno Entertainment System", R.drawable.nes));
		modules.put("libretro_gambatte.so", new Module("Ninteno Game Boy (Color)", R.drawable.gameboy));
		modules.put("libretro_genesis.so", new Module("Sega Genesis / Master System", R.drawable.genesis));
		modules.put("libretro_snes9xnext.so", new Module("Super Ninteno Entertainment System", R.drawable.snes));
		modules.put("libretro_stella.so", new Module("Atari 2600", R.drawable.a2600));
	}
	
	private static Module getFromFile(String aFile)
	{
		return modules.get(aFile);
	}
	
	// IMP
	private final String fileName;
	private final Module module;

    public ModuleWrapper(String aFileName)
    {
    	if(null == aFileName)
    	{
    		throw new IllegalArgumentException("Filename string may not be null");
    	}

        fileName = aFileName;
        module = getFromFile(aFileName);
    }
    
    public String getFile()
    {
    	return fileName;
    }
    
    @Override public String getText()
    {
    	return (null == module) ? fileName : module.name;
    }
    
    @Override public int getIconResourceId()
    {
    	return (null == module) ? 0 : module.icon;
    }
}

public class ModuleSelectActivity extends Activity implements AdapterView.OnItemClickListener
{
	// HACK: Hard path
	private static final String modulePath = "/data/data/org.andretro/lib/";
	
    private IconAdapter<ModuleWrapper> adapter;
    
    @Override public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.module_select);
        
        // Setup the list adapter
        adapter = new IconAdapter<ModuleWrapper>(this, R.layout.module_select_item);
        
        for(final File lib: new File(modulePath).listFiles())
        {
        	adapter.add(new ModuleWrapper(lib.getName()));
        }
        
        // Setup the list
        GridView list = (GridView)findViewById(R.id.grid);
        list.setAdapter(adapter);
        list.setOnItemClickListener(this);
    }
    
    @Override public void onResume()
    {
    	super.onResume();
    	
    	Game.I.queueCommand(new Commands.ShutDown(null));
    }
    
	@Override public void onItemClick(AdapterView<?> aListView, View aView, int aPosition, long aID)
	{
		String file = adapter.getItem(aPosition).getFile();
		
		Game.I.queueCommand(new Commands.Initialize(this,  modulePath + file,  null));
		startActivity(new Intent(this, DirectoryActivity.class));			
	}
}
