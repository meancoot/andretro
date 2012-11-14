package org.andretro.system;

import java.io.*;
import java.nio.*;

import android.graphics.*;

public class PngWriter
{	
	public static boolean write(final String aFileName, final ByteBuffer aPixels, final int aWidth, final int aHeight, final int aPixelFormat)
	{
		final int total = aWidth * aHeight;
		final int[] colors = new int[total];
		
		if(aPixelFormat != 1)
		{
			final ShortBuffer spixels = aPixels.asShortBuffer();
			final int less = (0 == aPixelFormat) ? 0 : 1;
		
			for(int i = 0; i != total; i ++)
			{
				final int color = spixels.get(i);
				final int r = (color >> 11) & 0x1F;
				final int g = (color >> 6) & 0x1F;
				final int b = (color >> (1 - less)) & 0x1F;
				
				colors[i] = Color.argb(0xFF, r << 3, g << 3, b << 3);
			}
		}
		
		// Write bitmap
		try
		{
			final Bitmap image = Bitmap.createBitmap(colors, aWidth, aHeight, Bitmap.Config.ARGB_8888);
			final FileOutputStream output = new FileOutputStream(aFileName);
			image.compress(Bitmap.CompressFormat.PNG, 100, output);
			output.close();
			image.recycle();
			return true;
		}
		catch(Exception e)
		{
			
		}
		
		return false;
	}
}
