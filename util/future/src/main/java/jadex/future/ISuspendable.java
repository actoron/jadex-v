package jadex.future;

import java.util.concurrent.locks.ReentrantLock;

import jadex.common.TimeoutException;

/**
 *  Interface for suspendable entities.
 *  Is used by the IFuture to suspend callers.
 */
public interface ISuspendable
{
	//-------- constants --------
	
	/** The component suspendable for a component thread. */
	public static final ThreadLocal<ISuspendable>	SUSPENDABLE	= new ThreadLocal<ISuspendable>();
//	public static final ScopedValue<ISuspendable> SUSPENDABLE = ScopedValue.newInstance();
//	{
//		public void set(ISuspendable value) 
//		{
//			if(value instanceof ThreadSuspendable)
//				System.out.println("setting: "+value);
//			super.set(value);
//		}
//	};
	
	//-------- methods --------
	
	/**
	 *  Suspend the execution of the suspendable.
	 *  @param future The future to wait for.
	 *  @param timeout The timeout (-1 for no timeout, -2 for default timeout).
	 *  @param realtime Flag if timeout is realtime (in contrast to simulation time).
	 *  @throws TimeoutException when not resumed before timeout.
	 */
	public void suspend(Future<?> future, long timeout, boolean realtime);
	
	/**
	 *  Resume the execution of the suspendable.
	 *  @param future The future that issues the resume.
	 */
	public void resume(Future<?> future);//, boolean force);
	
	/**
	 *  Get the future if currently suspended, null otherwise.
	 */
	public IFuture<?>	getFuture();
	
	/**
	 *  Return the lock for internal synchronization.
	 */
	public ReentrantLock getLock();
}
