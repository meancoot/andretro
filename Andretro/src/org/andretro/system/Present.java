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
	
    private static int programID;
    private static final int id[] = new int[8];
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
        Texture.create();
        
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
        
        frame.restarted = true;
    }
        
    public static void setForcedAspect(boolean aUseCustom, float aAspect)
    {
    	aspectMode = aUseCustom;
    	aspectForce = aAspect;
    }

    @Override public void onDrawFrame(GL10 gl)
    {
    	if(Game.doFrame(frame))
    	{
	    	final float width = frame.width;
	    	final float height = frame.height;
	    	final float aspect = frame.aspect;
	    	final float rotate = (1 == (frame.rotation & 1)) ? 1.0f : 0.0f;
	    	final int rotateMode = frame.rotation;
	            	        
	        // Now send the rest to OpenGL    	        
	        glUniform1f(id[4], width);
			glUniform1f(id[5], height);
			glUniform1f(id[6], !aspectMode ? aspect : ((aspectForce < 0.0f) ? (width / height) : aspectForce));
			glUniform1f(id[7], rotate);
		
			glDrawArrays(GL_TRIANGLE_STRIP, rotateMode * 4, 4);
    	}
    }
}
