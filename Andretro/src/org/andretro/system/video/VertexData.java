package org.andretro.system.video;

import static android.opengl.GLES20.*;
import android.graphics.*;

import java.nio.*;

class Size
{
	public final float aspect;
	public final float w;
	public final float h;
	
	public Size(Point aPoint)
	{
		w = (float)aPoint.x;
		h = (float)aPoint.y;
		
		aspect = (w / h);
	}
}

public class VertexData
{
	private static final float TEXTURESIZE = 1024.0f;
	private static final int BUFFERSIZE = (16 * 4);
	private static final int[] id = new int[1];
	
    private static final float[] vertexData = new float[]
    {
    	-1, 1, 0, 0, 1, 1, 1, 0, -1, -1, 0, 1, 1, -1, 1, 1,
    	-1, 1, 1, 0, 1, 1, 1, 1, -1, -1, 0, 0, 1, -1, 0, 1,
    	-1, 1, 1, 1, 1, 1, 0, 1, -1, -1, 1, 0, 1, -1, 0, 0,
    	-1, 1, 0, 1, 1, 1, 0, 0, -1, -1, 1, 1, 1, -1, 1, 0
    };
    
    private static Point screenSize = new Point();
    private static Point imageSize = new Point();
    
    private static float aspect;
    private static int rotate;
    private static boolean aspectMode = false;
    private static float aspectForce = 0.0f;
    
    private static boolean needUpdate = true;
    
    public static void create()
    {
        glGenBuffers(1, id, 0);        
        glBindBuffer(GL_ARRAY_BUFFER, id[0]);

        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * 4, 0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * 4, 4 * 2);

    }
        
    public static void setScreenSize(int aWidth, int aHeight)
    {
		needUpdate = needUpdate || !screenSize.equals(aWidth, aHeight);
		screenSize.set(aWidth, aHeight);
    }
    
    public static void setImageData(int aWidth, int aHeight, float aAspect, int aRotate)
    {
		needUpdate = needUpdate || !imageSize.equals(aWidth, aHeight) || (aRotate != rotate) || (aAspect != aspect);
    	
		imageSize.set(aWidth, aHeight);
    	aspect = aAspect;
    	rotate = aRotate;
    }
    	    
    public static void setForcedAspect(boolean aUseCustom, float aAspect)
    {
    	if(aAspect < 0.00001f)
    	{
    		aUseCustom = false;
    		aAspect = 0.0f;
    	}
    	
		needUpdate = needUpdate || (aUseCustom != aspectMode) || (aUseCustom && (aAspect != aspectForce));
    	
    	aspectMode = aUseCustom;
    	aspectForce = aAspect;
    }
	
    public static void draw()
    {
    	if(needUpdate)
    	{
    		update();
    		needUpdate = false;
    	}
    	
    	glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    }
    
    private static void update()
    {    	
    	final FloatBuffer vertexBuffer = ByteBuffer.allocateDirect(BUFFERSIZE).order(ByteOrder.nativeOrder()).asFloatBuffer();
    	final boolean invertAspect = (rotate & 1) == 1;

    	final Size scrS = new Size(screenSize);
    	final Size imgS = new Size(imageSize);    	
    	
    	float inputAspect = (aspect <= 0.00001f) ? imgS.aspect : aspect;
    	inputAspect = aspectMode ? aspectForce : inputAspect;
    	inputAspect = !invertAspect ? inputAspect : 1.0f / inputAspect;
    	
    	float width = (!(scrS.aspect < inputAspect)) ? scrS.h * inputAspect : scrS.w;
    	float height = (scrS.aspect < inputAspect) ? scrS.w / inputAspect : scrS.h;
    	    	
    	for(int i = 0; i != 4; i ++)
    	{
    		int idx = (i * 4) + (rotate * 16);
    	    		
    		vertexBuffer.put((i * 4) + 0, (vertexData[idx + 0] * width) / scrS.w);
    		vertexBuffer.put((i * 4) + 1, (vertexData[idx + 1] * height) / scrS.h);
    		vertexBuffer.put((i * 4) + 2, ((vertexData[idx + 2] * imgS.w) - (.5f * vertexData[idx + 2])) / TEXTURESIZE);
    		vertexBuffer.put((i * 4) + 3, ((vertexData[idx + 3] * imgS.h) - (.5f * vertexData[idx + 3])) / TEXTURESIZE);
    	}
    	
    	glBufferData(GL_ARRAY_BUFFER, BUFFERSIZE, vertexBuffer.position(0), GL_STATIC_DRAW);
    }
}