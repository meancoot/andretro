package org.andretro.view;

import android.util.*;
import android.view.*;
import android.content.*;
import android.widget.*;

public class InputGroup extends RelativeLayout implements InputHandler
{
    public InputGroup(Context aContext, AttributeSet aAttributes)
    {
        super(aContext, aAttributes);
    }
    
    public int getBits()
    {    
        int bits = 0;
        int childCount = getChildCount();
        
        for(int i = 0; i != childCount; i ++)
        {
            View child = getChildAt(i);
            if(child instanceof InputHandler)
            {
                InputHandler childIH = (InputHandler)child;
                bits |= childIH.getBits();
            }
        }
        
        return bits;
    }
}
