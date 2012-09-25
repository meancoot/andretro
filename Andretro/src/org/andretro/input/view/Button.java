package org.andretro.input.view;

import org.andretro.*;

import android.annotation.SuppressLint;
import android.content.*;
import android.widget.*;

@SuppressLint("ViewConstructor")
public class Button extends ImageView implements InputGroup.InputHandler
{
    int touchCount = 0;
    int bits;

    public Button(Context aContext, int aBits)
    {
        super(aContext);
    	setImageResource(R.drawable.button);
    	
        bits = aBits;
    }

    @Override public int getBits(int aX, int aY)
    {
        return bits;
    }
}
