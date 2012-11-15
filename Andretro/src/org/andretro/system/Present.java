package org.andretro.system;
import org.andretro.R;
import org.andretro.emulator.*;
import org.libretro.*;

import java.io.*;
import java.nio.*;

import javax.microedition.khronos.opengles.*;

import android.content.*;
import android.opengl.*;

import static android.opengl.GLES20.*;

// NOT THREAD SAFE
public final class Present implements GLSurfaceView.Renderer
{	
	final LibRetro.VideoFrame frame = new LibRetro.VideoFrame();
	
	public static class Texture
	{
		private static final int id[] = new int[1];
		private static boolean smoothMode = true;
		
		private static void create()
		{
	    	glGenTextures(1, id, 0);
	        glBindTexture(GL_TEXTURE_2D, id[0]);
	        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
	        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
	        
	        setSmoothMode(smoothMode);
		}
			    	    
	    public static void setSmoothMode(boolean aEnable)
	    {
	    	smoothMode = aEnable;
	    	
	        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, aEnable ? GL_LINEAR : GL_NEAREST);
	        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, aEnable ? GL_LINEAR : GL_NEAREST);	    	
	    }
	}
	
	public static class VertexData
	{
		private static final float TEXTURESIZE = 1024.0f;
		private static final int[] id = new int[1];
		
	    private static final float[] vertexData = new float[]
	    {
	    	-1, 1, 0, 0, 1, 1, 1, 0, -1, -1, 0, 1, 1, -1, 1, 1,
	    	-1, 1, 1, 0, 1, 1, 1, 1, -1, -1, 0, 0, 1, -1, 0, 1,
	    	-1, 1, 1, 1, 1, 1, 0, 1, -1, -1, 1, 0, 1, -1, 0, 0,
	    	-1, 1, 0, 1, 1, 1, 0, 0, -1, -1, 1, 1, 1, -1, 1, 0
	    };
	    
	    private static int screenWidth;
	    private static int screenHeight;
	    private static int imageWidth;
	    private static int imageHeight;
	    private static float aspect;
	    private static boolean rotate;
	    private static volatile boolean aspectMode = false;
	    private static volatile float aspectForce = 0.0f;
	    
	    private static boolean needUpdate = true;
	    
	    private static void create()
	    {
	        glGenBuffers(1, id, 0);        
	        glBindBuffer(GL_ARRAY_BUFFER, id[0]);
	    }
	    
	    private static void setScreenSize(int aWidth, int aHeight)
	    {
    		needUpdate = needUpdate || (aWidth != screenWidth) || (aHeight != screenHeight);
    		
	    	screenWidth = aWidth;
	    	screenHeight = aHeight;
	    }
	    
	    private static void setImageData(int aWidth, int aHeight, float aAspect, boolean aRotate)
	    {
    		needUpdate = needUpdate || (aWidth != imageWidth) || (aHeight != imageHeight) || (aRotate != rotate) || (aAspect != aspect);
	    	
	    	imageWidth = aWidth;
	    	imageHeight = aHeight;
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
		
		private static void checkUpdate()
	    {
	    	if(needUpdate)
	    	{
	    		update((float)screenWidth, (float)screenHeight, (float)imageWidth, (float)imageHeight);
	    		needUpdate = false;
	    	}
	    }
	    
	    private static void update(float aScreenWidth, float aScreenHeight, float aImageWidth, float aImageHeight)
	    {
	    	final FloatBuffer vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

	    	float inputAspect = (aspect <= 0.00001f) ? aImageWidth / aImageHeight : aspect;
	    	inputAspect = aspectMode ? aspectForce : inputAspect;
	    	inputAspect = !rotate ? inputAspect : 1.0f / inputAspect;
	    	
	    	float outputAspect = aScreenWidth / aScreenHeight;
	    	
	    	float width = (!(outputAspect < inputAspect)) ? aScreenHeight * inputAspect : aScreenWidth;
	    	float height = (outputAspect < inputAspect) ? aScreenWidth / inputAspect : aScreenHeight;
	    		    	
	    	for(int i = 0; i != 16; i ++)
	    	{
	    		final int idx = i * 4;
	    		
	    		vertexBuffer.put(idx + 0, (vertexData[idx + 0] * width) / aScreenWidth);
	    		vertexBuffer.put(idx + 1, (vertexData[idx + 1] * height) / aScreenHeight);
	    		vertexBuffer.put(idx + 2, ((vertexData[idx + 2] * aImageWidth) - (.5f * vertexData[idx + 2])) / TEXTURESIZE);
	    		vertexBuffer.put(idx + 3, ((vertexData[idx + 3] * aImageHeight) - (.5f * vertexData[idx + 3])) / TEXTURESIZE);
	    	}
	        
	        glBufferData(GL_ARRAY_BUFFER, vertexData.length * 4, vertexBuffer.position(0), GL_STATIC_DRAW);
	    	
	        glEnableVertexAttribArray(0);
	        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * 4, 0);
	        glEnableVertexAttribArray(1);
	        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * 4, 4 * 2);
	    }
	}
	
    private static int programID;
    private static String vertexShader;
    private static String fragmentShader;
        
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
    	
        // Objects
        Texture.create();
        VertexData.create();
    }
    
    @Override public void onSurfaceChanged(GL10 gl, int aWidth, int aHeight)
    {
        glViewport(0, 0, aWidth, aHeight);
        
        VertexData.setScreenSize(aWidth, aHeight);        
        frame.restarted = true;
    }
        
    @Override public void onDrawFrame(GL10 gl)
    {
    	if(Game.doFrame(frame))
    	{
    		VertexData.setImageData(frame.width, frame.height, frame.aspect, (frame.rotation & 1) == 1);
    		VertexData.checkUpdate();		
			glDrawArrays(GL_TRIANGLE_STRIP, frame.rotation * 4, 4);
    	}
    }
}
