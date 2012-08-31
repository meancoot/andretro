package org.andretro.system;
import org.andretro.R;

import java.io.*;
import java.nio.*;
import java.util.concurrent.*;

import android.content.*;

import static android.opengl.GLES20.*;

// NOT THREAD SAFE
public final class Present
{	
    private static final int FRAMESIZE = 1024;

    private static int programID;
    private static final int id[] = new int[8];
    
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
    	public final ByteBuffer pixels = ByteBuffer.allocateDirect(1024 * 1024 * 2);
    	public final int[] size = new int[3];
    	public float aspect;
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
    private static int buildShader(int aType, final InputStream aSource)
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
    	
    	int result = glCreateShader(aType);
    	glShaderSource(result, source);
    	glCompileShader(result);
    	
    	int[] state = new int[1];
    	glGetShaderiv(result, GL_COMPILE_STATUS, state, 0);
    	if(0 == state[0])
    	{
    		System.out.println(glGetShaderInfoLog(result));
    	}
    	
    	return result;
    }
    
    public static void initialize(Context aContext)
    {    
    	// Program    	
    	programID = glCreateProgram();
    	glAttachShader(programID, buildShader(GL_VERTEX_SHADER, aContext.getResources().openRawResource(R.raw.vertex_shader)));
    	glAttachShader(programID, buildShader(GL_FRAGMENT_SHADER, aContext.getResources().openRawResource(R.raw.fragment_shader)));
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
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
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
    
    public static void setScreenSize(int aWidth, int aHeight)
    {
        glViewport(0, 0, aWidth, aHeight);
        
        glUniform1f(id[2], aWidth);
        glUniform1f(id[3], aHeight);
    }
        
    public static void present() throws InterruptedException
    {
    	VideoFrame next = readyFrames.poll();
    	
    	if(null != next && 0 < next.size[0] && 0 < next.size[1])
    	{
    		glUniform1f(id[4], (float)next.size[0]);
    		glUniform1f(id[5], (float)next.size[1]);
    		glUniform1f(id[6], next.aspect);
    		glUniform1f(id[7], (1 == (next.size[2] & 1)) ? 1.0f : 0.0f);
    		
	    	glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, next.size[0], next.size[1], GL_RGBA, GL_UNSIGNED_SHORT_5_5_5_1, next.pixels);
	        glDrawArrays(GL_TRIANGLE_STRIP, next.size[2] * 4, 4);

	        emptyFrames.put(next);
    	}
    }
}
