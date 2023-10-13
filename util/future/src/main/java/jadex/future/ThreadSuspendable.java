package jadex.future;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import jadex.common.SUtil;
import jadex.common.TimeoutException;

/**
 *  Suspendable for threads.
 */
public class ThreadSuspendable extends ThreadLocalTransferHelper implements ISuspendable
{
	/** Threads waiting due to thread suspendable. */
	// Stored here and not in thread suspendable, because thread suspendable is closer to user.
	public static final Map<Thread, Future<?>>	WAITING_THREADS	= Collections.synchronizedMap(new HashMap<Thread, Future<?>>());
	
	//-------- attributes --------
	
	/** The future. */
	protected IFuture<?> future;
	
	/** The resumed flag to differentiante from timeout.*/
	protected boolean	resumed;
	
	protected Semaphore semaphore = new Semaphore(0);
	
	//-------- methods --------
	
	/**
	 *  Suspend the execution of the suspendable.
	 *  @param timeout The timeout.
	 *  @param realtime Flag if timeout is realtime (in contrast to simulation time).
	 */
	public void suspend(Future<?> future, long timeout, boolean realtime)
	{
		if(timeout==Future.UNSET)
			timeout = getDefaultTimeout();
		
		long endtime = timeout>0 ? System.currentTimeMillis()+timeout : -1;
		
		synchronized(this)
		{
			this.future	= future;
			this.resumed	= false;
			assert !WAITING_THREADS.containsKey(Thread.currentThread());
			WAITING_THREADS.put(Thread.currentThread(), future);
		}
		
		try
		{
			long waittime	= endtime-System.currentTimeMillis();
			if(!resumed && (endtime==-1 || waittime>0))
			{
				if(endtime==-1)
				{
					//if(!semaphore.tryAcquire(10000, TimeUnit.MILLISECONDS))
					//	System.out.println("not released!");
					semaphore.acquire();
					//this.wait();
				}
				else
				{
					semaphore.tryAcquire(waittime, TimeUnit.MILLISECONDS);
					//this.wait(waittime);
				}
			}
		}
		catch(InterruptedException e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		finally
		{
			synchronized(this)
			{
				assert WAITING_THREADS.get(Thread.currentThread())==future;
				WAITING_THREADS.remove(Thread.currentThread());
				// Restore the thread local values after switch
				afterSwitch();
				this.future	= null;
			}
		}
			
		if(!resumed)
		{
			if(timeout>0)
			{
				throw new TimeoutException("Timeout: "+timeout+", realtime="+realtime);
			}
			else
			{
				throw new IllegalStateException("Future.wait() returned unexpectedly. Timeout: "+timeout+", realtime="+realtime);
			}
		}
	}
	
	/**
	 *  Resume the execution of the suspendable.
	 */
	public void resume(Future<?> future)
	{
		boolean doresume = false;
		synchronized(this)
		{
			// Only wake up if still waiting for same future (invalid resume might be called from outdated future after timeout already occurred).
			if(future==this.future)
			{
				resumed	= true;
				// Save the thread local values before switch
				beforeSwitch();
				//this.notify();
				doresume = true;
			}
		}
		if(doresume)
			semaphore.release();
	}
	
	/**
	 *  Get the monitor for waiting.
	 *  @return The monitor.
	 */
	public Object getMonitor()
	{
		return this;
	}
	
	/**
	 *  Get the default timeout.
	 *  @return The default timeout (-1 for none).
	 */
	protected long getDefaultTimeout()
	{
		return SUtil.DEFTIMEOUT;
	}
}
