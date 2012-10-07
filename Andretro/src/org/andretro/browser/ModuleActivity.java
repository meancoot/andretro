package org.andretro.browser;

import org.andretro.*;
import org.andretro.emulator.*;

import java.io.*;

import android.content.*;
import android.content.res.*;
import android.app.*;
import android.os.*;
import android.widget.*;
import android.view.*;
import android.view.inputmethod.*;

class ModuleWrapper extends ModuleInfo implements IconAdapterItem
{
	public final File file;
	
    public ModuleWrapper(AssetManager aAssets, File aFile) throws IOException
    {
		super(aAssets, aFile);
		file = aFile;
    }
    
    @Override public boolean isEnabled()
    {
    	return true;
    }
    
    @Override public String getText()
    {
    	return name;
    }
    
    @Override public int getIconResourceId()
    {
    	return 0;
    }
}

public class ModuleActivity extends Activity implements AdapterView.OnItemClickListener
{
	// HACK: Hard path
	private static final String modulePath = "/data/data/org.andretro/lib/";
    private IconAdapter<ModuleWrapper> adapter;
    
    @Override public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.directory_list);
        
        // Setup the list
        adapter = new IconAdapter<ModuleWrapper>(this, R.layout.directory_list_item);
        ListView list = (ListView)findViewById(R.id.list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(this);
        
        setTitle("Select Emulator");
    	
        for(final File lib: new File(modulePath).listFiles())
        {
        	if(lib.getName().startsWith("libretro_"))
        	{
        		try
        		{
        			adapter.add(new ModuleWrapper(getAssets(), lib));;
        		}
        		catch(Exception e)
        		{
        			
        		}
        	}
        }
    }
    
	@Override public void onItemClick(AdapterView<?> aListView, View aView, int aPosition, long aID)
	{
		final ModuleWrapper item = adapter.getItem(aPosition);
		final File selected = item.file;

		startActivity(new Intent(ModuleActivity.this, DirectoryActivity.class)
			.putExtra("path", Environment.getExternalStorageDirectory().getPath())
			.putExtra("moduleName", selected.getAbsolutePath()));
	}
		
    @Override public boolean onCreateOptionsMenu(Menu aMenu)
    {
    	super.onCreateOptionsMenu(aMenu);
		getMenuInflater().inflate(R.menu.directory_list, aMenu);
    	
		aMenu.removeItem(R.id.goto_root);
		aMenu.removeItem(R.id.system_settings);
    	
    	return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem aItem)
    {
        if(R.id.input_method_select == aItem.getItemId())
        {
        	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        	imm.showInputMethodPicker();
        	return true;
        }
    	
        return super.onOptionsItemSelected(aItem);
    }
}