package org.andretro.system;

import java.nio.*;
import java.util.concurrent.*;

import javax.microedition.khronos.opengles.*;

import static javax.microedition.khronos.opengles.GL10.*;

// NOT THREAD SAFE
public final class Present
{
    private static final int FRAMESIZE = 1024;
    private static final int FLOATSIZE = 4;
    private static final int VERTEXSIZE = 5;

    private static final int id[] = new int[1];
    private static FloatBuffer vertexData;
    private static float screenWidth;
    private static float screenHeight;
    private static float lastAspect;
    private static boolean refreshPositions;

    private static final ArrayBlockingQueue<VideoFrame> emptyFrames = new ArrayBlockingQueue<VideoFrame>(1);
    private static final ArrayBlockingQueue<VideoFrame> readyFrames = new ArrayBlockingQueue<VideoFrame>(1);
    
    static
    {
    	emptyFrames.offer(new VideoFrame());
    }

    public static class VideoFrame
    {
    	private VideoFrame() {}
    	public final ByteBuffer pixels = ByteBuffer.allocateDirect(1024 * 1024 * 2);
    	public final int[] size = new int[2];
    	public float aspect;
    }
    
    
    public static void initialize(GL10 aGL)
    {
        // State
        aGL.glEnable(GL_TEXTURE_2D);
    
        // Texture
        aGL.glGenTextures(1, id, 0);
        aGL.glBindTexture(GL_TEXTURE_2D, id[0]);
        aGL.glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        aGL.glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        aGL.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, FRAMESIZE, FRAMESIZE, 0, GL_RGBA, GL_UNSIGNED_SHORT_5_5_5_1, null);
        aGL.glMatrixMode(GL_TEXTURE);
        aGL.glLoadIdentity();
        aGL.glScalef(1.0f / (float)FRAMESIZE, 1.0f / (float)FRAMESIZE, 0.0f);
        
        // Vertex Buffer (Only if needed)
        if(null == vertexData)
        {
            ByteBuffer bb = ByteBuffer.allocateDirect(FLOATSIZE * VERTEXSIZE * 4);
            bb.order(ByteOrder.nativeOrder());
            vertexData = bb.asFloatBuffer();
        }
        
        // Vertex pointers
        aGL.glEnableClientState(GL_VERTEX_ARRAY);
        aGL.glEnableClientState(GL_TEXTURE_COORD_ARRAY);
        aGL.glVertexPointer(3, GL_FLOAT, 4 * 5, vertexData.position(0));
        aGL.glTexCoordPointer(2, GL_FLOAT, 4 * 5, vertexData.position(3));
    }
    
    public static void setScreenSize(GL10 aGL, int aWidth, int aHeight)
    {
        aGL.glViewport(0, 0, aWidth, aHeight);
        aGL.glMatrixMode(GL_PROJECTION);
        aGL.glLoadIdentity();
        aGL.glOrthof(0, aWidth, aHeight, 0, -1, 1);
        
        screenWidth = aWidth;
        screenHeight = aHeight;
        refreshPositions = true;
    }
    
    public static VideoFrame getFrameBuffer() throws InterruptedException
    {
		return emptyFrames.take();
    }
    
    public static void putNextBuffer(VideoFrame aFrame) throws InterruptedException
    {
    	readyFrames.put(aFrame);
    }
    
    /**
     * Put a frame back into rotation without showing it.
     * @param aFrame
     * @throws InterruptedException
     */
    public static void cancel(VideoFrame aFrame) throws InterruptedException
    {
    	emptyFrames.put(aFrame);
    }
    
    public static void present(GL10 aGL) throws InterruptedException
    {
    	VideoFrame next = readyFrames.poll();
    	
    	if(null != next && 0 < next.size[0] && 0 < next.size[1])
    	{
    		// Update vertex positions
    		if(refreshPositions || lastAspect != next.aspect)
    		{
    			lastAspect = next.aspect;
    			
    	        float x = 0.0f;
    	        float y = 0.0f;
    	        float width = screenWidth;
    	        float height = screenHeight;
    	        
    	        float outputAspect = (width / height);
    	        float inputAspect = (lastAspect == 0.0f) ? ((float)next.size[0] / (float)next.size[1]) : lastAspect;

    	    	if(outputAspect < inputAspect)
    	    	{
    	    		float oldheight = height;
    	    		height = width / inputAspect;
    	    		y = (oldheight - height) / 2;
    	    	}
    	    	else
    	    	{
    	    		float oldwidth = width;
    	    		width = height * inputAspect;
    	    		x = (oldwidth - width) / 2;
    	    	}
    	    	
    	        setPosition(0, x, y, 0);
    	        setPosition(1, x + width, y, 0);
    	        setPosition(2, x, y + height, 0);
    	        setPosition(3, x + width, y + height, 0);        	
    		}
    		
	    	// Set the texture coords
	    	for(int i = 0; i != 4; i ++)
	    	{
	    		vertexData.put(VERTEXSIZE * i + 3, ((i & 1) == 1) ? next.size[0] - .5f : .5f);
	    		vertexData.put(VERTEXSIZE * i + 4, ((i & 2) == 2) ? next.size[1] - .5f : .5f);
	    	}

	    	// Draw and finish
	    	aGL.glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, next.size[0], next.size[1], GL_RGBA, GL_UNSIGNED_SHORT_5_5_5_1, next.pixels);
	        aGL.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
	    	emptyFrames.put(next);
    	}
    }
    
    // Details
    private static void setPosition(int aIndex, float aX, float aY, float aZ)
    {
        vertexData.put(VERTEXSIZE * aIndex + 0, aX);
        vertexData.put(VERTEXSIZE * aIndex + 1, aY);
        vertexData.put(VERTEXSIZE * aIndex + 2, aZ);
    }
}
