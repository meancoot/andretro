package org.andretro.browser;

import org.andretro.*;
import org.andretro.emulator.*;
import org.andretro.settings.*;

import java.util.*;
import java.io.*;

import android.content.*;
import android.app.*;
import android.os.*;
import android.widget.*;
import android.view.*;
import android.view.inputmethod.*;

final class FileWrapper implements IconAdapterItem
{
    private final File file;
    private final int typeIndex;

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
    		return (typeIndex == aOther.typeIndex) ? file.compareTo(aOther.file) : ((typeIndex < aOther.typeIndex) ? -1 : 1);
    	}
    	
    	return -1;
    }
}

public class DirectoryActivity extends Activity implements AdapterView.OnItemClickListener
{
    private IconAdapter<FileWrapper> adapter;
    private boolean inRoot;
    
    @Override public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.directory_list);
        inRoot = getIntent().getBooleanExtra("inroot", false);
        
        adapter = new IconAdapter<FileWrapper>(this, R.layout.directory_list_item);
        
        // Setup the list
        ListView list = (ListView)findViewById(R.id.list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(this);
        
        // Load Directory
        String path = getIntent().getStringExtra("path");
        path = (path == null) ? Environment.getExternalStorageDirectory().getPath() : path;

        setTitle(path);
        wrapFiles(new File(path));
    }
    
	@Override public void onItemClick(AdapterView<?> aListView, View aView, int aPosition, long aID)
	{
		final File selected = adapter.getItem(aPosition).getFile();

		if(selected.isFile())
		{
	    	Game.I.queueCommand(new Commands.LoadGame(selected.getAbsolutePath(), new Commands.Callback(this, new Runnable()
	        {
	            @Override public void run()
	            {
	            	startActivity(new Intent(DirectoryActivity.this, RetroDisplay.class));
	            }
	        })));
		}
		else
		{
			startActivity(new Intent(this, DirectoryActivity.class).putExtra("inroot", inRoot).putExtra("path", selected.getAbsolutePath()));
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
    	
    	return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem aItem)
    {
    	if(R.id.goto_root == aItem.getItemId())
    	{
    		startActivity(new Intent(this, DirectoryActivity.class).putExtra("inroot", true).putExtra("path", Environment.getExternalStorageDirectory().getPath()));
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
