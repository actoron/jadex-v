package jadex.mj.feature.execution.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jadex.common.SUtil;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ISuspendable;
import jadex.mj.core.ComponentIdentifier;
import jadex.mj.core.IComponent;
import jadex.mj.core.IThrowingConsumer;
import jadex.mj.core.IThrowingFunction;
import jadex.mj.core.MjComponent;
import jadex.mj.feature.execution.ComponentTerminatedException;
import jadex.mj.feature.execution.IMjExecutionFeature;

public class MjExecutionFeature	implements IMjExecutionFeature, IMjInternalExecutionFeature
{
	// TODO: how to prefer lifecycle termination to execution termination
//	static
//	{
//		IComponent.addComponentTerminator(new IComponentTerminator() 
//		{
//			public boolean filter(MjComponent component) 
//			{
//				return component.getClass().equals(MjComponent.class);
//			}
//			
//			@Override
//			public void terminate(IComponent component) 
//			{
//				((IMjInternalExecutionFeature)component.getFeature(IMjExecutionFeature.class)).terminate();
//			}
//		});
//	}
	
	public static ExecutorService EXECUTOR;
	public static final ThreadLocal<MjExecutionFeature>	LOCAL	= new ThreadLocal<>();
//	public static final ScopedValue<MjExecutionFeature> LOCAL = ScopedValue.newInstance();

	protected Queue<Runnable>	steps	= new ArrayDeque<>();
	protected boolean	executing;
	protected boolean	do_switch;
	protected boolean terminated;
	protected ThreadRunner	runner	= null;
	protected MjComponent	self	= null;
	protected Object endstep = null;
	protected Future<Object> endfuture = null;
	
	protected static ExecutorService getExecutor()
	{
		if(EXECUTOR==null)
		{
			synchronized(MjExecutionFeature.class)
			{
				if(EXECUTOR==null)
				{
					try
					{
						//EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
						EXECUTOR	= (ExecutorService)Executors.class.getMethod("newVirtualThreadPerTaskExecutor").invoke(null);
						System.out.println("Using virtual threads :-)");
					}
					catch(NoSuchMethodException e)
					{
						System.out.println("Using OS threads :-(");
						EXECUTOR = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 3, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
					}
					catch(Exception e)
					{
						throw SUtil.throwUnchecked(e);
					}
				}
			}
		}
		return EXECUTOR;
	}
	
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
			runTask(runner);
			//THREADPOOL.execute(runner);
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
				
				if(!saveEndStep(res, (Future)ret))
				{
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
				
				if(!saveEndStep(res, (Future)ret))
				{
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
//		return LOCAL.isBound()? this==LOCAL.get(): null;
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
		protected ComponentIdentifier cid;
		protected TimerTask task;
		protected Future<?> future;
		
		public TimerTaskInfo(ComponentIdentifier cid, Future<?> future)
		{
			this.cid = cid;
			this.future = future;
		}

		public ComponentIdentifier getComponentId() 
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

	/** Keep track of threads in use to unblock on terminate. */
	protected List<ComponentSuspendable>	threads	= new ArrayList<>();
	
	protected class ThreadRunner implements Runnable
	{
		@Override
		public void run()
		{
			ComponentSuspendable	sus	= new ComponentSuspendable();
			// synchronized because another thread could exit in parallel
			synchronized(this)
			{
				threads.add(sus);
			}
			ISuspendable.SUSPENDABLE.set(sus);
			LOCAL.set(MjExecutionFeature.this);
//			ScopedValue.where(ISuspendable.SUSPENDABLE, sus).run(()->
//			{
//				ScopedValue.where(LOCAL, MjExecutionFeature.this).run(()->
//				{
					boolean hasnext	= true;
					while(hasnext && !terminated)
					{
						Runnable	step;
						synchronized(MjExecutionFeature.this)
						{
							step	= steps.poll();
						}
						
						assert step!=null;
						
						try
						{
							doRun(step);
						}
						catch(ThreadDeath d)
						{
							assert terminated;
							// ignore aborted steps.
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
					// synchronized because multiple threads could exit in parallel (e.g. after unblocking a future)
					synchronized(this)
					{
						threads.remove(sus);
					}
//				});
//			});
			ISuspendable.SUSPENDABLE.remove();
			LOCAL.remove();
		}
	}
	
	protected class ComponentSuspendable implements ISuspendable
	{
		/** Check if currently blocked. */
		protected boolean	blocked;
		
		/** Set by terminate() to indicate step abortion. */  
		protected boolean	aborted;
		
		/** Use reentrant lock/condition instead of synchronized/wait/notify to avoid pinning when using virtual threads. */
		protected ReentrantLock lock	= new ReentrantLock();
		protected Condition	wait	= lock.newCondition();
		
		@Override
		public void suspend(Future<?> future, long timeout, boolean realtime)
		{
			assert !blocked;
			assert !aborted;
			
			boolean startnew	= false;
			
			synchronized(MjExecutionFeature.this)
			{
				if(!steps.isEmpty() && !aborted)
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
				//THREADPOOL.execute(runner);#
				runTask(runner);
			}
			
			beforeBlock();
			
			try
			{
				lock.lock();
				try
				{
					blocked	= true;
					// TODO timeout?
					wait.await();
				}
				catch(InterruptedException e)
				{
				}
				finally
				{
					blocked=false;
					if(aborted)
						throw new ThreadDeath();
				}
			}
			finally
			{
				lock.unlock();
			}
			
			afterBlock();
		}
	
		@Override
		public void resume(Future<?> future)
		{
			if(!aborted)
			{
				scheduleStep(() ->
				{
					try
					{
						lock.lock();
						do_switch	= true;
						wait.signal();
					}
					finally
					{
						lock.unlock();
					}
				});
			}
		}
		
		/**
		 *  Unblock and exit thread with ThreadDeath.
		 *  Has no effect when not blocked.
		 */
		protected void	abort()
		{
			assert !aborted;
			
			// Only terminate blocked threads -> the running thread is doing the termination and needs to stay alive ;-)
			if(blocked)
			{
				aborted	= true;
				try
				{
					lock.lock();
					wait.signal();
				}
				finally
				{
					lock.unlock();
				}
			}
		}
		
		@Override
		public ReentrantLock getLock()
		{
			return lock;
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
		
		executeEndStep();
		
		terminated = true;
		
		//System.out.println("terminate start: "+self.getId()+" "+steps.size());
		
		// Terminate blocked threads
		// Do first to unblock futures before setting results later
		// Use copy as threads remove themselves from list on exit. 
		List<ComponentSuspendable>	mythreads;
		synchronized(this)
		{
			mythreads	= new ArrayList<>(threads);
		}
		mythreads.forEach(thread -> thread.abort());
		
		// Drop queued steps.
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
		
		// Drop queued timer tasks.
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
		catch(ThreadDeath t)
		{
			// pass thread death to thread runner main loop
			throw t;
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
	
	public void runTask(Runnable task)
	{
		getExecutor().execute(task);
		/*if(THREADPOOL!=null)
		{
			THREADPOOL.execute(runner);
		}
		else
		{
			String name = self!=null? self.getId().getLocalName(): "bootstrap";
			//Thread.ofVirtual().name(name).start(runner);
			VIRTEXECUTOR.execute(runner);
		}*/
	}
	
	protected boolean saveEndStep(Object res, Future<Object> fut)
	{
		boolean ret = false;
		
		if(res instanceof IFuture) // Future is Supplier :(
			return ret;
		
		if(res instanceof Function || res instanceof IThrowingFunction || 
			res instanceof Consumer || res instanceof IThrowingConsumer ||
			res instanceof Runnable || res instanceof Supplier)
		{
			if(endstep==null)
			{
				endstep = res;
				endfuture = fut;
				ret = true;
				//System.out.println("endstep: "+self.getId()+" "+this.hashCode());
			}
			else
			{
				throw new RuntimeException("Only one endstep allowed: "+endstep+" "+res);
			}
		}
		return ret;
	}
	
	protected void executeEndStep()
	{
		if(endstep!=null)
		{
			if(endstep instanceof IThrowingFunction)
			{
				try
				{
					Object ret = ((IThrowingFunction<IComponent, Object>)endstep).apply(getComponent());
					endfuture.setResult(ret);
				}
				catch(Exception e)
				{
					endfuture.setException(e);
				}
			}
			else if(endstep instanceof Function)
			{
				try
				{
					Object ret = ((Function<IComponent, Object>)endstep).apply(getComponent());
					endfuture.setResult(ret);
				}
				catch(Exception e)
				{
					endfuture.setException(e);
				}
			}
			else if(endstep instanceof IThrowingConsumer)
			{
				try
				{
					((IThrowingConsumer<IComponent>)endstep).accept(getComponent());
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			else if(endstep instanceof Consumer)
			{
				try
				{
					((Consumer<IComponent>)endstep).accept(getComponent());
				}
				catch(Exception e)
				{
					e.printStackTrace();;
				}
			}
			else if(endstep instanceof Runnable)
			{
				try
				{
					((Runnable)endstep).run();
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			else if(endstep instanceof Supplier)
			{
				try
				{
					Object ret = ((Supplier)endstep).get();
					endfuture.setResult(ret);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}
}
