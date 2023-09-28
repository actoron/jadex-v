package jadex.mj.feature.execution.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import jadex.common.SUtil;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ISuspendable;
import jadex.mj.core.IComponent;
import jadex.mj.core.IThrowingConsumer;
import jadex.mj.core.IThrowingFunction;
import jadex.mj.core.MjComponent;
import jadex.mj.feature.execution.ComponentTerminatedException;
import jadex.mj.feature.execution.IMjExecutionFeature;

public class MjExecutionFeature	implements IMjExecutionFeature, IMjInternalExecutionFeature
{
	protected static final ThreadPoolExecutor	THREADPOOL	= new ThreadPoolExecutor(0, Integer.MAX_VALUE, 3, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
	public static final ThreadLocal<MjExecutionFeature>	LOCAL	= new ThreadLocal<>();

	protected Queue<Runnable>	steps	= new ArrayDeque<>();
	protected boolean	executing;
	protected boolean	do_switch;
	protected boolean terminated;
	protected ThreadRunner	runner	= null;
	protected MjComponent	self	= null;
	
	@Override
	public MjComponent getComponent()
	{
		if(self==null)
			throw new IllegalStateException("Component can not be accessed in 'beforeCreation' bootstrapping.");
		return self;
	}
	
	@Override
	public void scheduleStep(Runnable r)
	{
		if(terminated)
			throw new ComponentTerminatedException(self.getId());
		
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
				runner	= new ThreadRunner();
			THREADPOOL.execute(runner);
		}
	}
	
	@Override
	public <T> IFuture<T> scheduleStep(Supplier<T> s)
	{
		Future<T> ret = new Future<>();
		
		if(terminated)
		{
			ret.setException(new ComponentTerminatedException(self.getId()));
			return ret;
		}
		
		scheduleStep(new StepInfo(() ->
		{
			try
			{
				T res = s.get();
				if(res instanceof Future)
				{
					@SuppressWarnings("unchecked")
					Future<T>	resfut	= (Future<T>)res;
					// Use generic connection method to avoid issues with different future types.
					resfut.delegateTo(ret);
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
		}, ret));
		return ret;
	}
	
	/**
	 *  Schedule a step that provides a result.
	 *  @param step	A step that is executed via the {@link Supplier#get()} method.
	 *  @return	A future that provides access to the step result, once it is available.
	 */
	public void scheduleStep(IThrowingConsumer<IComponent> step)
	{
		scheduleStep(() ->
		{
			try
			{
				step.accept(self);
			}
			catch(Exception e)
			{
				SUtil.rethrowAsUnchecked(e);
			}
			catch(Throwable t)
			{
				SUtil.rethrowAsUnchecked(t);
			}
		});
	}
	
	/**
	 *  Schedule a step that provides a result.
	 *  @param step	A step that is executed via the {@link Supplier#get()} method.
	 *  @return	A future that provides access to the step result, once it is available.
	 */
	public <T> IFuture<T> scheduleStep(IThrowingFunction<IComponent, T> step)
	{
		Future<T>	ret	= new Future<>();
		
		if(terminated)
		{
			ret.setException(new ComponentTerminatedException(self.getId()));
			return ret;
		}
		
		scheduleStep(new StepInfo(() ->
		{
			try
			{
				T res = step.apply(self);
				if(res instanceof Future)
				{
					@SuppressWarnings("unchecked")
					Future<T>	resfut	= (Future<T>)res;
					// Use generic connection method to avoid issues with different future types.
					resfut.delegateTo(ret);
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
		}, ret));
		return ret;
	}
	
	/**
	 *  Test if the current thread is used for current component execution.
	 *  @return True, if it is the currently executing component thread.
	 */
	public boolean isComponentThread()
	{
		return this==LOCAL.get();
	}
	
	// Global on-demand timer shared by all components.
	protected static volatile Timer	timer;
	//protected static volatile int	timer_entries;
	protected static volatile List<TimerTaskInfo> entries = new LinkedList<TimerTaskInfo>();
	
	@Override
	public IFuture<Void> waitForDelay(long millis)
	{
		Future<Void> ret = new Future<>();
		
		if(terminated)
		{
			ret.setException(new ComponentTerminatedException(self.getId()));
			return ret;
		}
		
		synchronized(this.getClass())
		{
			if(timer==null)
				timer = new Timer();
			//timer_entries++;
			
			TimerTaskInfo task = new TimerTaskInfo(self.getId(), ret);
			task.setTask(new TimerTask()
			{
				@Override
				public void run()
				{
					scheduleStep(() -> ret.setResult(null));
					
					synchronized(MjExecutionFeature.this.getClass())
					{
						//timer_entries--;
						entries.remove(task);
						if(entries.size()==0)
						{
							timer.cancel();
							timer = null;
						}
					}
				}
			});
			
			entries.add(task);
			
			timer.schedule(task.getTask(), millis);
		}
		
		return  ret;
	}
	
	class TimerTaskInfo
	{
		protected UUID cid;
		protected TimerTask task;
		protected Future<?> future;
		
		public TimerTaskInfo(UUID cid, Future<?> future)
		{
			this.cid = cid;
			this.future = future;
		}

		public UUID getComponentId() 
		{
			return cid;
		}

		public TimerTask getTask() 
		{
			return task;
		}
		
		public void setTask(TimerTask task) 
		{
			this.task = task;
		}

		public Future<?> getFuture() 
		{
			return future;
		}
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
			while(hasnext && !terminated)
			{
				Runnable	step;
				synchronized(MjExecutionFeature.this)
				{
					step	= steps.poll();
				}
				
				if(step==null)
					System.out.println("step is null");
				
				doRun(step);
				
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
		public ComponentSuspendable()
		{
			// TODO Auto-generated constructor stub
		}

		@Override
		public void suspend(Future<?> future, long timeout, boolean realtime)
		{
			boolean startnew	= false;
			
			synchronized(MjExecutionFeature.this)
			{
				if(!steps.isEmpty() && !terminated)
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
					runner	= new ThreadRunner();
				THREADPOOL.execute(runner);
			}
			
			beforeBlock();
			
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
			
			afterBlock();
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
	
	protected List<IStepListener>	listeners	= null;

	@Override
	public void addStepListener(IStepListener lis)
	{
		if(listeners==null)
			listeners	= new ArrayList<>();
		listeners.add(lis);
	}

	@Override
	public void removeStepListener(IStepListener lis)
	{
		assert listeners!=null;
		boolean	removed	= listeners.remove(lis);
		assert removed;
	}
	
	@Override
	public void terminate()
	{
		if(terminated)
			return;
		
		terminated = true;
		
		//System.out.println("terminate start");
			
		ComponentTerminatedException ex = new ComponentTerminatedException(self.getId());
		for(Object step: steps)
		{
			if(step instanceof StepInfo)
			{
				((StepInfo)step).getFuture().setException(ex);
			}
		}
		
		synchronized(MjExecutionFeature.this)
		{
			steps.clear();
		}
		
		TimerTaskInfo[] ttis = entries.toArray(new TimerTaskInfo[entries.size()]);
		for(TimerTaskInfo tti: ttis)
		{
			if(self.getId().equals(tti.getComponentId()))
			{
				tti.getTask().cancel();
				tti.getFuture().setException(ex);
				entries.remove(tti);
			}
		}
		
		//System.out.println("terminate end");
	}
	
	protected void beforeStep()
	{
		if(listeners!=null)
		{
			for(IStepListener lis : listeners)
			{
				lis.beforeStep();
			}
		}
	}
	
	protected void afterStep()
	{
		if(listeners!=null)
		{
			for(IStepListener lis : listeners)
			{
				lis.afterStep();
			}
		}
	}
	
	protected void beforeBlock()
	{
		if(listeners!=null)
		{
			for(IStepListener lis : listeners)
			{
				lis.beforeBlock();
			}
		}
	}
	
	protected void afterBlock()
	{
		if(listeners!=null)
		{
			for(IStepListener lis : listeners)
			{
				lis.afterBlock();
			}
		}
	}

	/**
	 *  Template method to allow augmentation/alteration of step execution.
	 *  Responsible for beforeStep/afterStep calls.
	 */
	public void doRun(Runnable step)
	{		
		beforeStep();
		
		try
		{
			step.run();
		}
		catch(Throwable t)
		{
			new RuntimeException("Exception in step", t).printStackTrace();
		}
		
		afterStep();
	}
	
	class StepInfo implements Runnable
	{
		Runnable step;
		Future<?> future;
		
		public StepInfo(Runnable step, Future<?> future)
		{
			this.step	= step;
			this.future = future;
		}
		
		public Runnable getStep() 
		{
			return step;
		}
		
		public Future<?> getFuture() 
		{
			return future;
		}

		@Override
		public void run()
		{
			doRun(step);
		}
	}

}
