package org.andretro.browser;
import org.andretro.*;
import org.andretro.emulator.*;

import android.content.*;
import android.app.*;
import android.os.*;
import android.widget.*;
import android.view.*;

import java.io.*;

final class ModuleWrapper implements IconAdapterItem
{	
	private final String fileName;

    public ModuleWrapper(String aFileName)
    {
    	if(null == aFileName)
    	{
    		throw new IllegalArgumentException("Filename string may not be null");
    	}

        fileName = aFileName;
    }
    
    public String getFile()
    {
    	return fileName;
    }
    
    @Override public String getText()
    {
    	return fileName;
    }
    
    @Override public int getIconResourceId()
    {
    	return 0;
    }
}

public class ModuleSelectActivity extends Activity implements AdapterView.OnItemClickListener
{
	// HACK: Hard path
	static final String modulePath = "/data/data/org.andretro/lib/";
	
    private IconAdapter<ModuleWrapper> adapter;
    
    @Override public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
//        setContentView(R.layout.module_select);
        setContentView(R.layout.directory_list);

        
        // Setup the list adapter
//        adapter = new IconAdapter<ModuleWrapper>(this, R.layout.module_select_item);
        adapter = new IconAdapter<ModuleWrapper>(this, R.layout.directory_list_item);
        
        for(final File lib: new File(modulePath).listFiles())
        {
        	if(lib.getName().startsWith("libretro_"))
        	{
        		adapter.add(new ModuleWrapper(lib.getName()));
        	}
        }
        
        // Setup the list
//        GridView list = (GridView)findViewById(R.id.grid);
        ListView list = (ListView)findViewById(R.id.list);
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
		
		Game.I.queueCommand(new Commands.Initialize(this,  modulePath + file, new Commands.Callback(this, new Runnable()
		{
			@Override public void run()
			{
				startActivity(new Intent(ModuleSelectActivity.this, DirectoryActivity.class)
					.putExtra("path", Game.I.getModuleSystemDirectory() + "/Games"));
			}
		})));			
	}
}
