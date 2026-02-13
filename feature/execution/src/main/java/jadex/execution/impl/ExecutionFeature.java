package jadex.execution.impl;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.common.TimeoutException;
import jadex.core.ComponentTerminatedException;
import jadex.core.ICallable;
import jadex.core.IComponent;
import jadex.core.INoCopyStep;
import jadex.core.IThrowingConsumer;
import jadex.core.IThrowingFunction;
import jadex.core.impl.Component;
import jadex.core.impl.ILifecycle;
import jadex.core.impl.StepAborted;
import jadex.execution.IExecutionFeature;
import jadex.execution.future.ComponentFutureFunctionality;
import jadex.execution.future.FutureFunctionality;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ISuspendable;
import jadex.future.ITerminableFuture;
import jadex.future.TerminableFuture;

public class ExecutionFeature	implements IExecutionFeature, IInternalExecutionFeature, ILifecycle
{
	/** Provide access to the execution feature when running inside a component. */
	public static final ThreadLocal<ExecutionFeature>	LOCAL	= new ThreadLocal<>();
	
	private Queue<Runnable> steps;
	protected volatile boolean executing;
	protected volatile int	do_switch;
	protected Component	self = null;
	
	/**
	 *  Create a new execution feature.
	 *  @param self The component that is executing.
	 */
	public ExecutionFeature(Component self)
	{
		this.self = self;
	}
	
	@Override
	public IComponent getComponent()
	{
		return self;
	}
	
	@Override
	public void scheduleStep(Runnable r)
	{
		boolean	startnew	= false;
		boolean	setex	= false;
		synchronized(ExecutionFeature.this)
		{
			if(self.isTerminated())
			{
				if(r instanceof StepInfo)
				{
					setex	= true;
				}
				else
				{
					throw new ComponentTerminatedException(getComponent().getId());
				}
			}
			else
			{
				//System.out.println("insert step: "+r);
				if(steps==null)
				{
					steps	= new ArrayDeque<>(2);
				}
				steps.offer(r);
				if(!executing)
				{
					startnew	= true;
					executing	= true;
					busy();
				}
			}
		}
		
		if(setex)
		{
			((StepInfo)r).getFuture().setExceptionIfUndone(new ComponentTerminatedException(getComponent().getId()));
		}
		
		if(startnew)
		{
			restart();
		}
	}
	
	@Override
	public <T> IFuture<T> scheduleStep(Callable<T> step)
	{
		Future<T> ret = createStepFuture(step, true);
		Exception stack	= SUtil.DEBUG ? new RuntimeException() : null;
		
		scheduleStep(new StepInfo(() ->
		{
			try
			{
				T res = step.call();
				if(res instanceof IFuture)
				{
					throw new UnsupportedOperationException("Use scheduleAsyncStep for steps returning futures: "+step,
						stack!=null ? stack : new RuntimeException("Set SUtil.DEBUG to true for caller stack trace."));
				}
				ret.setResult(res);
			}
			catch(Throwable t)
			{
				handleStepException(t, ret);
			}
		}, ret));
		return ret;
	}
	
	/**
	 *  Schedule a step that provides a result.
	 *  @param step	A step that is executed via the {@link Supplier#get()} method.
	 *  @return	A future that provides access to the step result, once it is available.
	 */
	@Override
	public void scheduleStep(IThrowingConsumer<IComponent> step)
	{
		scheduleStep(() ->
		{
			try
			{
				step.accept(self);
			}
			catch(Throwable t)
			{
				handleStepException(t, null);
			}
		});
	}
	
	/**
	 *  Schedule a step that provides a result.
	 *  @param step	A step that is executed via the {@link Supplier#get()} method.
	 *  @return	A future that provides access to the step result, once it is available.
	 */
	@Override
	public <T> IFuture<T> scheduleStep(IThrowingFunction<IComponent, T> step)
	{
		Future<T> ret = createStepFuture(step, true);
		Exception stack	= SUtil.DEBUG ? new RuntimeException() : null;
		
		scheduleStep(new StepInfo(() ->
		{
			try
			{
				T res = step.apply(self);
				if(res instanceof IFuture)
				{
					throw new UnsupportedOperationException("Use scheduleAsyncStep for steps returning futures: "+step,
						stack!=null ? stack : new RuntimeException("Set SUtil.DEBUG to true for caller stack trace."));
				}
				ret.setResult(res);
			}
			catch(Throwable t)
			{
				handleStepException(t, ret);
			}
		}, ret));
		return ret;
	}
	
	/**
	 *  Schedule a step that provides a result.
	 *  @param step	A step that is executed via the {@link Supplier#get()} method.
	 *  @return	A future that provides access to the step result, once it is available.
	 */
	@Override
	public <E, T extends IFuture<E>> T scheduleAsyncStep(Callable<T> step)
	{
		@SuppressWarnings("unchecked")
		T	ret = (T) createStepFuture(step, false);
		
		scheduleStep(new StepInfo(() ->
		{
			try
			{
				T res = step.call();
				@SuppressWarnings("unchecked")
				Future<E>	retfut	= (Future<E>)ret;
				// Use generic connection method to avoid issues with different future types.
				res.delegateTo(retfut);
			}
			catch(Throwable t)
			{
				handleStepException(t, (Future<?>) ret);
			}
		}, (Future<?>) ret));
		return ret;
	}
	
	/**
	 *  Schedule a step that provides a result.
	 *  @param step	A step that is executed via the {@link IThrowingFunction#apply()} method.
	 *  @return	A future that provides access to the step result, once it is available.
	 */
	@Override
	public <E, T extends IFuture<E>> T scheduleAsyncStep(IThrowingFunction<IComponent, T> step)
	{
		@SuppressWarnings("unchecked")
		T	ret = (T) createStepFuture(step, false);
		
		scheduleStep(new StepInfo(() ->
		{
			try
			{
				T res = step.apply(self);
				@SuppressWarnings("unchecked")
				Future<E>	retfut	= (Future<E>)ret;
				// Use generic connection method to avoid issues with different future types.
				res.delegateTo(retfut);
			}
			catch(Throwable t)
			{
				handleStepException(t, (Future<?>) ret);
			}
		}, (Future<?>) ret));
		
		return ret;
	}
	
	/**
	 *  Create future for step result depending on step type and potential annotations. 
	 */
	protected <T> Future<T> createStepFuture(Object step, boolean sync)
	{
		if(sync && LOCAL.get()==this)
		{
			return new Future<>();
		}
		else
		{
			AnnotatedType	type	= null;
			if(step instanceof ICallable)
			{
				type	= ((ICallable<?>) step).getReturnType();
			}
			else if(step instanceof IThrowingFunction)
			{
				type	= ((IThrowingFunction<?, ?>) step).getReturnType();
			}
			
			// Default implementation of throwing function gives null type
			if(type==null)
			{
				type = getReturnType(step.getClass());
			}
			
			@SuppressWarnings("unchecked")
			Future<T>	ret	= (Future<T>)FutureFunctionality.getDelegationFuture(type==null ? Object.class : SReflect.getRawClass(type.getType()),
				LOCAL.get()==this ?	new FutureFunctionality()
					: new ComponentFutureFunctionality(this, type==null || !Component.isNoCopy(type.getAnnotations())));
			return ret;
		}
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
	private static Timer timer = new Timer(true);

	/** Timer entries to be cancelled on termination. */
	protected Set<WaitTask> entries;
	
	@Override
	public ITerminableFuture<Void> waitForDelay(long millis)
	{
		TerminableFuture<Void> ret = new TerminableFuture<>();
		
		if(self.isTerminated())
		{
			ret.setException(new ComponentTerminatedException(getComponent().getId()));
			return ret;
		}
		
		WaitTask task = new WaitTask(this, ret);
		timer.schedule(task, millis);

		if(entries==null)
			entries	= new LinkedHashSet<>(2);
		
		entries.add(task);
		
		ret.setTerminationCommand(ex -> 
		{
			try
			{
				scheduleStep(() ->
				{
					entries.remove(task);
					task.cancel();
				});
			}
			catch(ComponentTerminatedException cte)
			{
				// Ignore -> already cancelled in terminate()
			}
		});
		
		return  ret;
	}
	
	/**
	 *  Task to notify future after delay.
	 */
	static class WaitTask	extends TimerTask
	{
		protected ExecutionFeature	exe;
		protected Future<?> future;
		
		WaitTask(ExecutionFeature exe, Future<?> future)
		{
			this.exe = exe;
			this.future = future;
		}

		Future<?> getFuture() 
		{
			return future;
		}
		
		@Override
		public void run()
		{
			try
			{
				// Check for null -> cancel() might run in parallel
				ExecutionFeature	exe	= this.exe;
				if(exe!=null)
				{
					exe.scheduleStep(() ->
					{
						exe.entries.remove(WaitTask.this);
						// Check for null -> cancel() might run interleaved
						Future<?>	future	= this.future;
						if(future!=null)
						{
							future.setResultIfUndone(null);
						}
					});
				}
			}
			catch(ComponentTerminatedException e)
			{
				// Check for null -> cancel() might run in parallel
				Future<?>	future	= this.future;
				if(future!=null)
				{
					future.setExceptionIfUndone(e);
				}
			}
		}
		
		@Override
		public boolean cancel()
		{
			// Remove references to avoid memory leak.
			this.exe	= null;
			this.future	= null;
			return super.cancel();
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
					while(hasnext && !self.isTerminated())
					{
						Runnable	step;
						synchronized(ExecutionFeature.this)
						{
//							int presize = steps.size();
							step = steps.poll();
//							if(step==null)
//								System.out.println("step is null: "+steps.size()+" "+presize+" "+terminated);							
						}
						
						assert step!=null;
						try
						{
							doRun(step);
						}
						catch(StepAborted d)
						{
							// NOP -> continue main loop after abortion of a step.
						}
						
						synchronized(ExecutionFeature.this)
						{
							if(steps!=null && steps.isEmpty())
							{
								steps	= null;	// free memory
							}
							
							// Stop this thread, because another thread is still executing
							if(do_switch>0)
							{
								do_switch--;
								hasnext	= false;
							}
							
							// Stop this thread, because there are no more steps -> set executing=false
							else if(!self.isTerminated() && steps==null)
							{
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
		
		/** Set by timer to indicate timeout. */  
		protected boolean	istimeout;
		
		/** Provide access to future when suspended. */
		protected Future<?> future;
		
		/** The timer, when blocked and timeout is set. */
		protected ITerminableFuture<Void>	timer;
		
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
			boolean istimeout	= false;
			
			synchronized(ExecutionFeature.this)
			{
				if(!(steps==null || steps.isEmpty()) && !aborted)
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
					
					if(timeout==Future.UNSET)
					{
						timeout = SUtil.DEFTIMEOUT;
					}
					if(timeout>0)
					{
						this.timer	= waitForDelay(timeout);
						timer.then(v -> 
						{
							boolean	resume	= false;
							try
							{
								lock.lock();
								// Only wake up if still waiting for same future (no timeout, when result already occurred).
								if(future==this.future)
								{
									this.istimeout	= true;
									resume	= true;
								}
							}
							finally
							{
								lock.unlock();
							}
							
							if(resume)
							{
								resume(future);
							}
						});
					}
					
					wait.await();
					istimeout	= this.istimeout;
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
				}
			}
			finally
			{
				lock.unlock();
			}
			
			afterBlock();
			
			if(istimeout)
			{
				throw new TimeoutException();
			}
		}
	
		@Override
		public void resume(Future<?> future)
		{
			if(!aborted)
			{
				try
				{
					scheduleStep((Runnable)() ->
					{
						try
						{
							lock.lock();
							// Only wake up if still waiting for same future (invalid resume might be called from outdated future after timeout already occurred).
							if(future==this.future)
							{
								synchronized(ExecutionFeature.this)
								{
									// Force this thread (or another) to end execution
									// Can be two resume() of different threads before any threads end,
									// thus a counter is needed.
									do_switch++;
								}
								wait.signal();
								
								// Abort this step to skip afterStep() call, because other thread is already running now.
								// Use null because code is used in bootstrapping before getComponent() is available
								throw new StepAborted(null);
							}
						}
						finally
						{
							lock.unlock();
						}
					});
				}
				catch(ComponentTerminatedException cte)
				{
					// ignore outdated resume -> future will be aborted on terminate
				}
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
			listeners	= new ArrayList<>(1);
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
	public void init()
	{
	}
	
	@Override
	public void cleanup()
	{
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
		ComponentTerminatedException ex = null;
		synchronized(ExecutionFeature.this) 
		{
			if(steps!=null)
			{
				for(Object step: steps)
				{
					if(step instanceof StepInfo)
					{
						if(ex==null)
							ex	= new ComponentTerminatedException(getComponent().getId());
						((StepInfo)step).getFuture().setExceptionIfUndone(ex);
					}
				}
				steps	= null;
			}
		}
		
		// Drop queued timer tasks.
		if(entries!=null)
		{
			for(WaitTask tti: entries)
			{
				if(ex==null)
					ex	= new ComponentTerminatedException(getComponent().getId());
				tti.getFuture().setExceptionIfUndone(ex);
				tti.cancel();
			}
			entries	= null;
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
			if(step==null)
				System.out.println("nullstep");
			step.run();
		}
		catch(Throwable t)
		{
			handleStepException(t, null);
		}
		
		afterStep();
	}
	
	/**
	 *  Handle exception in user code.
	 *  Set exception in result future if possible and otherwise handle on component level.
	 *  
	 *  @param t	The throwable.
	 *  @param ret	The result future of the step, if any.
	 */
	protected void handleStepException(Throwable t, Future<?> ret)
	{
		if(t instanceof StepAborted)
		{
			if(ret!=null)
			{
				// Undone, because, exception might occur in listener after future is set
				ret.setExceptionIfUndone(new RuntimeException("Error in step", t));
			}
			
			// Pass abort error to thread runner main loop
			throw (StepAborted)t;
		}
		else
		{
			boolean handled	= false;
			Exception ex	= t instanceof Exception ? (Exception) t : new RuntimeException("Error in step", t);
			
			if(ret!=null)
			{
				// Undone, because, exception might occur in listener after future is set
				handled	= ret.setExceptionIfUndone(ex);
			}

			if(!handled)
			{
				// TODO: why can be null?
				if(self!=null)
				{
					// Handler might be user code and thus might throw exceptions itself.
					try
					{
						self.handleException(ex);
					}
					catch(Exception e2)
					{
						System.out.println("Exception in user code of component; component will be terminated: "+self.getId());
						e2.printStackTrace();
						
						// user terminate throws StepAborted so afterStep() is not called.
						self.terminate();
					}
				}
				else
				{
					throw SUtil.throwUnchecked(t);
				}
			}
		}		
	}
	
	static class StepInfo implements Runnable
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
			step.run();
		}
	}
	
	/**
	 *  (Re-)Start the execution thread.
	 */
	protected void restart()
	{
		SUtil.getExecutor().execute(new ThreadRunner());
	}

	protected static Map<Class<?>, AnnotatedType>	RETURNTYPE	= new LinkedHashMap<>();
	
	/**
	 *  Get return type from pojo method.
	 */
	protected static AnnotatedType	getReturnType(Class<?> pojoclazz)
	{
		synchronized (RETURNTYPE)
		{
			if(!RETURNTYPE.containsKey(pojoclazz))
			{
				Method	m	= null;
				
				if(SReflect.isSupertype(INoCopyStep.class, pojoclazz))
				{
					try
					{
						m	= INoCopyStep.class.getMethod("apply", IComponent.class);
					}
					catch(Exception e)
					{
						// Should not happen
						SUtil.throwUnchecked(e);
					}
				}
				else if(SReflect.isSupertype(IThrowingFunction.class, pojoclazz))
				{
					// Can be also explicitly declared with component type or just implicit (lambda) as object type
					try
					{
						m	= pojoclazz.getMethod("apply", IComponent.class);
					}
					catch(Exception e)
					{
						try
						{
							m	= pojoclazz.getMethod("apply", Object.class);
						}
						catch(Exception e2)
						{
						}
					}
				}
				else	// Callable
				{
					try
					{
						m	= pojoclazz.getMethod("call");
					}
					catch(Exception e)
					{
					}
				}
				
				RETURNTYPE.put(pojoclazz, m!=null ? m.getAnnotatedReturnType() : null);
			}
			return RETURNTYPE.get(pojoclazz);
		}
	}
}
