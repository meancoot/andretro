package org.andretro.input.view;
import org.andretro.*;

import android.annotation.*;
import android.content.*;
import android.widget.*;

@SuppressLint("ViewConstructor")
public class ButtonDiamond extends ImageView implements InputGroup.InputHandler
{
    private final int bits[] = new int[4];   

    public ButtonDiamond(Context aContext, int aUpBits, int aDownBits, int aLeftBits, int aRightBits)
    {
    	super(aContext);
    	setImageResource(R.drawable.dpad);
    	
    	bits[0] = aUpBits;
    	bits[1] = aDownBits;
    	bits[2] = aLeftBits;
    	bits[3] = aRightBits;
    }

    @Override public int getBits(int aX, int aY)
    {
    	final int w = getWidth() / 3;
    	final int h = getHeight() / 3;
    	
    	final int x = aX - getLeft();
    	final int y = aY - getTop();
    	
    	int result = 0;
    	result |= (x < w) ? bits[2] : 0;
    	result |= (x > w * 2) ? bits[3] : 0;
    	result |= (y < h) ? bits[0] : 0;
    	result |= (y > h * 2) ? bits[1] : 0;

    	return result;

    }
}
