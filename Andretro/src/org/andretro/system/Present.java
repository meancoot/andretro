package org.andretro.system;
import org.andretro.R;

import java.io.*;
import java.nio.*;
import java.util.concurrent.*;

import javax.microedition.khronos.opengles.*;

import android.content.*;
import android.graphics.*;
import android.opengl.*;

import static android.opengl.GLES20.*;

// NOT THREAD SAFE
public final class Present implements GLSurfaceView.Renderer
{	
    private static final int FRAMESIZE = 1024;

    private static int programID;
    private static final int id[] = new int[8];
    private static volatile boolean smoothMode = true;
    private static volatile boolean aspectMode = false;
    private static volatile float aspectForce = 0.0f;
    private static String vertexShader;
    private static String fragmentShader;
    
    private static final float[] vertexBufferData = new float[]
    {
    	-1, 1, 0, 0, 1, 1, 1, 0, -1, -1, 0, 1, 1, -1, 1, 1,
    	-1, 1, 1, 0, 1, 1, 1, 1, -1, -1, 0, 0, 1, -1, 0, 1,
    	-1, 1, 1, 1, 1, 1, 0, 1, -1, -1, 1, 0, 1, -1, 0, 0,
    	-1, 1, 0, 1, 1, 1, 0, 0, -1, -1, 1, 1, 1, -1, 1, 0
    };

    
    // Frame buffer queue
    private static final ArrayBlockingQueue<VideoFrame> emptyFrames = new ArrayBlockingQueue<VideoFrame>(1);
    private static final ArrayBlockingQueue<VideoFrame> readyFrames = new ArrayBlockingQueue<VideoFrame>(1);
    
    static
    {
    	emptyFrames.offer(new VideoFrame());
    }

    public static class VideoFrame
    {
    	private VideoFrame() {}
    	public final ByteBuffer pixels = ByteBuffer.allocateDirect(1024 * 1024 * 2).order(ByteOrder.nativeOrder());
    	public final int[] size = new int[3];
    	public float aspect;
    	
    	public void writeToPng(final String aFileName)
    	{
    		// Ugly, but effective
    		final int width = size[0];
    		final int height = size[1];
    		final int total = width * height;
    		final ShortBuffer spixels = pixels.asShortBuffer();
    		
    		// Convert to RGB32 :(
    		final int[] colors = new int[total];
    		for(int i = 0; i != total; i ++)
    		{
    			final int color = spixels.get(i);
    			final int r = (color >> 11) & 0x1F;
    			final int g = (color >> 6) & 0x1F;
    			final int b = (color >> 1) & 0x1F;
    			
    			colors[i] = Color.argb(0xFF, r << 3, g << 3, b << 3);
    		}
    		
    		// Make bitmap
    		try
    		{
    			final Bitmap image = Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888);
    			final FileOutputStream output = new FileOutputStream(aFileName);
    			image.compress(Bitmap.CompressFormat.PNG, 100, output);
    			output.close();
    			image.recycle();
    		}
    		catch(Exception e)
    		{
    			throw new RuntimeException(e);
    		}
    	}
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
    
    // OpenGL Renderer
    private static String getShaderString(final InputStream aSource)
    {
    	String source = "";
    	
    	try
    	{
    		source = new java.util.Scanner(aSource).useDelimiter("\\A").next();
    		aSource.close();
    	}
    	catch(IOException e)
    	{
    	
    	}

    	return source;
    }
    
    private static int buildShader(int aType, final String aSource)
    {
    	int result = glCreateShader(aType);
    	glShaderSource(result, aSource);
    	glCompileShader(result);
    	
    	int[] state = new int[1];
    	glGetShaderiv(result, GL_COMPILE_STATUS, state, 0);
    	if(0 == state[0])
    	{
    		System.out.println(glGetShaderInfoLog(result));
    	}
    	
    	return result;
    }
    
    public static GLSurfaceView.Renderer createRenderer(Context aContext)
    {
    	vertexShader = getShaderString(aContext.getResources().openRawResource(R.raw.vertex_shader));
    	fragmentShader = getShaderString(aContext.getResources().openRawResource(R.raw.fragment_shader));
    	return new Present();
    }
    
    @Override public void onSurfaceCreated(GL10 gl, javax.microedition.khronos.egl.EGLConfig config)
    {    	
    	// Program
    	programID = glCreateProgram();
    	glAttachShader(programID, buildShader(GL_VERTEX_SHADER, vertexShader));
    	glAttachShader(programID, buildShader(GL_FRAGMENT_SHADER, fragmentShader));
    	glBindAttribLocation(programID, 0, "pos");
    	glBindAttribLocation(programID, 1, "tex");
    	
    	glLinkProgram(programID);
    	glUseProgram(programID);

    	id[2] = glGetUniformLocation(programID, "screenWidth");
    	id[3] = glGetUniformLocation(programID, "screenHeight");
    	id[4] = glGetUniformLocation(programID, "imageWidth");
    	id[5] = glGetUniformLocation(programID, "imageHeight");
    	id[6] = glGetUniformLocation(programID, "imageAspect");
    	id[7] = glGetUniformLocation(programID, "imageAspectInvert");
    	
        // Texture
    	glGenTextures(1, id, 0);
        glBindTexture(GL_TEXTURE_2D, id[0]);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, FRAMESIZE, FRAMESIZE, 0, GL_RGBA, GL_UNSIGNED_SHORT_5_5_5_1, null);
               
        // Vertex Buffer
        FloatBuffer vertexData = ByteBuffer.allocateDirect(vertexBufferData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(vertexBufferData);
        glGenBuffers(1, id, 1);        
        glBindBuffer(GL_ARRAY_BUFFER, id[1]);
        glBufferData(GL_ARRAY_BUFFER, vertexBufferData.length * 4, vertexData.position(0), GL_STATIC_DRAW);
        
        // Vertex pointers
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * 4, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * 4, 4 * 2);
    }
    
    @Override public void onSurfaceChanged(GL10 gl, int aWidth, int aHeight)
    {
        glViewport(0, 0, aWidth, aHeight);
        
        glUniform1f(id[2], aWidth);
        glUniform1f(id[3], aHeight);
    }
    
    public static void setSmoothingMode(boolean aEnable)
    {
    	smoothMode = aEnable;
    }
    
    public static void setForcedAspect(boolean aUseCustom, float aAspect)
    {
    	aspectMode = aUseCustom;
    	aspectForce = aAspect;
    }
    
    @Override public void onDrawFrame(GL10 gl)
    {
    	try
    	{
	    	VideoFrame next = readyFrames.poll(250, TimeUnit.MILLISECONDS);
	    	
	    	if(null != next)
	    	{	
	    		if((0 < next.size[0]) && (0 < next.size[1]))
	    		{
	    			// Do this first, the emulator is waiting to get the buffer back!
	    	    	glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, next.size[0], next.size[1], GL_RGBA, GL_UNSIGNED_SHORT_5_5_5_1, next.pixels);
	    	    	
	    	    	float width = (float)next.size[0];
	    	    	float height = (float)next.size[1];
	    	    	float aspect = next.aspect;
	    	    	float rotate = (1 == (next.size[2] & 1)) ? 1.0f : 0.0f;
	    	    	
	    	        emptyFrames.put(next);
	    	        
	    	        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, smoothMode ? GL_LINEAR : GL_NEAREST);
	    	        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, smoothMode ? GL_LINEAR : GL_NEAREST);
	    	        
	    	        // Now send the rest to OpenGL    	        
	    	        glUniform1f(id[4], width);
	    			glUniform1f(id[5], height);
	    			glUniform1f(id[6], !aspectMode ? aspect : ((aspectForce < 0.0f) ? (width / height) : aspectForce));
	    			glUniform1f(id[7], rotate);
	    		
	    			glDrawArrays(GL_TRIANGLE_STRIP, next.size[2] * 4, 4);
	    		}
	    		else
	    		{
	    			emptyFrames.put(next);
	    		}
	    	}
    	}
    	catch(InterruptedException e)
    	{
    		Thread.currentThread().interrupt();
    	}
    }
}
