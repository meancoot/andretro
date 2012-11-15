package org.andretro.system;

import java.io.*;
import java.nio.*;

import android.graphics.*;

import static android.opengl.GLES20.*;

public class PngWriter
{	
	public static boolean write(final String aFileName, final int aX, final int aY, final int aWidth, final int aHeight)
	{
		final int total = aWidth * aHeight;
		final int[] colors = new int[total];

		glReadPixels(0, 0, 256, 256, GL_RGBA, GL_UNSIGNED_BYTE, IntBuffer.wrap(colors));
		
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
