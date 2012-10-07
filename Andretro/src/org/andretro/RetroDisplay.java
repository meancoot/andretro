package org.andretro;
import org.andretro.emulator.*;
import org.andretro.settings.*;
import org.andretro.system.*;
import org.andretro.input.view.*;

import javax.microedition.khronos.opengles.*;
import java.io.*;

import android.view.*;
import android.view.inputmethod.*;
import android.annotation.*;
import android.opengl.*;
import android.os.*;
import android.content.*;
import android.widget.*;
import android.app.*;

public class RetroDisplay extends android.support.v4.app.FragmentActivity implements QuestionDialog.QuestionHandler
{
	private static final int CLOSE_GAME_QUESTION = 1;
	
	private GLSurfaceView view;
	private boolean questionOpen;
	private boolean onScreenInput;
	private boolean showActionBar;
	private volatile boolean refreshWindowAndInput = true;
	private String moduleName;
	
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
	    	if(refreshWindowAndInput)
	    	{	    		
	    		RetroDisplay.this.runOnUiThread(new Runnable()
	    		{
	    			@Override public void run()
	    			{
	    				setupWindowAndControls();
	    			}
	    		});
	    	}
	    	
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
	
    @Override @TargetApi(14) public void onCreate(Bundle aState)
    {	
        super.onCreate(aState);

        questionOpen = (null == aState) ? false : aState.getBoolean("questionOpen", false);
        onScreenInput = (null == aState) ? true : aState.getBoolean("onScreenInput", true);
        showActionBar = (null == aState) ? true : aState.getBoolean("showActionBar", true);
        moduleName = getIntent().getStringExtra("moduleName");
        
        // Setup the window
        final int feature = (Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB) ? Window.FEATURE_NO_TITLE : Window.FEATURE_ACTION_BAR_OVERLAY;
        requestWindowFeature(feature);
 
		// Setup the view
        setContentView(R.layout.retro_display);

        view = (GLSurfaceView)findViewById(R.id.renderer);
        view.setEGLContextClientVersion(2);
        view.setRenderer(new Draw());
		view.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		view.setKeepScreenOn(true);
		
		if(!Game.hasGame())
		{
			Game.queueCommand(new Commands.LoadGame(this, moduleName, new File(getIntent().getStringExtra("path"))));
		}
    }
    
    @Override public void onResume()
    {
    	super.onResume();
    	refreshWindowAndInput = true;
    	
	    Game.queueCommand(new Commands.SetPresentNotify(new Runnable()
	    {
	    	@Override public void run()
	    	{
	    		view.requestRender();
	    	}
	    }).setCallback(new CommandQueue.Callback(this, new Runnable()
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
    	
    	Game.queueCommand(new Commands.SetPresentNotify(null).setCallback(new CommandQueue.Callback(this, new Runnable()
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
    		Game.queueCommand(new Commands.Pause(false));
    		if(aPositive)
    		{
    			Game.queueCommand(new Commands.CloseGame().setCallback(new CommandQueue.Callback(this, new Runnable()
    			{
    				@Override public void run()
    				{
    					System.exit(0);
    				}
    			})));
    		}
    		
    		questionOpen = false;
    		refreshWindowAndInput = true;
    	}
    }
    
    @Override @TargetApi(11) public boolean dispatchTouchEvent(MotionEvent aEvent)
    {	
    	if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB)
    	{
	    	final ActionBar bar = getActionBar();
	    	final float barSize = (null == bar) ? 0 : bar.getHeight() * getResources().getDisplayMetrics().density;
	    	
			if((null != bar) && (aEvent.getActionMasked() == MotionEvent.ACTION_DOWN))
			{
				final boolean top = aEvent.getY() < barSize;
				if((top && !showActionBar) || (!top && showActionBar))
				{
					showActionBar = !showActionBar;
					refreshWindowAndInput = true;
					return true;		
				}
			}
    	}
		
		return super.dispatchTouchEvent(aEvent);
    }
    
    @Override public boolean dispatchKeyEvent(KeyEvent aEvent)
    {   
    	int keyCode = aEvent.getKeyCode();
    	
    	// Keys to never handle as game input
		if(keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_MENU)
		{
			return super.dispatchKeyEvent(aEvent);
		}
		
		// Update game input structure
		Input.processEvent(aEvent);
		return true;
    }
        
    @Override public void onBackPressed()
    {	
        if(Game.hasGame())
        {
        	if(!questionOpen)
        	{
        		Game.queueCommand(new Commands.Pause(true));
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
    	aState.putBoolean("showActionBar", showActionBar);
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
        	
        	refreshWindowAndInput = true;
        }
        
        if(aItem.getItemId() == R.id.reset)
        {
            Game.queueCommand(new Commands.Reset().setCallback(new CommandQueue.Callback(this, new Runnable()
            {
                @Override public void run()
                {
                    Toast.makeText(RetroDisplay.this, "Game Reset", Toast.LENGTH_SHORT).show();
                }
            })));

            return true;
        }
    
        // Start a new activity
        final Intent intent = new Intent().putExtra("moduleName", moduleName);
        
        if(aItem.getItemId() == R.id.save_state || aItem.getItemId() == R.id.load_state)
        {
        	intent.setClass(this, StateList.class).putExtra("loading", aItem.getItemId() == R.id.load_state);
        	startActivity(intent);
        	return true;
        }
        else if(aItem.getItemId() == R.id.input_settings || aItem.getItemId() == R.id.system_settings)
        {
        	intent.setClass(this, (aItem.getItemId() == R.id.system_settings) ? SettingActivity.class : InputActivity.class);
        	startActivity(intent);
        	return true;
        }
        
        // Unhandled
        return super.onOptionsItemSelected(aItem);
    }
        
    @TargetApi(14) private void setupWindowAndControls()
    {
    	refreshWindowAndInput = false;
    	
    	// Set Window Properties
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB)
        {        	
        	final ActionBar bar = getActionBar();
        	
	        if(null != bar && showActionBar)
	        {
	        	bar.show();
	        }
	        else if(null != bar && !showActionBar)
	        {
	        	bar.hide();
	        }
        }

        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        {
	        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }
        
        // Set on screen input
		InputGroup inputBase = (InputGroup)findViewById(R.id.base);
    	inputBase.removeChildren();
		
    	if(onScreenInput)
    	{
			inputBase.loadInputLayout(this, getResources().openRawResource(R.raw.default_retro_pad));
    	}
    	
    	Input.setOnScreenInput(onScreenInput ? inputBase : null);
    }
}

