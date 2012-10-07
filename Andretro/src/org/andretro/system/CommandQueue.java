package org.andretro.system;
import java.util.concurrent.*;

import android.app.*;

public class CommandQueue
{
    private final BlockingQueue<BaseCommand> eventQueue = new ArrayBlockingQueue<BaseCommand>(8);
	private Thread thread;
	
    /**
     * Must be called before any other function
     * @param aThread
     */
    public void setThread(Thread aThread)
    {
    	if(null != thread)
    	{
    		throw new RuntimeException("setThread must be called once and only once.");
    	}
    	
    	thread = aThread;
    }
    
    public Thread getThread()
    {
    	return thread;
    }
	
    public void assertThread()
    {
    	if(null == thread || Thread.currentThread() != thread)
    	{
    		throw new RuntimeException("This function must only be called on the specified thread.");
    	}
    }

    public void assertNotThread()
    {
    	if(null == thread || Thread.currentThread() == thread)
    	{
    		throw new RuntimeException("This function must not be called on the specified thread.");
    	}
    }
            
    public void queueCommand(final BaseCommand aCommand)
    {
    	assertNotThread();
    	
		// Put the event in the queue and notify any waiting clients that it's present. (This will wake the waiting emulator if needed.)
		eventQueue.add(aCommand);
		
		synchronized(thread)
		{
			thread.notifyAll();
		}
    }

    public void pump()
    {
    	assertThread();    	

    	// Run all events
    	for(BaseCommand i = eventQueue.poll(); null != i; i = eventQueue.poll())
    	{
    		i.run();
    	}
    }	
	
    public static class Callback
    {
        private final Runnable callback;
        private final Activity activity;
        
        public Callback(Activity aActivity, Runnable aCallback)
        {
            callback = aCallback;
            activity = aActivity;
            
            if(null == callback || null == activity)
            {
                throw new RuntimeException("Neither aCallback nor aActivity may be null.");
            }
        }
        
        public void perform()
        {
            activity.runOnUiThread(callback);
        }
    }

	public static abstract class BaseCommand implements Runnable
	{
		private final Callback finishedCallback;
		
		public BaseCommand(Callback aFinished)
		{
			finishedCallback = aFinished;
		}
		
		@Override public final void run()
		{
			perform();
			
			if(null != finishedCallback)
			{
				finishedCallback.perform();
			}
		}
		
		abstract protected void perform();
	}
}