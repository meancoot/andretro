package org.andretro.input.view;
import org.andretro.*;

import android.annotation.*;
import android.content.*;
import android.widget.*;
import android.view.*;

@SuppressLint("ViewConstructor")
public class ButtonDiamond extends ImageView implements InputHandler
{
    private final int bits[] = new int[4];
    private int currentBits;   

    public ButtonDiamond(Context aContext, int aUpBits, int aDownBits, int aLeftBits, int aRightBits)
    {
    	super(aContext);
    	setImageResource(R.drawable.dpad);
    	
    	bits[0] = aUpBits;
    	bits[1] = aDownBits;
    	bits[2] = aLeftBits;
    	bits[3] = aRightBits;
    }

    @Override public int getBits()
    {
        return currentBits;
    }
    
    @Override public boolean onTouchEvent(MotionEvent aEvent)
    {
        if(aEvent.getAction() == MotionEvent.ACTION_DOWN || aEvent.getAction() == MotionEvent.ACTION_MOVE)
        {
        	// TODO: Cache these!
        	final int w = getWidth() / 3;
        	final int h = getHeight() / 3;
        	
        	final int x = (int)aEvent.getX();
        	final int y = (int)aEvent.getY();
        	
        	currentBits = 0;
        	currentBits |= (x < w) ? bits[2] : 0;
        	currentBits |= (x > w * 2) ? bits[3] : 0;
        	currentBits |= (y < h) ? bits[0] : 0;
        	currentBits |= (y > h * 2) ? bits[1] : 0;

        	return true;
        }
        else if(aEvent.getAction() == MotionEvent.ACTION_UP)
        {
            currentBits = 0;
            return true;
        }
        
        return false;
    }
}
