package org.andretro.emulator;

import java.io.*;
import java.util.*;

import org.andretro.*;
import org.andretro.browser.*;
import org.andretro.emulator.Commands.*;
import org.andretro.settings.*;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;

public class StateList extends Activity implements AdapterView.OnItemClickListener
{
    private ArrayAdapter<String> adapter;
    private boolean loading;
    
    @Override public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.directory_list);
        loading = getIntent().getBooleanExtra("loading", false);
        
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        
        // Setup the list
        ListView list = (ListView)findViewById(R.id.list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(this);
        
        setTitle(loading ? "Load State" : "Save State");
        
        // Add data
        for(int i = 0; i != 10; i ++)
        {
        	adapter.add("Slot " + i);
        }
    }
    
	@Override public void onItemClick(AdapterView<?> aListView, View aView, int aPosition, long aID)
	{
		Game.I.queueCommand(new Commands.StateAction(loading, aPosition, new Commands.Callback(this, new Runnable()
		{
			@Override public void run()
			{
				finish();
			}
		})));
	}
}
