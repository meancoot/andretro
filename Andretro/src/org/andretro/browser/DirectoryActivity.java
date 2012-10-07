package org.andretro.browser;

import org.andretro.*;
import org.andretro.emulator.*;
import org.andretro.settings.*;
import org.andretro.system.*;

import java.util.*;
import java.io.*;

import android.content.*;
import android.app.*;
import android.os.*;
import android.widget.*;
import android.view.*;
import android.view.inputmethod.*;

class FileWrapper implements IconAdapterItem
{
    protected final File file;
    protected final int typeIndex;

    public FileWrapper(File aFile)
    {
    	if(null == aFile)
    	{
    		throw new IllegalArgumentException("File object may not be null");
    	}

        file = aFile;
        typeIndex = (file.isDirectory() ? 1 : 0) + (file.isFile() ? 2 : 0);
    }
    
    public File getFile()
    {
    	return file;
    }
    
    public boolean isEnabled()
    {
    	return file.isDirectory() || Game.validFile(file);
    }
    
    @Override public String getText()
    {
    	return file.getName();
    }
    
    @Override public int getIconResourceId()
    {
    	return file.isFile() ? R.drawable.file : R.drawable.folder;
    }
    
    public int compareTo(FileWrapper aOther)
    {	
    	if(null != aOther)
    	{
    		// Who says ternary is hard to follow
    		if(isEnabled() == aOther.isEnabled())
    		{
    			return (typeIndex == aOther.typeIndex) ? file.compareTo(aOther.file) : ((typeIndex < aOther.typeIndex) ? -1 : 1);
    		}
    		else
    		{
    			return isEnabled() ? -1 : 1;
    		}
    	}
    	
    	return -1;
    }
}

final class ModuleWrapper extends FileWrapper
{
    public ModuleWrapper(File aFile)
    {
    	super(aFile);
    }
        
    public boolean isEnabled()
    {
    	return true;
    }
}


public class DirectoryActivity extends Activity implements AdapterView.OnItemClickListener
{
	// HACK: Hard path
	static final String modulePath = "/data/data/org.andretro/lib/";
	
    private IconAdapter<FileWrapper> adapter;
    private boolean inRoot;
    private String moduleName;
    
    @Override public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.directory_list);
        moduleName = getIntent().getStringExtra("moduleName");
        inRoot = getIntent().getBooleanExtra("inroot", false);
        
        adapter = new IconAdapter<FileWrapper>(this, R.layout.directory_list_item);
        
        // Setup the list
        ListView list = (ListView)findViewById(R.id.list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(this);
        
        // Load Directory
        String path;
        if(null != moduleName)
        {
        	path = getIntent().getStringExtra("path");
			Game.queueCommand(new Commands.Initialize(this, moduleName, new CommandQueue.Callback(this, new Runnable()
			{
				@Override public void run()
				{
		        	String filePath = getIntent().getStringExtra("path");
		        	filePath = (filePath == null) ? Environment.getExternalStorageDirectory().getPath() : filePath;
		            wrapFiles(new File(filePath));
				}
			})));
        }
        else
        {
        	path = "Select Emulator";
        	
            for(final File lib: new File(modulePath).listFiles())
            {
            	if(lib.getName().startsWith("libretro_"))
            	{
            		adapter.add(new ModuleWrapper(lib));
            	}
            }
        }

        setTitle(path);
    }
    
	@Override public void onItemClick(AdapterView<?> aListView, View aView, int aPosition, long aID)
	{
		final File selected = adapter.getItem(aPosition).getFile();

		if(null != moduleName)
		{
			if(selected.isFile())
			{
		    	Game.queueCommand(new Commands.LoadGame(selected, new CommandQueue.Callback(this, new Runnable()
		        {
		            @Override public void run()
		            {
		            	startActivity(new Intent(DirectoryActivity.this, RetroDisplay.class));
		            }
		        })));
			}
			else
			{
				startActivity(new Intent(this, DirectoryActivity.class).putExtra("inroot", inRoot)
						.putExtra("path", selected.getAbsolutePath())
						.putExtra("moduleName", moduleName));
			}
		}
		else
		{
			Game.queueCommand(new Commands.Initialize(this, selected.getAbsolutePath(), new CommandQueue.Callback(this, new Runnable()
			{
				@Override public void run()
				{
					startActivity(new Intent(DirectoryActivity.this, DirectoryActivity.class)
						.putExtra("path", Game.getModuleSystemDirectory() + "/Games")
						.putExtra("moduleName", selected.getAbsolutePath()));
				}
			})));
		}
	}
	
	@Override public void onResume()
	{
		super.onResume();
		
		if(null != moduleName)
		{
			Game.queueCommand(new Commands.Initialize(this, moduleName, null));
		}
	}
	
    @Override public boolean onCreateOptionsMenu(Menu aMenu)
    {
    	super.onCreateOptionsMenu(aMenu);
		getMenuInflater().inflate(R.menu.directory_list, aMenu);
    	
    	if(inRoot)
    	{
    		aMenu.removeItem(R.id.goto_root);
    	}
    	
    	if(null == moduleName)
    	{
    		aMenu.removeItem(R.id.goto_root);
    		aMenu.removeItem(R.id.system_settings);
    	}
    	
    	return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem aItem)
    {
    	if(R.id.goto_root == aItem.getItemId())
    	{
    		startActivity(new Intent(this, DirectoryActivity.class)
    				.putExtra("inroot", true)
    				.putExtra("path", Environment.getExternalStorageDirectory().getPath())
    				.putExtra("moduleName", moduleName));
    		return true;
    	}
    	
        if(R.id.input_method_select == aItem.getItemId())
        {
        	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        	imm.showInputMethodPicker();
        	return true;
        }
    	
        if(aItem.getItemId() == R.id.system_settings)
        {
        	startActivity(new Intent(this, SettingActivity.class));
    		return true;
        }

        
        return super.onOptionsItemSelected(aItem);
    }
        
    private void wrapFiles(File aDirectory)
    {
    	if(null == aDirectory || !aDirectory.isDirectory())
    	{
    		throw new IllegalArgumentException("Directory is not valid.");
    	}
    	        
        // Copy new items
        for(File file: aDirectory.listFiles())
        {
        	adapter.add(new FileWrapper(file));
        }
        
        // Sort items
        adapter.sort(new Comparator<FileWrapper>()
        {
            @Override public int compare(FileWrapper aLeft, FileWrapper aRight)
            {
                return aLeft.compareTo(aRight);
            };
        });

        // Update
        adapter.notifyDataSetChanged();    	
    }
}
