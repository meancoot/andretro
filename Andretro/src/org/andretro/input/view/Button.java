package org.andretro.input.view;

import org.andretro.*;

import android.content.*;
import android.widget.*;
import android.view.*;

public class Button extends ImageView implements InputHandler
{
    int touchCount = 0;
    int bits;

    public Button(Context aContext, int aBits)
    {
        super(aContext);
    	setImageResource(R.drawable.button);
    	
        bits = aBits;
    }

    @Override public int getBits()
    {
        return (touchCount > 0) ? bits : 0;
    }
    
    @Override public boolean onTouchEvent(MotionEvent aEvent)
    {
        if(aEvent.getAction() == MotionEvent.ACTION_DOWN)
        {
            touchCount ++;
            return true;
        }
        else if(aEvent.getAction() == MotionEvent.ACTION_UP)
        {
            touchCount --;
            return true;
        }
        
        return false;
    }
}
