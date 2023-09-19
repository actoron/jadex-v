package jadex.mj.feature.execution.impl;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ISuspendable;
import jadex.mj.core.MjComponent;
import jadex.mj.feature.execution.IMjExecutionFeature;

public class MjExecutionFeature	implements IMjExecutionFeature
{
	protected static final ThreadPoolExecutor	THREADPOOL	= new ThreadPoolExecutor(0, Integer.MAX_VALUE, 3, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
	public static final ThreadLocal<MjExecutionFeature>	LOCAL	= new ThreadLocal<>();

	protected Queue<Runnable>	steps	= new ArrayDeque<>();
	protected boolean	executing;
	protected boolean	do_switch;
	protected ThreadRunner	runner	= null;
	protected MjComponent	self	= null;
	
	@Override
	public MjComponent getComponent()
	{
		if(self==null)
		{
			throw new IllegalStateException("Component can not be accessed in 'beforeCreation' bootstrapping.");
		}
		return self;
	}
	
	@Override
	public void scheduleStep(Runnable r)
	{
		boolean	startnew	= false;
		synchronized(this)
		{
			steps.offer(r);
			if(!executing)
			{
				startnew	= true;
				executing	= true;
				busy();
			}
		}
		
		if(startnew)
		{
			if(runner==null)
			{
				runner	= new ThreadRunner();
			}
			THREADPOOL.execute(runner);
		}
	}
	
	@Override
	public <T> IFuture<T> scheduleStep(Supplier<T> s)
	{
		Future<T>	ret	= new Future<>();
		scheduleStep(() ->
		{
			try
			{
				T res = s.get();
				if(res instanceof Future)
				{
					((Future<T>)res).delegateTo(ret);
				}
				else
				{
					ret.setResult(res);
				}
			}
			catch(Exception e)
			{
				ret.setException(e);
			}
			catch(Throwable t)
			{
				ret.setException(new RuntimeException("Error in step", t));
			}
		});
		return ret;
	}
	
	
	
	// Use generic connection method to avoid issues with different future types.
	

	
	/**
	 *  Test if the current thread is used for current component execution.
	 *  @return True, if it is the currently executing component thread.
	 */
	public boolean isComponentThread()
	{
		return this==LOCAL.get();
	}
	
	protected static volatile Timer	timer;
	protected static volatile int	timer_entries;
	
	@Override
	public IFuture<Void> waitForDelay(long millis)
	{
		Future<Void>	ret	= new Future<>();
		
		synchronized(this.getClass())
		{
			if(timer==null)
			{
				timer	= new Timer();
			}
			timer_entries++;
			timer.schedule(new TimerTask()
			{
				@Override
				public void run()
				{
					ret.setResult(null);
					
					synchronized(MjExecutionFeature.this.getClass())
					{
						timer_entries--;
						if(timer_entries==0)
						{
							timer.cancel();
							timer	= null;
						}
					}
				}
			}, millis);
		}
		
		return  ret;
	}
	
	@Override
	public long getTime()
	{
		return System.currentTimeMillis();
	}
	
	/**
	 *  Template method to schedule operations
	 *  whenever execution temporarily ends, i.e.,
	 *  there are currently no more steps to execute.
	 *  
	 *  This method is called while holding the lock on the steps queue.
	 *  Make sure not to call any external activities when overriding this method,
	 *  otherwise deadlocks might occur.
	 *  Preferably, you should use scheduleStep() to execute your activity
	 *  after the method call ends. 
	 */
	protected void	idle()
	{
		// nop
	}

	
	/**
	 *  Template method to schedule operations
	 *  whenever execution starts/resumes.
	 *  
	 *  This method is called while holding the lock on the steps queue.
	 *  Make sure not to call any external activities when overriding this method,
	 *  otherwise deadlocks might occur.
	 *  Preferably, you should use scheduleStep() to execute your activity
	 *  after the method call ends. 
	 */
	protected void	busy()
	{
		// nop
	}

	protected class ThreadRunner implements Runnable
	{
		@Override
		public void run()
		{
			ISuspendable.SUSPENDABLE.set(new ComponentSuspendable());
			LOCAL.set(MjExecutionFeature.this);
			
			boolean hasnext	= true;
			while(hasnext)
			{
				Runnable	step;
				synchronized(MjExecutionFeature.this)
				{
					step	= steps.poll();
				}
				
				try
				{
					step.run();
				}
				catch(Throwable t)
				{
					new RuntimeException("Exception in step", t).printStackTrace();
				}
				
				synchronized(MjExecutionFeature.this)
				{
					if(do_switch)
					{
						do_switch	= false;
						hasnext	= false;							
					}
					else if(steps.isEmpty())
					{
						hasnext	= false;
						executing	= false;
						idle();
					}					
				}
			}
			ISuspendable.SUSPENDABLE.remove();
			LOCAL.remove();
		}
	}
	
	protected class ComponentSuspendable implements ISuspendable
	{
		@Override
		public void suspend(Future<?> future, long timeout, boolean realtime)
		{
			boolean startnew	= false;
			
			synchronized(MjExecutionFeature.this)
			{
				if(!steps.isEmpty())
				{
					startnew	= true;
				}
				else
				{
					executing	= false;
					idle();
				}
			}
	
			if(startnew)
			{
				if(runner==null)
				{
					runner	= new ThreadRunner();
				}
				THREADPOOL.execute(runner);
			}
			
			synchronized(this)
			{
				try
				{
					// TODO timeout?
					this.wait();
				}
				catch(InterruptedException e)
				{
				}
			}
		}
	
		@Override
		public void resume(Future<?> future)
		{
			scheduleStep(() ->
			{
				do_switch	= true;
				
				synchronized(this)
				{
					this.notify();
				}
			});
		}
	}
}
