package jadex.mj.feature.execution.impl;

import java.util.ArrayDeque;
import java.util.Map;
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
import jadex.mj.core.impl.MjFeatureProvider;
import jadex.mj.core.impl.SMjFeatureProvider;
import jadex.mj.feature.execution.IMjExecutionFeature;

public class MjExecutionFeature	implements IMjExecutionFeature
{
	protected  static final ThreadPoolExecutor	THREADPOOL	= new ThreadPoolExecutor(0, Integer.MAX_VALUE, 3, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());

	public static void	bootstrap(Class<? extends MjComponent> type, Supplier<? extends MjComponent> creator)
	{
		Map<Class<Object>, MjFeatureProvider<Object>>	providers	= SMjFeatureProvider.getProvidersForComponent(type);
		MjFeatureProvider<Object>	exeprovider	= providers.get(IMjExecutionFeature.class);
		IMjExecutionFeature	exe	= (IMjExecutionFeature)exeprovider.createFeatureInstance(null);
		exe.scheduleStep(() ->
		{
			MjExecutionFeatureProvider.BOOTSTRAP_FEATURE.set(exe);
			creator.get();
		});
	}
	
	protected Queue<Runnable>	steps	= new ArrayDeque<>();
	protected boolean	running;
	protected boolean	do_switch;
	protected ThreadRunner	runner	= null;
	
	@Override
	public void scheduleStep(Runnable r)
	{
		boolean	startnew	= false;
		synchronized(this)
		{
			steps.offer(r);
			if(!running)
			{
				startnew	= true;
				running	= true;
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
				ret.setResult(s.get());
			}
			catch(Exception e)
			{
				ret.setException(e);
			}
			catch(Throwable t)
			{
				ret.setException(new RuntimeException("Erro in step", t));
			}
		});
		return ret;
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
	
	protected class ThreadRunner implements Runnable, ISuspendable
	{
		@Override
		public void run()
		{
			ISuspendable.SUSPENDABLE.set(this);
			
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
						do_switch	= false;
						hasnext	= false;
						running	= false;
					}
				}
			}
			ISuspendable.SUSPENDABLE.remove();
		}
		
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
					running	= false;
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
