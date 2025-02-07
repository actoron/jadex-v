package jadex.execution.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jadex.common.SUtil;
import jadex.core.ComponentIdentifier;
import jadex.core.ComponentTerminatedException;
import jadex.core.ICallable;
import jadex.core.IComponent;
import jadex.core.IThrowingConsumer;
import jadex.core.IThrowingFunction;
import jadex.core.impl.Component;
import jadex.execution.IExecutionFeature;
import jadex.execution.StepAborted;
import jadex.execution.future.FutureFunctionality;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ISuspendable;
import jadex.future.ITerminableFuture;
import jadex.future.TerminableFuture;

public class ExecutionFeature	implements IExecutionFeature, IInternalExecutionFeature
{
	/** Provide access to the execution feature when running inside a component. */
	public static final ThreadLocal<ExecutionFeature>	LOCAL	= new ThreadLocal<>();
	
	private Queue<Runnable> steps = new ArrayDeque<>(4);
	protected volatile boolean executing;
	protected volatile boolean do_switch;
	protected boolean terminated;
	protected ThreadRunner runner = null;
	protected Component	self = null;
	protected Object endstep = null;
	protected Future<Object> endfuture = null;
	
	// Debug Heisenbug
	AtomicInteger	threadcount	= new AtomicInteger();
	
	@Override
	public IComponent getComponent()
	{
		if(self==null)
			throw new IllegalStateException("Component can not be accessed in 'beforeCreation' bootstrapping.");
		return self;
	}
	
	@Override
	public void scheduleStep(Runnable r)
	{
		if(terminated)
			throw new ComponentTerminatedException(getComponent().getId());
		
		boolean	startnew	= false;
		synchronized(ExecutionFeature.this)
		{
			System.out.println("insert step: "+r);
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
			restart();
		}
	}
	
	@Override
	public <T> IFuture<T> scheduleStep(Callable<T> s)
	{
		Future<T> ret = new Future<>();
		
		if(terminated)
		{
			ret.setException(new ComponentTerminatedException(getComponent().getId()));
			return ret;
		}
		
		scheduleStep(new StepInfo(() ->
		{
			try
			{
				T res = s.call();
				
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
			catch(StepAborted t)
			{
				ret.setException(new RuntimeException("Error in step", t));
				// Pass abort error to thread runner main loop
				throw t;
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
		});
	}
	
	/**
	 *  Schedule a step that provides a result.
	 *  @param step	A step that is executed via the {@link Supplier#get()} method.
	 *  @return	A future that provides access to the step result, once it is available.
	 */
	public <T> IFuture<T> scheduleStep(IThrowingFunction<IComponent, T> step)
	{
		Future<T> ret = new Future<>();
		
		if(terminated)
		{
			ret.setException(new ComponentTerminatedException(getComponent().getId()));
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
			catch(StepAborted t)
			{
				ret.setException(new RuntimeException("Error in step", t));
				// Pass abort error to thread runner main loop
				throw t;
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
	public <T> IFuture<T> scheduleAsyncStep(Callable<IFuture<T>> step)
	{
		Future<T> ret = createStepFuture(step);
		
		if(terminated)
		{
			ret.setException(new ComponentTerminatedException(getComponent().getId()));
			return ret;
		}
		
		scheduleStep(new StepInfo(() ->
		{
			try
			{
				IFuture<T> res = step.call();
				
				if(!saveEndStep(res, (Future)ret))
				{
					@SuppressWarnings("unchecked")
					Future<T>	resfut	= (Future<T>)res;
					// Use generic connection method to avoid issues with different future types.
					resfut.delegateTo(ret);
				}
			}
			catch(StepAborted t)
			{
				ret.setException(new RuntimeException("Error in step", t));
				// Pass abort error to thread runner main loop
				throw t;
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
	 *  @param step	A step that is executed via the {@link IThrowingFunction#apply()} method.
	 *  @return	A future that provides access to the step result, once it is available.
	 */
	public <T> IFuture<T> scheduleAsyncStep(IThrowingFunction<IComponent, IFuture<T>> step)
	{
		Future<T> ret = createStepFuture(step);
		
		if(terminated)
		{
			ret.setException(new ComponentTerminatedException(getComponent().getId()));
			return ret;
		}
		
		scheduleStep(new StepInfo(() ->
		{
			try
			{
				IFuture<T> res = step.apply(self);
				
				if(!saveEndStep(res, (Future)ret))
				{
					@SuppressWarnings("unchecked")
					Future<T> resfut = (Future<T>)res;
					// Use generic connection method to avoid issues with different future types.
					resfut.delegateTo(ret);
				}
			}
			catch(StepAborted t)
			{
				ret.setException(new RuntimeException("Error in step", t));
				// Pass abort error to thread runner main loop
				throw t;
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
	 *  Create intermediate of direct future.
	 */
	protected <T> Future<T> createStepFuture(Callable<?> step)
	{
		Future<T> ret;
		try
		{
			if(step instanceof ICallable)
			{
				Class<?> clazz = ((ICallable)step).getFutureReturnType();
		
				if(IFuture.class.equals(clazz))
					ret = new Future<T>();
				else
					ret = (Future<T>)FutureFunctionality.getDelegationFuture(clazz, new FutureFunctionality());
			}
			else
			{
				ret = new Future<T>();
			}
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
		
		return ret;
	}
	
	/**
	 *  Create intermediate of direct future.
	 */
	protected <T> Future<T> createStepFuture(IThrowingFunction<IComponent, ?> step)
	{
		Future<T> ret;
		try
		{
			Class<?> clazz = step.getFutureReturnType();
		
			if(IFuture.class.equals(clazz))
				ret = new Future<T>();
			else
				ret = (Future<T>)FutureFunctionality.getDelegationFuture(clazz, new FutureFunctionality());
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
		
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
	private static volatile Timer timer = new Timer(true);
	//protected static volatile int	timer_entries;
	protected static volatile Set<TimerTaskInfo> entries;
	
	@Override
	public ITerminableFuture<Void> waitForDelay(long millis)
	{
		TerminableFuture<Void> ret = new TerminableFuture<>();
		
		if(terminated)
		{
			ret.setException(new ComponentTerminatedException(getComponent().getId()));
			return ret;
		}
		
		synchronized(ExecutionFeature.class)
		{
			//if(timer==null)
			//	timer = new Timer();
			//timer_entries++;
			
			TimerTaskInfo task = new TimerTaskInfo(getComponent().getId(), ret);
			task.setTask(new TimerTask()
			{
				@Override
				public void run()
				{
					//if(!this.cancel())
					//	return;
					
					scheduleStep((Runnable)() -> ret.setResultIfUndone(null));
					
					synchronized(ExecutionFeature.class)
					{
						if(entries==null)
							return;

						//timer_entries--;
						entries.remove(task);
						if(entries.size()==0)
						{
							entries	= null;
							//timer.cancel();
							//timer = null;
						}
					}
				}
			});
			
			if(entries==null)
				entries	= new LinkedHashSet<>(2, 1);
			
			entries.add(task);
			
			timer.schedule(task.getTask(), millis);
			
			ret.setTerminationCommand(ex -> 
			{
				synchronized(ExecutionFeature.class)
				{
					//if(!task.getTask().cancel())
					//	return;
					if(entries==null)
						return;
					
					entries.remove(task);
					if(entries.size()==0)
					{
						entries	= null;
						//timer.cancel();
						//timer = null;
					}
				}
			});
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
	protected Set<ComponentSuspendable>	threads;
	
	protected class ThreadRunner implements Runnable
	{
		@Override
		public void run()
		{
			ComponentSuspendable	sus	= new ComponentSuspendable();
			// synchronized because another thread could exit in parallel
			synchronized(ExecutionFeature.this)
			{
				if(threads==null)
					threads	= new LinkedHashSet<>(2, 1);
				threads.add(sus);
			}
			ISuspendable.SUSPENDABLE.set(sus);
			LOCAL.set(ExecutionFeature.this);
//			ScopedValue.where(ISuspendable.SUSPENDABLE, sus).run(()->
//			{
//				ScopedValue.where(LOCAL, MjExecutionFeature.this).run(()->
//				{
					boolean hasnext	= true;
					while(hasnext && !terminated)
					{
						Runnable	step;
						synchronized(ExecutionFeature.this)
						{
							step	= steps.poll();
						}
						
						assert step!=null;
						
//						// for debugging only
//						boolean aborted	= false;
						
						try
						{
//							if(step!=null)	// TODO: why can be null?
								doRun(step);
						}
						catch(StepAborted d)
						{
							// ignore aborted steps.
//							aborted	= true;
						}
						
						synchronized(ExecutionFeature.this)
						{
							if(do_switch)
							{
								do_switch	= false;
								hasnext	= false;
							}
							else if(steps.isEmpty() && !terminated)
							{
								// decrement only if not terminated, otherwise blocking lambda fails
								if(threadcount.decrementAndGet()<0)
								{
									throw new IllegalStateException("Threadcount<0");
								}
								
								hasnext	= false;
								executing	= false;
								idle();	// only call idle when not terminated
							}
						}
					}
					// synchronized because multiple threads could exit in parallel (e.g. after unblocking a future)
					synchronized(ExecutionFeature.this)
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
		
		/** Provide access to future when suspended. */
		protected Future<?> future;
		
		/** Use reentrant lock/condition instead of synchronized/wait/notify to avoid pinning when using virtual threads. */
		protected ReentrantLock lock	= new ReentrantLock();
		protected Condition	wait	= lock.newCondition();
		
		@Override
		public void suspend(Future<?> future, long timeout, boolean realtime)
		{
			assert !blocked;
			assert !aborted;
			
			beforeBlock(future);
			
			boolean startnew	= false;
			
			synchronized(ExecutionFeature.this)
			{
				if(threadcount.decrementAndGet()<0)
				{
					throw new IllegalStateException("Threadcount<0");
				}
				
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
	
			try
			{
				lock.lock();
				
				if(startnew)
				{
					restart();
				}
				
				try
				{
					this.future	= future;
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
					this.future	= null;
					
					if(aborted)
					{
						throw new StepAborted(getComponent().getId());
					}

					if(threadcount.incrementAndGet()>1)
					{
						throw new IllegalStateException("Threadcount>1");
					}
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
				scheduleStep((Runnable)() ->
				{
					try
					{
						lock.lock();
						do_switch	= true;
						if(threadcount.decrementAndGet()<0)
						{
							throw new IllegalStateException("Threadcount<0");
						}
						wait.signal();
						
						// Abort this step to skip afterStep() call, because other thread is already running now.
						throw new StepAborted(null);//getComponent().getId()); // Use null because code is used in bootstrapping before getComponent() is available
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
		public IFuture<?> getFuture()
		{
			return future;
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
		
		Collection<Object>	cfeatures	= self.getFeatures();
		Object[]	features	= cfeatures.toArray(new Object[cfeatures.size()]);
		for(int i=features.length-1; i>=0; i--)
		{
			if(features[i] instanceof ILifecycle) 
			{
				ILifecycle lfeature = (ILifecycle)features[i];
				lfeature.onEnd();
			}
		}
		
		terminated = true;
		
		//System.out.println("terminate start: "+getComponent().getId()+" "+steps.size());
		
		// Terminate blocked threads
		// Do first to unblock futures before setting results later
		// Use copy as threads remove themselves from set on exit. 
		ComponentSuspendable[]	mythreads	= null;
		synchronized(ExecutionFeature.this)
		{
			if(threads!=null)
				mythreads = threads.toArray(ComponentSuspendable[]::new);
		}
		if(mythreads!=null)
		{
			for(ComponentSuspendable thread: mythreads)
				thread.abort();
		}
		
		// Drop queued steps.
		ComponentTerminatedException ex = new ComponentTerminatedException(getComponent().getId());
		synchronized(ExecutionFeature.this) 
		{
			for(Object step: steps)
			{
				if(step instanceof StepInfo)
				{
					((StepInfo)step).getFuture().setExceptionIfUndone(ex);
				}
			}
			steps.clear();
		}
		
		// Drop queued timer tasks.
		List<TimerTaskInfo> todo = new ArrayList<>();
		TimerTaskInfo[] ttis;
		synchronized(ExecutionFeature.class)
		{
			ttis = entries==null? new TimerTaskInfo[0]: entries.toArray(TimerTaskInfo[]::new);
			
			for(TimerTaskInfo tti: ttis)
			{
				if(getComponent().getId().equals(tti.getComponentId()))
				{
					todo.add(tti);
					tti.getTask().cancel();
					entries.remove(tti);
				}
			}
		}
		for(TimerTaskInfo tti: todo)
		{
			tti.getFuture().setExceptionIfUndone(ex);
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
	
	protected <T> void beforeBlock(Future<T> fut)
	{
		if(listeners!=null)
		{
			for(IStepListener lis : listeners)
			{
				lis.beforeBlock(fut);
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
		catch(StepAborted t)
		{
			// Pass abort error to thread runner main loop
			throw t;
		}
		catch(Exception e)
		{
			self.handleException(e);
		}
		catch(Throwable t)
		{
			// Print and otherwise ignore any other exceptions
			RuntimeException ex = new RuntimeException("Exception in step", t);//.printStackTrace();
			ex.printStackTrace();
			self.handleException(ex);
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
//			doRun(step);
			step.run();
		}
	}
	
	/**
	 *  (Re-)Start the execution thread.
	 */
	protected void restart()
	{
		if(runner==null)
			runner	= new ThreadRunner();
		if(threadcount.incrementAndGet()>1)
		{
			throw new IllegalStateException("Threadcount>1");
		}
		SUtil.getExecutor().execute(runner);
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
				//System.out.println("endstep: "+getComponent().getId()+" "+this.hashCode());
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
