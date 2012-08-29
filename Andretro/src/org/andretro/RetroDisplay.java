package org.andretro;
import org.andretro.emulator.*;
import org.andretro.settings.*;
import org.andretro.system.*;
import org.andretro.input.view.*;

import javax.microedition.khronos.opengles.*;

import android.view.*;
import android.view.inputmethod.*;
import android.annotation.*;
import android.opengl.*;
import android.os.*;
import android.content.*;
import android.content.res.*;
import android.widget.*;

public class RetroDisplay extends android.support.v4.app.FragmentActivity implements QuestionDialog.QuestionHandler
{
	private static final int CLOSE_GAME_QUESTION = 1;
	
	private GLSurfaceView view;
	
	class Draw implements GLSurfaceView.Renderer
	{		
	    @Override public void onSurfaceCreated(GL10 gl, javax.microedition.khronos.egl.EGLConfig config)
	    {
	        Present.initialize(gl);
	    }    

	    @Override public void onSurfaceChanged(GL10 gl, int aWidth, int aHeight)
	    {
	        Present.setScreenSize(gl, aWidth, aHeight);
	    }

	    @Override public void onDrawFrame(GL10 gl)
	    {
	    	try
	    	{
	    		Present.present(gl);
	    	}
	    	catch(InterruptedException e)
	    	{
	    		Thread.currentThread().interrupt();
	    	}
	    }    		
	}
	
    @Override @TargetApi(11) public void onCreate(Bundle aState)
    {	
        super.onCreate(aState);

        // Go fullscreen
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB)
        {
	        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
	        {
	        	getActionBar().hide();
	        }
        }

		// Setup the view
        setContentView(R.layout.retro_display);

        view = (GLSurfaceView)findViewById(R.id.renderer);
        view.setRenderer(new Draw());
		view.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		view.setKeepScreenOn(true);
		
		// Add controls
		// TODO: Support system specific / disabling
		InputGroup inputBase = (InputGroup)findViewById(R.id.base);
		inputBase.loadInputLayout(this, getResources().openRawResource(R.raw.default_retro_pad));
		Input.setOnScreenInput(inputBase);
    }
    
    @Override public void onResume()
    {
    	super.onResume();
    	view.onResume();
    	
	    Game.I.queueCommand(new Commands.SetPresentNotify(new Runnable()
	    {
	    	@Override public void run()
	    	{
	    		view.requestRender();
	    	}
	    }, null));
    }
    
    @Override public void onPause()
    {
    	super.onPause();
    	view.onPause();
    	
    	Game.I.queueCommand(new Commands.SetPresentNotify(null, null));
    }
            
    // QuestionDialog.QuestionHandler	
    @Override public void onAnswer(int aID, QuestionDialog aDialog, boolean aPositive)
    {
    	if(CLOSE_GAME_QUESTION == aID)
    	{	
    		Game.I.queueCommand(new Commands.Pause(false, null));
    		if(aPositive)
    		{
    			Game.I.queueCommand(new Commands.CloseGame(null));
    			super.onBackPressed();
    		}
    	}
    }
       
    @Override public boolean dispatchKeyEvent(KeyEvent aEvent)
    {   
    	int keyCode = aEvent.getKeyCode();
    	
    	// Keys to never handle as game input
		if(keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)
		{
			return super.dispatchKeyEvent(aEvent);
		}
		
		// Update game input structure
		Input.processEvent(aEvent);
		return true;
    }
        
    @Override public void onBackPressed()
    {	
        if(Game.I.isRunning())
        {
        	Game.I.queueCommand(new Commands.Pause(true, null));
        	QuestionDialog.newInstance(CLOSE_GAME_QUESTION, "Really Close Game?", "All unsaved data will be lost.", "Yes", "No", null).show(getSupportFragmentManager(), "mainfragment");
        }
        else
        {
        	super.onBackPressed();
        }
    }
    
    // Menu
    @Override public boolean onCreateOptionsMenu(Menu aMenu)
    {
    	super.onCreateOptionsMenu(aMenu);
    	getMenuInflater().inflate(R.menu.retro_display, aMenu);
        return true;
    }
        
    @Override public boolean onOptionsItemSelected(MenuItem aItem)
    {
        if(aItem.getItemId() == R.id.input_method_select)
        {
        	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        	imm.showInputMethodPicker();
        	return true;
        }

    	if(Game.I.isRunning())
    	{
	        if(aItem.getItemId() == R.id.save_state || aItem.getItemId() == R.id.load_state)
	        {
	            final boolean loading = aItem.getItemId() == R.id.load_state;
	        
	            Game.I.queueCommand(new Commands.StateAction(loading, 0, new Commands.Callback(this, new Runnable()
	            {
	                @Override public void run()
	                {
	                    Toast.makeText(RetroDisplay.this, loading ? "State Loaded" : "State Saved", Toast.LENGTH_SHORT).show();
	                }
	            })));
	            return true;
	        }
	        else if(aItem.getItemId() == R.id.input_settings)
	        {
	        	startActivity(new Intent(this, InputActivity.class));
        		return true;
	        }
	        else if(aItem.getItemId() == R.id.reset)
	        {
	        	Game.I.queueCommand(new Commands.Reset(null));
	        	return true;
	        }
    	}
    	
        return super.onOptionsItemSelected(aItem);
    }
}

