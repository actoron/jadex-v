package jadex.execution.future;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.logging.Logger;

import jadex.common.ICommand;
import jadex.common.IResultCommand;
import jadex.common.SReflect;
import jadex.future.DelegationResultListener;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.IIntermediateFuture;
import jadex.future.IPullIntermediateFuture;
import jadex.future.IPullSubscriptionIntermediateFuture;
import jadex.future.IResultListener;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.ITerminableFuture;
import jadex.future.ITerminableIntermediateFuture;
import jadex.future.IntermediateDelegationResultListener;
import jadex.future.IntermediateFuture;
import jadex.future.PullIntermediateDelegationFuture;
import jadex.future.PullSubscriptionIntermediateDelegationFuture;
import jadex.future.SubscriptionIntermediateDelegationFuture;
import jadex.future.TerminableDelegationFuture;
import jadex.future.TerminableIntermediateDelegationFuture;

/**
 *  Default future functionality.
 */
public class FutureFunctionality
{
	//-------- constants --------
	
	/** Marker for an intermediate result to be dropped. */
	public static final String	DROP_INTERMEDIATE_RESULT	= "__drop_intermediate_result__";
	
	//-------- attributes --------
	
	/** The logger used for notification failure warnings (if any). */
	protected Logger	logger;
	protected IResultCommand<Logger, Void> loggerfetcher;
	protected boolean undone;
	
	protected Future<?> future;
	
	/**
	 * 
	 */
	public FutureFunctionality()
	{
	}
	
	/**
	 * 
	 */
	public FutureFunctionality(Logger logger)
	{
		this.logger	= logger;
	}
	
	/**
	 * 
	 */
	public FutureFunctionality(IResultCommand<Logger, Void> loggerfetcher)
	{
		this.loggerfetcher = loggerfetcher;
	}
	
	
	
	public Future<?> getFuture() 
	{
		return future;
	}

	public void setFuture(Future<?> future) 
	{
		this.future = future;
	}

	/**
	 *  Get the logger.
	 */
	protected Logger	getLogger()
	{
		if(logger==null)
		{
			if(loggerfetcher!=null)
			{
				logger = loggerfetcher.execute(null);
			}
			else
			{
				Logger.getAnonymousLogger();
			}
		}
		return logger;
//		return logger!=null ? logger : Logger.getAnonymousLogger();
	}
	
	/**
	 *  Log an exception.
	 */
	protected void	logException(Exception e, Exception userex, boolean terminable, boolean undone, boolean async)
	{
//		if(userex!=null)
//		{
//			StringWriter	sw	= new StringWriter();
//			userex.printStackTrace(new PrintWriter(sw));
//			getLogger().severe("Exception in future functionality: "+sw);
//		}
//		e.printStackTrace(new PrintWriter(sw));
//		Thread.dumpStack();
//		if(!undone && ! async)
//		{
//			throw SUtil.throwUnchecked(e);
//		}
	}
	
	//--------  control flow handling --------
	
	/**
	 *  Schedule forward in result direction,
	 *  i.e. from callee to caller,
	 *  e.g. update timer to avoid timeouts.
	 */
	public <T> void	scheduleForward(ICommand<T> code, T arg)
	{
		code.execute(arg);
	}
	
	/**
	 *  Schedule backward in result direction,
	 *  i.e. from caller to callee,
	 *  e.g. future termination.
	 */
	public void	scheduleBackward(ICommand<Void> code)
	{
		code.execute(null);
	}
	
	//-------- data handling --------
	
	/**
	 *  Optionally alter the undone flag.
	 */
	public boolean	isUndone(boolean undone)
	{
		return undone;
	}
	
	/**
	 *  Optionally alter a result.
	 */
	public Object handleResult(Object result) throws Exception
	{
		return result;
	}
	
	/**
	 *  Optionally alter a result.
	 */
	public Object handleIntermediateResult(Object result) throws Exception
	{
		return result;
	}
	
	/**
	 *  Perform code after an intermediate result has been added.
	 */
	public void handleAfterIntermediateResult(Object result) throws Exception
	{
	}
	
	/**
	 *  Optionally alter finished behavior.
	 */
	public void	handleFinished(Collection<Object> results)	throws Exception
	{
	}
	
	/**
	 *  Optionally augment exception behavior.
	 */
	public void	handleException(Exception exception)
	{
	}
	
	/**
	 *  Optionally augment termination behavior.
	 */
	public void	handleTerminated(Exception reason)
	{
	}
	
	/**
	 *  Optionally augment backward command behavior.
	 */
	public void	handleBackwardCommand(Object info)
	{
	}
	
	/**
	 *  Optionally augment pull behavior.
	 */
	public void	handlePull()
	{
	}
	
	/**
	 * 
	 */
	public static <T> Future<T> getDelegationFuture(IFuture<T> orig, final FutureFunctionality func)
	{
		Future ret = null;
		
		if(orig instanceof IPullSubscriptionIntermediateFuture)
		{
			PullSubscriptionIntermediateDelegationFuture<Object> fut = new DelegatingPullSubscriptionIntermediateDelegationFuture((IPullSubscriptionIntermediateFuture)orig, func);
			// automatically done in future constructor
//			((Future<Collection<Object>>)orig).addResultListener(new TerminableIntermediateDelegationResultListener<Object>(fut, (ITerminableIntermediateFuture)orig));
			ret	= fut;
		}
		else if(orig instanceof IPullIntermediateFuture)
		{
			PullIntermediateDelegationFuture<Object> fut = new DelegatingPullIntermediateDelegationFuture((IPullIntermediateFuture)orig, func);
			// automatically done in future constructor
//			((Future<Collection<Object>>)orig).addResultListener(new TerminableIntermediateDelegationResultListener<Object>(fut, (ITerminableIntermediateFuture)orig));
			ret	= fut;
		}
		else if(orig instanceof ISubscriptionIntermediateFuture)
		{
			SubscriptionIntermediateDelegationFuture<Object> fut = new DelegatingSubscriptionIntermediateDelegationFuture((ISubscriptionIntermediateFuture)orig, func);
			// automatically done in future constructor
//			((Future<Collection<Object>>)orig).addResultListener(new TerminableIntermediateDelegationResultListener<Object>(fut, (ITerminableIntermediateFuture)orig));
			ret	= fut;
		}
		else if(orig instanceof ITerminableIntermediateFuture)
		{
			TerminableIntermediateDelegationFuture<Object> fut = new DelegatingTerminableIntermediateDelegationFuture((ITerminableIntermediateFuture)orig, func);
			// automatically done in future constructor
//			((Future<Collection<Object>>)orig).addResultListener(new TerminableIntermediateDelegationResultListener<Object>(fut, (ITerminableIntermediateFuture)orig));
			ret	= fut;
		}
		else if(orig instanceof ITerminableFuture)
		{
			TerminableDelegationFuture<Object> fut = new DelegatingTerminableDelegationFuture((ITerminableFuture)orig, func);
			// automatically done in future constructor
//			((Future<Object>)orig).addResultListener(new TerminableDelegationResultListener<Object>(fut, (ITerminableFuture)orig));
			ret	= fut;
		}
		else if(orig instanceof IIntermediateFuture)
		{
			IntermediateFuture<Object>	fut	= new DelegatingIntermediateFuture(func);
			((IntermediateFuture<Object>)orig).addResultListener(new IntermediateDelegationResultListener<Object>(fut));
			ret	= fut;
		}
		else
		{
			Future<Object>	fut	= new DelegatingFuture(func);
			((Future<Object>)orig).addResultListener(new DelegationResultListener<Object>(fut));
			ret	= fut;
		}
		
		return ret;
	}
	
	/**
	 * 
	 */
	public static Future<?> getDelegationFuture(Class<?> clazz, final FutureFunctionality func)
	{
		Future<?> ret = null;
		
		if(IPullSubscriptionIntermediateFuture.class.isAssignableFrom(clazz))
		{
			ret = new DelegatingPullSubscriptionIntermediateDelegationFuture(func);
		}
		else if(IPullIntermediateFuture.class.isAssignableFrom(clazz))
		{
			ret = new DelegatingPullIntermediateDelegationFuture(func);
		}
		else if(ISubscriptionIntermediateFuture.class.isAssignableFrom(clazz))
		{
			ret = new DelegatingSubscriptionIntermediateDelegationFuture(func);
		}
		else if(ITerminableIntermediateFuture.class.isAssignableFrom(clazz))
		{
			ret = new DelegatingTerminableIntermediateDelegationFuture(func);
		}
		else if(ITerminableFuture.class.isAssignableFrom(clazz))
		{
			ret = new DelegatingTerminableDelegationFuture(func);
		}
		else if(IIntermediateFuture.class.isAssignableFrom(clazz))
		{
			ret	= new DelegatingIntermediateFuture(func);
		}
		else
		{
			ret	= new DelegatingFuture(func);
		}
		
		return ret;
	}
	
//	public static Future<Object> createReturnFuture(Method method, Object[] args, ClassLoader cl, FutureFunctionality func)
//	{
//		return createReturnFuture(method, args!=null? SUtil.arrayToList(args): null, cl, func);
//	}
	
	public static Future<Object> createReturnFuture(Method method, /*List<Object> args, ClassLoader cl,*/ FutureFunctionality func)
	{
		Future<Object> ret = null;
		
		if(SReflect.isSupertype(IFuture.class, method.getReturnType()))
		{
			Class<?> rettype = null;
//			Annotation[][] anss = method.getParameterAnnotations();
//			for(int i=0; i<anss.length; i++)
//			{
//				Annotation[] ans = anss[i];
//				for(Annotation an: ans)
//				{
//					if(an instanceof FutureReturnType)
//					{
//						Object t = args.get(i);
//						if(t instanceof Class)
//							rettype = (Class<?>)t;
//						else if(t instanceof ClassInfo)
//							rettype = ((ClassInfo)t).getType(cl);
//						if(rettype!=null)
//							break;
//					}
//				}
//			}
			/*if("invokeMethod".equals(method.getName()))
			{
				ClassInfo rtype = (ClassInfo)myargs.get(3);
				if(rtype!=null)
					rettype = rtype.getType(comp.getClassLoader());
			}*/
			
//			if(rettype==null)
				rettype = method.getReturnType();
			
			ret = (Future<Object>)FutureFunctionality.getDelegationFuture(rettype, 
				func==null? new FutureFunctionality(): func);
		}
		
		return ret;
	}
	
	/**
	 *  Handle a to be added result listener.
	 *  @return the listener to be actually added, or null, if the listener should not be added.
	 */
	public IResultListener<Object> handleAddResultListener(IResultListener<Object> listener)
	{
		return listener;
	}
}


/**
 * 
 */
class DelegatingPullSubscriptionIntermediateDelegationFuture<E> extends PullSubscriptionIntermediateDelegationFuture<E>
{
	/** The future functionality. */
	protected FutureFunctionality func;
	
	/**
	 * 
	 */
	public DelegatingPullSubscriptionIntermediateDelegationFuture(FutureFunctionality func)
	{
		if(func==null)
			throw new IllegalArgumentException("Func must not null.");
		this.func = func;
		func.setFuture(this);
	}
	
	/**
	 * 
	 */
	public DelegatingPullSubscriptionIntermediateDelegationFuture(IPullSubscriptionIntermediateFuture<E> src, FutureFunctionality func)
	{
		if(func==null)
			throw new IllegalArgumentException("Func must not null.");
		this.func = func;
		func.setFuture(this);
		src.delegateTo(this);
	}
	
	/**
	 *  Overwritten to change result or undone, if necessary.
	 */
	@Override
	protected boolean	doSetResult(Collection<E> result, boolean undone)
	{
		try
		{
			result = (Collection<E>)func.handleResult(result);
			return DelegatingPullSubscriptionIntermediateDelegationFuture.super.doSetResult(result, func.isUndone(undone));
		}
		catch(Exception e)
		{
			return doSetException(e, func.isUndone(undone));
		}		
	}
	
	/**
	 *  Overwritten to change undone, if necessary.
	 */
	@Override
	protected boolean	doSetException(Exception exception, boolean undone)
	{
		func.handleException(exception);
		return DelegatingPullSubscriptionIntermediateDelegationFuture.super.doSetException(exception, func.isUndone(undone));
	}
	
	/**
	 *  Overwritten to change result, if necessary.
	 */
	@Override
	protected boolean	doAddIntermediateResult(E result, boolean undone)
	{
		try
		{
			result = (E)func.handleIntermediateResult(result);
			boolean ret = FutureFunctionality.DROP_INTERMEDIATE_RESULT.equals(result) ? false
				: DelegatingPullSubscriptionIntermediateDelegationFuture.super.doAddIntermediateResult(result, func.isUndone(undone));
			func.handleAfterIntermediateResult(result);
			return ret;
		}
		catch(Exception e)
		{
			return doSetException(e, func.isUndone(undone));
		}		
	}

	/**
	 *  Overwritten to change result, if necessary.
	 */
	@Override
	protected synchronized boolean doSetFinished(boolean undone)
	{
		try
		{
			func.handleFinished((Collection<Object>)getIntermediateResults());
			return DelegatingPullSubscriptionIntermediateDelegationFuture.super.doSetFinished(func.isUndone(undone));
		}
		catch(Exception e)
		{
			return doSetException(e, func.isUndone(undone));
		}		
	}

	/**
     *  Execute a notification. Override for scheduling on other threads.
     */
	@Override
    protected void	executeNotification(IResultListener<Collection<E>> listener, ICommand<IResultListener<Collection<E>>> command)
    {
		func.scheduleForward(command, listener);
    }
	
	/**
	 *  Pull an intermediate result.
	 */
	@Override
	public void pullIntermediateResult()
	{
		func.scheduleBackward(new ICommand<Void>()
		{
			@Override
			public void execute(Void args)
			{
				func.handlePull();
				DelegatingPullSubscriptionIntermediateDelegationFuture.super.pullIntermediateResult();
			}
		});
	}
	
	/**
	 *  Terminate the future.
	 */
	@Override
	public void terminate(final Exception reason)
	{
		func.scheduleBackward(new ICommand<Void>()
		{
			@Override
			public void execute(Void args)
			{
				func.handleTerminated(reason);
				DelegatingPullSubscriptionIntermediateDelegationFuture.super.terminate(reason);
			}	
		});
	}
	
	/**
	 *  Send a backward command.
	 */
	@Override
	public void sendBackwardCommand(final Object info)
	{
		func.scheduleBackward(new ICommand<Void>()
		{
			@Override
			public void execute(Void args)
			{
				func.handleBackwardCommand(info);
				DelegatingPullSubscriptionIntermediateDelegationFuture.super.sendBackwardCommand(info);
			}	
		});
	}
	
	@Override
	public void	addResultListener(IResultListener listener)
	{
		listener	= func.handleAddResultListener(listener);
		if(listener!=null)
		{
			super.addResultListener(listener);
		}
	}
};

/**
 * 
 */
class DelegatingPullIntermediateDelegationFuture<E> extends PullIntermediateDelegationFuture<E>
{
	/** The future functionality. */
	protected FutureFunctionality func;
	
	/**
	 * 
	 */
	public DelegatingPullIntermediateDelegationFuture(FutureFunctionality func)
	{
		if(func==null)
			throw new IllegalArgumentException("Func must not null.");
		this.func = func;
		func.setFuture(this);
	}
	
	/**
	 * 
	 */
	public DelegatingPullIntermediateDelegationFuture(IPullIntermediateFuture<E> src, FutureFunctionality func)
	{
		if(func==null)
			throw new IllegalArgumentException("Func must not null.");
		this.func = func;
		func.setFuture(this);
		src.delegateTo(this);
	}

	/**
	 *  Overwritten to change result, if necessary.
	 */
	@Override
	protected boolean doSetResult(Collection<E> result, boolean undone)
	{
		try
		{
			result = (Collection<E>)func.handleResult(result);
			return DelegatingPullIntermediateDelegationFuture.super.doSetResult(result, func.isUndone(undone));
		}
		catch(Exception e)
		{
			return doSetException(e, func.isUndone(undone));
		}		
	}
	
	/**
	 *  Overwritten to change undone, if necessary.
	 */
	@Override
	protected boolean	doSetException(Exception exception, boolean undone)
	{
		func.handleException(exception);
		return DelegatingPullIntermediateDelegationFuture.super.doSetException(exception, func.isUndone(undone));
	}
	
	/**
	 *  Overwritten to change result, if necessary.
	 */
	@Override
	protected boolean doAddIntermediateResult(E result, boolean undone)
	{
		try
		{
			result = (E)func.handleIntermediateResult(result);
			boolean ret = FutureFunctionality.DROP_INTERMEDIATE_RESULT.equals(result) ? false
				: DelegatingPullIntermediateDelegationFuture.super.doAddIntermediateResult(result, func.isUndone(undone));
			func.handleAfterIntermediateResult(result);
			return ret;
		}
		catch(Exception e)
		{
			return doSetException(e, func.isUndone(undone));
		}		
	}

	/**
	 *  Overwritten to change result, if necessary.
	 */
	@Override
	protected synchronized boolean doSetFinished(boolean undone)
	{
		try
		{
			func.handleFinished((Collection<Object>)getIntermediateResults());
			return DelegatingPullIntermediateDelegationFuture.super.doSetFinished(func.isUndone(undone));
		}
		catch(Exception e)
		{
			return doSetException(e, func.isUndone(undone));
		}		
	}

	/**
     *  Execute a notification. Override for scheduling on other threads.
     */
	@Override
    protected void	executeNotification(IResultListener<Collection<E>> listener, ICommand<IResultListener<Collection<E>>> command)
    {
		func.scheduleForward(command, listener);
    }
	
	
	/**
	 *  Pull an intermediate result.
	 */
	@Override
	public void pullIntermediateResult()
	{
		func.scheduleBackward(new ICommand<Void>()
		{
			@Override
			public void execute(Void args)
			{
				func.handlePull();
				DelegatingPullIntermediateDelegationFuture.super.pullIntermediateResult();
			}
		});
	}
	
	/**
	 *  Terminate the future.
	 */
	@Override
	public void terminate(final Exception reason)
	{
		func.scheduleBackward(new ICommand<Void>()
		{
			@Override
			public void execute(Void args)
			{
				func.handleTerminated(reason);
				DelegatingPullIntermediateDelegationFuture.super.terminate(reason);
			}	
		});
	}
	
	/**
	 *  Send a backward command.
	 */
	@Override
	public void sendBackwardCommand(final Object info)
	{
		func.scheduleBackward(new ICommand<Void>()
		{
			@Override
			public void execute(Void args)
			{
				func.handleBackwardCommand(info);
				DelegatingPullIntermediateDelegationFuture.super.sendBackwardCommand(info);
			}	
		});
	}
	
	@Override
	public void	addResultListener(IResultListener listener)
	{
		listener	= func.handleAddResultListener(listener);
		if(listener!=null)
		{
			super.addResultListener(listener);
		}
	}
};

/**
 * 
 */
class DelegatingSubscriptionIntermediateDelegationFuture<E> extends SubscriptionIntermediateDelegationFuture<E>
{
	/** The future functionality. */
	protected FutureFunctionality func;
	
//	//-------- debugging --------
//	ISubscriptionIntermediateFuture<?> mysrc;
//	List<Object>	myresults	= new ArrayList<>();
//	@Override
//	public String toString()
//	{
//		return super.toString() + "(storeforfirst="+storeforfirst+", src="+mysrc+", results="+results+", ownresults="+ownresults+", myresults="+myresults+")";
//	}
//	@Override
//	protected void	storeResult(Object result, boolean scheduled)
//	{
//		if((""+result).contains("IMarkerService"))
//		{
//			try
//			{
//				myresults.add(result);
//				super.storeResult(result, scheduled);
//			}
//			finally
//			{
//				Logger.getLogger(getClass().getName()).info("storeResult: "+this+", "+result+", "+IComponentIdentifier.LOCAL.get());
//			}
//		}
//		else
//		{
//			super.storeResult(result, scheduled);
//		}
//	}
//	//-------- debugging end --------

	
	/**
	 * 
	 */
	public DelegatingSubscriptionIntermediateDelegationFuture(FutureFunctionality func)
	{
		if(func==null)
			throw new IllegalArgumentException("Func must not null.");
		this.func = func;
		func.setFuture(this);
	}
	
	/**
	 * 
	 */
	public DelegatingSubscriptionIntermediateDelegationFuture(ISubscriptionIntermediateFuture<E> src, FutureFunctionality func)
	{
//		this.mysrc	= src;	// for debugging only
		
		if(func==null)
			throw new IllegalArgumentException("Func must not null.");
		this.func = func;
		func.setFuture(this);
		src.delegateTo(this);
	}
	
	/**
	 *  Overwritten to change result, if necessary.
	 */
	@Override
	protected boolean	doSetResult(Collection<E> result, boolean undone)
	{
		try
		{
			result = (Collection<E>)func.handleResult(result);
			return DelegatingSubscriptionIntermediateDelegationFuture.super.doSetResult(result, func.isUndone(undone));
		}
		catch(Exception e)
		{
			return doSetException(e, func.isUndone(undone));
		}		
	}
	
	/**
	 *  Overwritten to change undone, if necessary.
	 */
	@Override
	protected boolean	doSetException(Exception exception, boolean undone)
	{
		func.handleException(exception);
		return DelegatingSubscriptionIntermediateDelegationFuture.super.doSetException(exception, func.isUndone(undone));
	}
	
	/**
	 *  Overwritten to change result, if necessary.
	 */
	@Override
	protected boolean	doAddIntermediateResult(E result, boolean undone)
	{
//		if((""+result).contains("IMarkerService"))
////			|| (""+result).contains("PartDataChunk"))
//		{
//			Logger.getLogger(getClass().getName()).info("add: "+this+", "+result+", "+IComponentIdentifier.LOCAL.get());
//		}
		try
		{
			result = (E)func.handleIntermediateResult(result);
			boolean ret = FutureFunctionality.DROP_INTERMEDIATE_RESULT.equals(result) ? false
				: DelegatingSubscriptionIntermediateDelegationFuture.super.doAddIntermediateResult(result, func.isUndone(undone));
			func.handleAfterIntermediateResult(result);
			return ret;
		}
		catch(Exception e)
		{
			return doSetException(e, func.isUndone(undone));
		}
	}

	/**
	 *  Overwritten to change result, if necessary.
	 */
	@Override
	protected synchronized boolean doSetFinished(boolean undone)
	{
		try
		{
			func.handleFinished((Collection<Object>)getIntermediateResults());
			return DelegatingSubscriptionIntermediateDelegationFuture.super.doSetFinished(func.isUndone(undone));
		}
		catch(Exception e)
		{
			return doSetException(e, func.isUndone(undone));
		}		
	}

	/**
     *  Execute a notification. Override for scheduling on other threads.
     */
	@Override
    protected void	executeNotification(IResultListener<Collection<E>> listener, ICommand<IResultListener<Collection<E>>> command)
    {
		func.scheduleForward(command, listener);
    }
	
	/**
	 *  Terminate the future.
	 */
	@Override
	public void terminate(final Exception reason)
	{
		func.scheduleBackward(new ICommand<Void>()
		{
			@Override
			public void execute(Void args)
			{
				func.handleTerminated(reason);
				DelegatingSubscriptionIntermediateDelegationFuture.super.terminate(reason);
			}	
		});
	}
	
	/**
	 *  Send a backward command.
	 */
	@Override
	public void sendBackwardCommand(final Object info)
	{
		func.scheduleBackward(new ICommand<Void>()
		{
			@Override
			public void execute(Void args)
			{
				func.handleBackwardCommand(info);
				DelegatingSubscriptionIntermediateDelegationFuture.super.sendBackwardCommand(info);
			}	
		});
	}
	
	@Override
	public void	addResultListener(IResultListener listener)
	{
		listener	= func.handleAddResultListener(listener);
		if(listener!=null)
		{
			super.addResultListener(listener);
		}
	}
};


/**
 * 
 */
class DelegatingTerminableIntermediateDelegationFuture<E> extends TerminableIntermediateDelegationFuture<E>
{
	/** The future functionality. */
	protected FutureFunctionality func;
	
	/**
	 * 
	 */
	public DelegatingTerminableIntermediateDelegationFuture(FutureFunctionality func)
	{
		if(func==null)
			throw new IllegalArgumentException("Func must not null.");
		this.func = func;
		func.setFuture(this);
	}
	
	/**
	 * 
	 */
	public DelegatingTerminableIntermediateDelegationFuture(ITerminableIntermediateFuture<E> src, FutureFunctionality func)
	{
		if(func==null)
			throw new IllegalArgumentException("Func must not null.");
		this.func = func;
		func.setFuture(this);
		src.delegateTo(this);
	}
	
	/**
	 *  Overwritten to change result, if necessary.
	 */
	@Override
	protected boolean doSetResult(Collection<E> result, boolean undone)
	{
		try
		{
			result = (Collection<E>)func.handleResult(result);
			return DelegatingTerminableIntermediateDelegationFuture.super.doSetResult(result, func.isUndone(undone));
		}
		catch(Exception e)
		{
			return doSetException(e, func.isUndone(undone));
		}		
	}
	
	/**
	 *  Overwritten to change undone, if necessary.
	 */
	@Override
	protected boolean	doSetException(Exception exception, boolean undone)
	{
		func.handleException(exception);
		return DelegatingTerminableIntermediateDelegationFuture.super.doSetException(exception, func.isUndone(undone));
	}
	
	/**
	 *  Overwritten to change result, if necessary.
	 */
	@Override
	protected boolean	doAddIntermediateResult(E result, boolean undone)
	{
		try
		{
			result = (E)func.handleIntermediateResult(result);
			boolean ret = FutureFunctionality.DROP_INTERMEDIATE_RESULT.equals(result) ? false
				: DelegatingTerminableIntermediateDelegationFuture.super.doAddIntermediateResult(result, func.isUndone(undone));
			func.handleAfterIntermediateResult(result);
			return ret;
		}
		catch(Exception e)
		{
			return doSetException(e, func.isUndone(undone));
		}		
	}

	/**
	 *  Overwritten to change result, if necessary.
	 */
	@Override
	protected synchronized boolean doSetFinished(boolean undone)
	{
		try
		{
			func.handleFinished((Collection<Object>)getIntermediateResults());
			return DelegatingTerminableIntermediateDelegationFuture.super.doSetFinished(func.isUndone(undone));
		}
		catch(Exception e)
		{
			return doSetException(e, func.isUndone(undone));
		}		
	}

	/**
     *  Execute a notification. Override for scheduling on other threads.
     */
	@Override
    protected void	executeNotification(IResultListener<Collection<E>> listener, ICommand<IResultListener<Collection<E>>> command)
    {
		func.scheduleForward(command, listener);
    }
	
	/**
	 *  Terminate the future.
	 */
	@Override
	public void terminate(final Exception reason)
	{
		func.scheduleBackward(new ICommand<Void>()
		{
			@Override
			public void execute(Void args)
			{
				func.handleTerminated(reason);
				DelegatingTerminableIntermediateDelegationFuture.super.terminate(reason);
			}	
		});
	}
	
	/**
	 *  Send a backward command.
	 */
	@Override
	public void sendBackwardCommand(final Object info)
	{
		func.scheduleBackward(new ICommand<Void>()
		{
			@Override
			public void execute(Void args)
			{
				func.handleBackwardCommand(info);
				DelegatingTerminableIntermediateDelegationFuture.super.sendBackwardCommand(info);
			}	
		});
	}
	
	@Override
	public void	addResultListener(IResultListener listener)
	{
		listener	= func.handleAddResultListener(listener);
		if(listener!=null)
		{
			super.addResultListener(listener);
		}
	}
};

/**
 * 
 */
class DelegatingTerminableDelegationFuture<E> extends TerminableDelegationFuture<E>
{
	/** The future functionality. */
	protected FutureFunctionality func;
	
	/**
	 * 
	 */
	public DelegatingTerminableDelegationFuture(FutureFunctionality func)
	{
		this.func = func;
		func.setFuture(this);
	}
	
	/**
	 * 
	 */
	public DelegatingTerminableDelegationFuture(ITerminableFuture<E> src, FutureFunctionality func)
	{
		// Cannot use super because it triggers and func is still null
//		super(src);
		this.func = func;
		func.setFuture(this);
		src.delegateTo(this);
	}
	
	/**
	 *  Overwritten to change result, if necessary.
	 */
	@Override
	protected boolean	doSetResult(E result, boolean undone)
	{
		try
		{
			result = (E)func.handleResult(result);
			return DelegatingTerminableDelegationFuture.super.doSetResult(result, func.isUndone(undone));
		}
		catch(Exception e)
		{
			return doSetException(e, func.isUndone(undone));
		}		
	}
	
	/**
	 *  Overwritten to change undone, if necessary.
	 */
	@Override
	protected boolean	doSetException(Exception exception, boolean undone)
	{
		func.handleException(exception);
		return DelegatingTerminableDelegationFuture.super.doSetException(exception, func.isUndone(undone));
	}
	
	/**
     *  Execute a notification. Override for scheduling on other threads.
     */
	@Override
    protected void	executeNotification(IResultListener<E> listener, ICommand<IResultListener<E>> command)
    {
		func.scheduleForward(command, listener);
    }
	
	/**
	 *  Terminate the future.
	 */
	@Override
	public void terminate(final Exception reason)
	{
		func.scheduleBackward(new ICommand<Void>()
		{
			@Override
			public void execute(Void args)
			{
				func.handleTerminated(reason);
				DelegatingTerminableDelegationFuture.super.terminate(reason);
			}	
		});
	}
	
	/**
	 *  Send a backward command.
	 */
	@Override
	public void sendBackwardCommand(final Object info)
	{
		func.scheduleBackward(new ICommand<Void>()
		{
			@Override
			public void execute(Void args)
			{
				func.handleBackwardCommand(info);
				DelegatingTerminableDelegationFuture.super.sendBackwardCommand(info);
			}	
		});
	}
	
	@Override
	public void	addResultListener(IResultListener listener)
	{
		listener	= func.handleAddResultListener(listener);
		if(listener!=null)
		{
			super.addResultListener(listener);
		}
	}
};

/**
 * 
 */
class DelegatingIntermediateFuture extends IntermediateFuture<Object>
{
	//-------- debugging --------
	@Override
	public String toString()
	{
		return super.toString() + "(listeners="+listeners+")";
	}
	//-------- debugging end --------

	
	/** The future functionality. */
	protected FutureFunctionality func;
	
	/**
	 * 
	 */
	public DelegatingIntermediateFuture(FutureFunctionality func)
	{
		this.func = func;
		func.setFuture(this);
	}
	
	/**
	 *  Overwritten to change result, if necessary.
	 */
	@Override
	protected boolean	doSetResult(Collection<Object> result, boolean undone)
	{
		try
		{
			result = (Collection<Object>)func.handleResult(result);
			return DelegatingIntermediateFuture.super.doSetResult(result, func.isUndone(undone));
		}
		catch(Exception e)
		{
			return doSetException(e, func.isUndone(undone));
		}		
	}
	
	/**
	 *  Overwritten to change undone, if necessary.
	 */
	@Override
	protected boolean	doSetException(Exception exception, boolean undone)
	{
		func.handleException(exception);
		return DelegatingIntermediateFuture.super.doSetException(exception, func.isUndone(undone));
	}
	
	/**
	 *  Overwritten to change result, if necessary.
	 */
	@Override
	protected boolean	doAddIntermediateResult(Object result, boolean undone)
	{
		try
		{
//			//-------- debugging --------
//			if((""+result).contains("PartDataChunk"))
//			{
//				System.out.println("DelegatingIntermediateFuture.doAddIntermediateResult: "+this+", "+result+", "+IComponentIdentifier.LOCAL.get());
//			}
//			//-------- debugging end --------
			
			result = func.handleIntermediateResult(result);
			boolean ret = FutureFunctionality.DROP_INTERMEDIATE_RESULT.equals(result) ? false
				: DelegatingIntermediateFuture.super.doAddIntermediateResult(result, func.isUndone(undone));
			func.handleAfterIntermediateResult(result);
			return ret;
		}
		catch(Exception e)
		{
			return doSetException(e, func.isUndone(undone));
		}		
	}

	/**
	 *  Overwritten to change result, if necessary.
	 */
	@Override
	protected synchronized boolean doSetFinished(boolean undone)
	{
		try
		{
			func.handleFinished(getIntermediateResults());
			return DelegatingIntermediateFuture.super.doSetFinished(func.isUndone(undone));
		}
		catch(Exception e)
		{
			return doSetException(e, func.isUndone(undone));
		}		
	}

	/**
     *  Execute a notification. Override for scheduling on other threads.
     */
	@Override
    protected void	executeNotification(IResultListener<Collection<Object>> listener, ICommand<IResultListener<Collection<Object>>> command)
    {
		func.scheduleForward(command, listener);
    }
	
	@Override
	public void	addResultListener(IResultListener listener)
	{
		listener	= func.handleAddResultListener(listener);
		if(listener!=null)
		{
			super.addResultListener(listener);
		}
	}
};

/**
 * 
 */
class DelegatingFuture extends Future<Object>
{
	/** The future functionality. */
	protected FutureFunctionality func;
	
	/**
	 * 
	 */
	public DelegatingFuture(FutureFunctionality func)
	{
		this.func = func;
		func.setFuture(this);
	}
	
	/**
	 *  Overwritten to change result, if necessary.
	 */
	@Override
	public boolean	doSetResult(Object result, boolean undone)
	{
		try
		{
//			if(result!=null && ProxyFactory.isProxyClass(result.getClass()))
//				System.out.println("DelegatingFuture.setResult: "+result);
			result = func.handleResult(result);
			return DelegatingFuture.super.doSetResult(result, func.isUndone(undone));
		}
		catch(Exception e)
		{
			return doSetException(e, func.isUndone(undone));
		}		
	}
	
	/**
	 *  Overwritten to change undone, if necessary.
	 */
	@Override
	protected boolean	doSetException(Exception exception, boolean undone)
	{
		func.handleException(exception);
		return DelegatingFuture.super.doSetException(exception, func.isUndone(undone));
	}
	
	/**
     *  Execute a notification. Override for scheduling on other threads.
     */
	@Override
    protected void	executeNotification(IResultListener<Object> listener, ICommand<IResultListener<Object>> command)
    {
		if((""+listener).indexOf("Heisenbug")!=-1)
			System.err.println("exe0: "+this+", "+command+", "+listener+", "+func.getClass());
		try
		{
			func.scheduleForward(command, listener);
		}
		finally
		{
			if((""+listener).indexOf("Heisenbug")!=-1)
			{
				System.err.println("exe1: "+this+", "+command+", "+listener);
				Thread.dumpStack();
			}
		}
    }
	
	@Override
	public void	addResultListener(IResultListener<Object> listener)
	{
		listener	= func.handleAddResultListener(listener);
		if(listener!=null)
		{
			super.addResultListener(listener);
		}
	}
};
