package org.andretro.system;
import android.media.*;

/**
 * A stateless audio stream.
 * 
 * Underlying audio object is created and managed transparently. User only needs to call write().
 * @author jason
 *
 */
public final class Audio
{
    private static AudioTrack audio;
    private static int rate;

    public synchronized static void open(int aRate)
    {
        close();

        rate = aRate;

        audio = new AudioTrack(AudioManager.STREAM_MUSIC, aRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, 24000, AudioTrack.MODE_STREAM);
        audio.setStereoVolume(1, 1);
        audio.play();
    }

    public synchronized static void close()
    {
        if(null != audio)
        {
            audio.stop();
            audio.release();
        }
        
        audio = null;
    }

    public synchronized static void write(int aRate, short aSamples[], int aCount)
    {
        // Check args
        if(null == aSamples || aCount < 0 || aCount >= aSamples.length)
        {
            throw new IllegalArgumentException("Invalid audio stream chunk.");
        }
    
        // Create audio if needed
        if(null == audio || aRate != rate)
        {
            open(aRate);
        }
    
        // Write samples
        audio.write(aSamples, 0, aCount);
    }
}

