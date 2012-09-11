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
	private boolean questionOpen;
	private boolean onScreenInput;
	
	class Draw implements GLSurfaceView.Renderer
	{		
	    @Override public void onSurfaceCreated(GL10 gl, javax.microedition.khronos.egl.EGLConfig config)
	    {
	        Present.initialize(RetroDisplay.this);
	    }    

	    @Override public void onSurfaceChanged(GL10 gl, int aWidth, int aHeight)
	    {
	        Present.setScreenSize(aWidth, aHeight);
	    }

	    @Override public void onDrawFrame(GL10 gl)
	    {
	    	try
	    	{
	    		Present.present();
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

        questionOpen = (null == aState) ? false : aState.getBoolean("questionOpen", false);
        onScreenInput = (null == aState) ? true : aState.getBoolean("onScreenInput", true);
        
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
        view.setEGLContextClientVersion(2);
        view.setRenderer(new Draw());
		view.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		view.setKeepScreenOn(true);
		
		// Add controls
		updateOnScreenControls();
    }
    
    @Override public void onResume()
    {
    	super.onResume();
    	
	    Game.I.queueCommand(new Commands.SetPresentNotify(new Runnable()
	    {
	    	@Override public void run()
	    	{
	    		view.requestRender();
	    	}
	    }, new Commands.Callback(this, new Runnable()
        {
            @Override public void run()
            {
                view.onResume();
            }
        })));
    }
    
    @Override public void onPause()
    {
    	super.onPause();
    	
    	Game.I.queueCommand(new Commands.SetPresentNotify(null, new Commands.Callback(this, new Runnable()
        {
            @Override public void run()
            {
                view.onPause();
            }
        })));
    }
            
    // QuestionDialog.QuestionHandler	
    @Override public void onAnswer(int aID, QuestionDialog aDialog, boolean aPositive)
    {
    	if(CLOSE_GAME_QUESTION == aID && questionOpen)
    	{
    		Game.I.queueCommand(new Commands.Pause(false, null));
    		if(aPositive)
    		{
    			Game.I.queueCommand(new Commands.CloseGame(null));
    			super.onBackPressed();
    		}
    		
    		questionOpen = false;
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
        	if(!questionOpen)
        	{
        		Game.I.queueCommand(new Commands.Pause(true, null));
        		QuestionDialog.newInstance(CLOSE_GAME_QUESTION, "Really Close Game?", "All unsaved data will be lost.", "Yes", "No", null).show(getSupportFragmentManager(), "mainfragment");
        		
        		questionOpen = true;
        	}
        }
        else
        {
        	super.onBackPressed();
        }
    }
    
    @Override protected void onSaveInstanceState(Bundle aState)
    {
    	super.onSaveInstanceState(aState);
    	aState.putBoolean("questionOpen", questionOpen);
    	aState.putBoolean("onScreenInput", onScreenInput);
    }
    
    // Menu
    @Override public boolean onCreateOptionsMenu(Menu aMenu)
    {
    	super.onCreateOptionsMenu(aMenu);
    	getMenuInflater().inflate(R.menu.retro_display, aMenu);
    	
    	aMenu.findItem(R.id.show_on_screen_input).setChecked(onScreenInput);
    	
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

        if(aItem.getItemId() == R.id.show_on_screen_input)
        {
        	onScreenInput = !aItem.isChecked();
        	aItem.setChecked(onScreenInput);
        	
        	updateOnScreenControls();
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
	        else if(aItem.getItemId() == R.id.system_settings)
	        {
	        	startActivity(new Intent(this, SettingActivity.class));
        		return true;
	        }
	        else if(aItem.getItemId() == R.id.reset)
	        {
	            Game.I.queueCommand(new Commands.Reset(new Commands.Callback(this, new Runnable()
	            {
	                @Override public void run()
	                {
	                    Toast.makeText(RetroDisplay.this, "Game Reset", Toast.LENGTH_SHORT).show();
	                }
	            })));

	            return true;
	        }
    	}
    	
        return super.onOptionsItemSelected(aItem);
    }
    
    private void updateOnScreenControls()
    {
		InputGroup inputBase = (InputGroup)findViewById(R.id.base);
    	inputBase.removeChildren();
		
    	if(onScreenInput)
    	{
			inputBase.loadInputLayout(this, getResources().openRawResource(R.raw.default_retro_pad));
    	}
    	
    	Input.setOnScreenInput(onScreenInput ? inputBase : null);
    }
}

