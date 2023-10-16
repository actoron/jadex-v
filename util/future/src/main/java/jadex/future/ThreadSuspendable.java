package jadex.future;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

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
	
	/** The resumed flag to differentiate from timeout.*/
	protected boolean	resumed;
	
	/** Use reentrant lock/condition instead of synchronized/wait/notify to avoid pinning when using virtual threads. */
	protected ReentrantLock lock	= new ReentrantLock();
	protected Condition	wait	= lock.newCondition();
	
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
		
		try
		{
			lock.lock();
			this.future	= future;
			this.resumed	= false;
			assert !WAITING_THREADS.containsKey(Thread.currentThread());
			WAITING_THREADS.put(Thread.currentThread(), future);
			
			try
			{
				long waittime	= endtime-System.currentTimeMillis();
				if(!resumed && (endtime==-1 || waittime>0))
				{
					if(endtime==-1)
					{
						wait.await();
					}
					else
					{
						wait.await(waittime, TimeUnit.MILLISECONDS);
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
				assert WAITING_THREADS.get(Thread.currentThread())==future;
				WAITING_THREADS.remove(Thread.currentThread());
				// Restore the thread local values after switch
				afterSwitch();
				this.future	= null;
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
		finally
		{
			lock.unlock();
		}
	}
	
	/**
	 *  Resume the execution of the suspendable.
	 */
	public void resume(Future<?> future)
	{
		try
		{
			lock.lock();
			// Only wake up if still waiting for same future (invalid resume might be called from outdated future after timeout already occurred).
			if(future==this.future)
			{
				resumed	= true;
				// Save the thread local values before switch
				beforeSwitch();
				wait.signal();
			}
		}
		finally
		{
			lock.unlock();
		}
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
