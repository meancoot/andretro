package org.andretro.view;
import org.andretro.*;

import android.content.*;
import android.widget.*;
import android.view.*;
import android.util.*;

public class DirectionalPad extends RelativeLayout implements InputHandler
{
    private GamepadButton[] buttons = new GamepadButton[4];
    int bits[] = new int[4];
    int currentBits;   
    
    static final int positionMultiply[] = new int[]{1, 0, 1, 2, 0, 1, 2, 1};
    
    int downX;
    int downY;

    public DirectionalPad(Context aContext, AttributeSet aAttributes)
    {
        super(aContext, aAttributes);

        bits[0] = aAttributes.getAttributeIntValue(null, "upbits", 0);
        bits[1] = aAttributes.getAttributeIntValue(null, "downbits", 0);
        bits[2] = aAttributes.getAttributeIntValue(null, "leftbits", 0);
        bits[3] = aAttributes.getAttributeIntValue(null, "rightbits", 0);
        
        addButton(R.drawable.button, 0, bits[0]);
        addButton(R.drawable.button, 1, bits[1]);
        addButton(R.drawable.button, 2, bits[2]);
        addButton(R.drawable.button, 3, bits[3]);
    }

    @Override protected void onSizeChanged(int aWidth, int aHeight, int aOldWidth, int aOldHeight)
    {
        int width = aWidth / 3;
        int height = aHeight / 3;
    
        for(int i = 0; i != 4; i ++)
        {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)buttons[i].getLayoutParams();
            
            params.leftMargin = width * positionMultiply[i * 2 + 0];
            params.topMargin = height * positionMultiply[i * 2 + 1];
            params.width = width;
            params.height = height;
            updateViewLayout(buttons[i], params);
        }
        
        super.onSizeChanged(aWidth, aHeight, aOldWidth, aOldHeight);
    }

    private void addButton(int aResource, int aID, int aBits)
    {
        buttons[aID] = new GamepadButton(getContext(), aBits);
        buttons[aID].setImageResource(aResource);
        addView(buttons[aID], new RelativeLayout.LayoutParams(32, 32));
    }

    @Override public int getBits()
    {
        if(true)
        {
            int bits = 0;
            
            for(int i = 0; i != 4; i ++)
            {
                bits |= buttons[i].getBits();
            }
            
            return bits;
        }
        else
        {
            return currentBits;        
        }
    }
    
    public int checkAxis(int aDown, int aCurrent, int aNegative, int aPositive)
    {
        if(aDown - aCurrent < -32)
        {
            return aNegative;
        }
        else if(aDown - aCurrent > 32)
        {
            return aPositive;
        }
        
        return 0;
    }
    
    @Override public boolean onTouchEvent(MotionEvent aEvent)
    {
        if(true)
        {
            return false;
        }
        else
        {
            if(aEvent.getAction() == MotionEvent.ACTION_DOWN)
            {
                downX = (int)aEvent.getX();
                downY = (int)aEvent.getY();
                return true;
            }
            else if(aEvent.getAction() == MotionEvent.ACTION_UP)
            {
                currentBits = 0;
                return true;
            }
    
            if(aEvent.getAction() == MotionEvent.ACTION_MOVE)
            {
                currentBits = 0;
            
                int x = (int)aEvent.getX();
                int y = (int)aEvent.getY();
                
                currentBits |= checkAxis(downX, x, bits[3], bits[2]);
                currentBits |= checkAxis(downY, y, bits[1], bits[0]);
            }
            
            return false;
        }
    }
}
