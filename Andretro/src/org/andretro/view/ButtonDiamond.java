package org.andretro.view;


import android.content.*;
import android.widget.*;
import android.view.*;
import android.util.*;

public class ButtonDiamond extends ImageView implements InputHandler
{
    int currentBits = 0;
    int bits[] = new int[4];

    public ButtonDiamond(Context aContext, AttributeSet aAttributes)
    {
        super(aContext, aAttributes);
        bits[0] = aAttributes.getAttributeIntValue(null, "bitsleft", 0);
        bits[1] = aAttributes.getAttributeIntValue(null, "bitsright", 0);
        bits[2] = aAttributes.getAttributeIntValue(null, "bitsup", 0);
        bits[3] = aAttributes.getAttributeIntValue(null, "bitsdown", 0);
    }
        
    @Override public int getBits()
    {
        return currentBits;
    }
    
    @Override public boolean onTouchEvent(MotionEvent aEvent)
    {
        int action = aEvent.getAction();
    
        if(action == MotionEvent.ACTION_UP)
        {
            currentBits = 0;
            return true;
        }
        else if(action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE)
        {
            int x = (int)aEvent.getX();
            int y = (int)aEvent.getY();
            
            int width = getWidth() / 3;
            int height = getHeight() / 3;
            
            currentBits |= (x < width * 2) ? bits[0] : 0;
            currentBits |= (x > width) ? bits[1] : 0;

            currentBits |= (y < height * 2) ? bits[2] : 0;
            currentBits |= (y > height) ? bits[3] : 0;
            
            return true;
        }
        
        return false;
    }
}
