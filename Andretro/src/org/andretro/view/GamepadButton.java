package org.andretro.view;
import org.andretro.*;

import android.content.*;
import android.content.res.*;
import android.widget.*;
import android.view.*;
import android.util.*;

public class GamepadButton extends ImageView implements InputHandler
{
    int touchCount = 0;
    int bits;

    public GamepadButton(Context aContext, AttributeSet aAttributes)
    {
        super(aContext, aAttributes);

        TypedArray a = aContext.obtainStyledAttributes(aAttributes, R.styleable.GamepadButton);

        bits = a.getInt(R.styleable.GamepadButton_bits, 0);

        a.recycle();
    }

    public GamepadButton(Context aContext, int aBits)
    {
        super(aContext);
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
