package jadex.future;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jadex.common.ICommand;

/**
 *  Default implementation of an intermediate future.
 */
public class IntermediateFuture<E> extends Future<Collection <E>> implements IIntermediateFuture<E> 
{
	//-------- attributes --------
	
	/** The intermediate results. */
	protected List<E> results;
	
	/** Flag indicating that addIntermediateResult()has been called. */
	protected boolean intermediate;
	
	/** The blocked intermediate callers (caller->state). */
	protected Map<ISuspendable, String> icallers;
    
	/** The index of the next result for a thread. */
    protected Map<Thread, Integer>	indices;
    
    /** The max result count (if given by the producer). */
    protected int maxresultcnt = -1;
    
	//-------- constructors--------
	
	/**
	 *  Create a future that is already done.
	 */
	public IntermediateFuture()
	{
	}
	
	/**
	 *  Create a future that is already done.
	 *  @param results	The results, if any.
	 */
	public IntermediateFuture(Collection<E> results)
	{
		super(results);
	}
	
	/**
	 *  Create a future that is already done (failed).
	 *  @param exception The exception.
	 */
	public IntermediateFuture(Exception exception)
	{
		super(exception);
	}
	
	//-------- IIntermediateFuture interface --------
		
    /**
     *  Get the intermediate results that are available.
     *  @return The current intermediate results (copy of the list).
     */
	public synchronized Collection<E> getIntermediateResults()
	{
		Collection<E>	ret;
		if(results!=null)
			ret	= new ArrayList<E>(results);
		else
			ret	= Collections.emptyList();
		return ret;
	}
	
	//-------- methods --------
	
	/**
	 *  Add an intermediate result.
	 */
	public void	addIntermediateResult(E result)
	{
	   	doAddIntermediateResult(result, false);

	   	resumeIntermediate();
	}
	
	/**
     *  Set the result. 
     *  @param result The result.
     *  @return True if result was set.
     */
    public boolean addIntermediateResultIfUndone(E result)
    {
    	boolean	ret	= doAddIntermediateResult(result, true);

    	if(ret)
    		resumeIntermediate();
    	
    	return ret;
    }
	
    /**
     *  Set the result and schedule listener notifications.
     *  @return true, when the result was added (finished and undone otherwise).
     */
    protected boolean doAddIntermediateResult(final E result, boolean undone)
    {
//		//-------- debugging --------
//		if((""+result).contains("PartDataChunk"))
//		{
//			System.out.println("IntermediateFuture.doAddIntermediateResult0: "+this+", "+result);
//		}
//		//-------- debugging end --------

    	boolean	ret	= true;
    	boolean	notify	= false;
    	
    	synchronized(this)
    	{
//    		//-------- debugging --------
//    		if((""+result).contains("PartDataChunk"))
//    		{
//    			System.out.println("IntermediateFuture.doAddIntermediateResult1: "+this+", "+result);
//    		}
//    		//-------- debugging end --------
    		
	    	if(undone)
	    		this.undone = true;
	
	    	// There is an exception when this is ok.
	    	// In BDI when belief value is a future.
	//    	if(result instanceof IFuture)
	//    	{
	//    		System.out.println("Internal error, future in future.");
	//    		setException(new RuntimeException("Future in future not allowed."));
	//    	}
	    	
	    	if(isDone())
	    	{
//	    		//-------- debugging --------
//	    		if((""+result).contains("PartDataChunk"))
//	    		{
//	    			System.out.println("IntermediateFuture.doAddIntermediateResult2: "+this+", "+result+", "+Thread.currentThread());
//	    		}
//	    		//-------- debugging end --------
	    		
	    		if(undone)
	    		{
	    			ret	= false;
	    		}
	    		else if(this.exception!=null)
	    		{
	        		throw new DuplicateResultException(DuplicateResultException.TYPE_EXCEPTION_RESULT, this, this.exception, result);
	    		}
	    		else
	    		{
	        		throw new DuplicateResultException(DuplicateResultException.TYPE_RESULT_RESULT, this, this.result, result);        			
	    		}
	    	}
	    	else
	    	{
	    		//if(listener!=null && getResultCount()==1)
	    		//	scheduleMaxNotification(null);
	    		
//				//-------- debugging --------
//				if((""+result).contains("PartDataChunk"))
//				{
//					System.out.println("IntermediateFuture.doAddIntermediateResult3: "+this+", "+result+", "+listeners+", "+Thread.currentThread());
//				}
//				//-------- debugging end --------

	    		
	    		boolean	scheduled	= scheduleNotification(listener -> listener instanceof IIntermediateResultListener, new ICommand<IResultListener<Collection<E>>>()
				{
	    			@Override
	    			public void execute(IResultListener<Collection<E>> listener)
	    			{
//	    				//-------- debugging --------
//	    				if((""+result).contains("PartDataChunk"))
//	    				{
//	    					System.out.println("IntermediateFuture.doAddIntermediateResult4: "+IntermediateFuture.this+", "+result+", "+listener);
//	    				}
//	    				//-------- debugging end --------
		        		notifyIntermediateResult((IIntermediateResultListener<E>)listener, result);
	    			}
				});
	    		
	    		storeResult(result, scheduled);
	    		notify	= true;
	    	}
    	}
    	
    	if(notify)
    		startScheduledNotifications();
    	
    	return ret;
    }

    
	/**
	 *  Add a result.
	 *  @param result The result.
	 *  @param scheduled	True, if any listener notification has been scheduled for this result. (used for subscription futures to check for lost values)
	 */
	protected void	storeResult(E result, boolean scheduled)
	{
//		//-------- debugging --------
//		if((""+result).contains("PartDataChunk"))
//		{
//			System.out.println("storeResult: "+IntermediateFuture.this+", "+result+", "+listeners);
//		}
//		//-------- debugging end --------

		
//		if(result!=null && result.getClass().getName().indexOf("ChangeEvent")!=-1)
//			System.out.println("ires: "+this+" "+result);
      	intermediate = true;
		if(results==null)
			results	= new ArrayList<E>();
		results.add(result);
		if(maxresultcnt==getResultCount())
		{
//			boolean	fini	= setFinishedIfUndone();
//			System.out.println("Finished due to max cnt: "+this+", "+maxresultcnt+", "+fini);
			setFinishedIfUndone();
		}
		
		//if(result!=null && result.toString().indexOf("Chunk")!=-1)
		//	System.out.println("ifuture: "+results.size()+" "+hashCode());
	}
	
	/**
     *  Set the result. 
     *  Listener notifications occur on calling thread of this method.
     *  @param result The result.
     */
	@Override
	protected synchronized boolean doSetResult(Collection<E> result, boolean undone)
	{
		if(intermediate)
    	{
    		throw new RuntimeException("setResult() only allowed without intermediate results: "+results);
    	}
   		boolean	ret	= super.doSetResult(result, undone);
   		if(ret)
   		{
   			this.results	= result!=null ? new ArrayList<>(result) : null;
   		}
   		return ret;
    }

    /**
     *  Declare that the future is finished.
     */
    public void setFinished()
    {
    	//if(done.contains(this.toString()))
    	//	System.out.println("setFini: "+this);
    	//done.add(this.toString());
    	//Thread.currentThread().dumpStack();
    	doSetFinished(false);
    	
    	resume();
    }
    
    //protected static Set<String> done = new HashSet<>();
    
    /**
     *  Declare that the future is finished.
     */
    public boolean setFinishedIfUndone()
    {
    	boolean	ret	= doSetFinished(true);
    	if(ret)
    		resume();
    	return ret;
    }

    /**
     *  Declare that the future is finished.
     */
    protected synchronized boolean	doSetFinished(boolean undone)
    {
//		//-------- debugging --------
//		if((""+results).contains("PartDataChunk"))
//		{
//			System.out.println("IntermediateFuture.doSetFinished: "+this+", "+results+", "+Thread.currentThread()
//				+"\n"+SUtil.getExceptionStacktrace(new Exception("Stack trace").fillInStackTrace()));
//		}
//		//-------- debugging end --------
    	
    	boolean	 ret;
    	
    	Collection<E>	res	= getIntermediateResults();
    	ret	= super.doSetResult(res, undone);
		if(ret)
		{
			// Hack!!! Set results to avoid inconsistencies between super.result and this.results,
    		// because getIntermediateResults() returns empty list when results==null.
    		if(results==null)
    			results	= Collections.emptyList();
		}

    	return ret;
    }
    
    @Override
    protected boolean doSetException(Exception exception, boolean undone)
    {
    	return super.doSetException(exception, undone);
    }
    
    /**
     *  Add a result listener.
     *  @param listener The listener.
     */
    public void	addResultListener(IResultListener<Collection<E>> listener)
    {
    	if(listener==null)
    		throw new RuntimeException();
       	
    	boolean	notify_intermediate = false;
    	boolean notify_finished;
    	
    	synchronized(this)
    	{    		
        	notify_finished	= doAddResultListener(listener);

    		// If results==null its a subscription future and first results are already collected.
    		if(intermediate && listener instanceof IIntermediateResultListener)
    		{
	    		IIntermediateResultListener<E> lis = (IIntermediateResultListener<E>)listener;
	    		notify_intermediate = scheduleMaxNotification(lis);
	    		//System.out.println("addRes scheduleAll: "+maxresultcnt+" "+this);

	    		if(results!=null && !results.isEmpty())
	    		{    			
	    			//System.out.println("notify scheduled: "+results);
	    			notify_intermediate = true;
		    				    		
		    		for(final E result: results)
		    		{
		    			@SuppressWarnings("unchecked")
						ICommand<IResultListener<Collection<E>>> c = (ICommand<IResultListener<Collection<E>>>) ((Object) new ICommand<IIntermediateResultListener<E>>()
						{
//							//-------- debugging --------
//							@Override
//							public String toString()
//							{
//								return "NotifyIntermediateResultCommand("+listener+", "+result+")";
//							}
//							//-------- debugging end --------
							
		    				@Override
		    				public void execute(IIntermediateResultListener<E> listener)
		    				{
		    					// Use template method to allow overwriting (e.g. for tuple2future).
		    					notifyIntermediateResult(listener, result);
		    				}
						}); 
		    			scheduleNotification(lis, c);
		    		}
	    		}
    		}
    		
        	// Notify final result if any
        	if(notify_finished)
        		scheduleNotification(listener, getNotificationCommand());
    	}

    	// Notify intermediate results and or final result if any
    	if(notify_intermediate || notify_finished)
    		startScheduledNotifications();
    }
    
    protected ICommand<IResultListener<Collection<E>>>	notcommand	= new ICommand<IResultListener<Collection<E>>>()
	{
		@Override
		public void execute(IResultListener<Collection<E>> listener)
		{
	    	// Special handling only required for finished() instead of resultAvailable()
			if(exception==null && listener instanceof IIntermediateResultListener)
			{
				// If non-intermediate future use -> send collection results as intermediate results
				if(!intermediate && results!=null)
				{
		    		for(E result: results)
		    		{
		    			notifyIntermediateResult((IIntermediateResultListener<E>)listener, result);
		    		}
				}
				
    			if(undone && listener instanceof IUndoneIntermediateResultListener)
				{
					((IUndoneIntermediateResultListener<E>)listener).finishedIfUndone();
				}
				else
				{
					((IIntermediateResultListener<E>)listener).finished();
				}
			}
				
			// Use default handling for exception and non-intermediate listeners
			else
			{
				IntermediateFuture.super.getNotificationCommand().execute(listener);
			}
		}
	};
	
    /**
     *  Get the notification command.
     */
    protected ICommand<IResultListener<Collection<E>>>	getNotificationCommand()
    {
    	return notcommand;
    }
    
	/**
	 * Add an result listener, which called on intermediate results.
	 * 
	 * @param listener The intermediate listener.
	 * /
	public void addIntermediateResultListener(IIntermediateResultListener<E> listener)
	{
		addResultListener(listener);
	}*/
    
	/**
	 * Add a functional result listener, which called on intermediate results.
	 * Exceptions will be logged.
	 * 
	 * @param intermediateListener The intermediate listener.
	 * /
	public void addIntermediateResultListener(IFunctionalIntermediateResultListener<E> intermediateListener)
	{
		addIntermediateResultListener(intermediateListener, null, null);
	}*/

	/**
	 * Add a functional result listener, which called on intermediate results.
	 * Exceptions will be logged.
	 * 
	 * @param intermediateListener The intermediate listener.
	 * @param finishedListener The finished listener, called when no more
	 *        intermediate results will arrive. If <code>null</code>, the finish
	 *        event will be ignored.
	 * /
	public void addIntermediateResultListener(IFunctionalIntermediateResultListener<E> intermediateListener, IFunctionalIntermediateFinishedListener<Void> finishedListener)
	{
		addIntermediateResultListener(intermediateListener, finishedListener, null);
	}*/
	
	/**
	 * Add a functional result listener, which called on intermediate results.
	 * 
	 * @param ilistener The intermediate listener.
	 * @param elistener The listener that is called on exceptions. Passing
	 *        <code>null</code> enables default exception logging.
	 * /
    public void addIntermediateResultListener(IFunctionalIntermediateResultListener<E> ilistener, IFunctionalExceptionListener elistener)
    {    	
		addIntermediateResultListener(ilistener, null, elistener);
    }* /

    public void addIntermediateResultListener(final IFunctionalIntermediateResultListener<E> ilistener, final IFunctionalIntermediateFinishedListener<Void> flistener,
    	IFunctionalExceptionListener elistener)
    {
    	addIntermediateResultListener(ilistener, flistener, elistener, null);
    }
    	
	/**
	 * Add a functional result listener, which called on intermediate results.
	 * 
	 * @param ilistener The intermediate listener.
	 * @param flistener The finished listener, called when no more
	 *        intermediate results will arrive. If <code>null</code>, the finish
	 *        event will be ignored.
	 * @param elistener The listener that is called on exceptions. Passing
	 *        <code>null</code> enables default exception logging.
	 * /
	public void addIntermediateResultListener(final IFunctionalIntermediateResultListener<E> ilistener, final IFunctionalIntermediateFinishedListener<Void> flistener,
		IFunctionalExceptionListener elistener, final IFunctionalIntermediateResultCountListener rlistener)
	{
		final IFunctionalExceptionListener ielistener = (elistener == null) ? SResultListener.printExceptions(): elistener;
		addResultListener(new IntermediateDefaultResultListener<E>()
		{
			public void intermediateResultAvailable(E result)
			{
				ilistener.intermediateResultAvailable(result);
			}

			public void finished()
			{
				if(flistener != null) 
					flistener.finished();
			}

			public void exceptionOccurred(Exception exception)
			{
				ielistener.exceptionOccurred(exception);
			}
			
			public void maxResultCountAvailable(int max) 
			{
				rlistener.maxResultCountAvailable(max);
			}
		});
	}*/

	/**
     *  Check if there are more results for iteration for the given caller.
     *  If there are currently no unprocessed results and future is not yet finished,
     *  the caller is blocked until either new results are available and true is returned
     *  or the future is finished, thus returning false.
     *  
     *  @return	True, when there are more intermediate results for the caller.
     */
    public boolean hasNextIntermediateResult()
    {
    	return hasNextIntermediateResult(UNSET, false);
    }
    
    /**
     *  Check if there are more results for iteration for the given caller.
     *  If there are currently no unprocessed results and future is not yet finished,
     *  the caller is blocked until either new results are available and true is returned
     *  or the future is finished, thus returning false.
     *  
	 *  @param timeout The timeout in millis.
	 *  @param realtime Flag, if wait should be realtime (in constrast to simulation time).
     *  @return	True, when there are more intermediate results for the caller.
     */
    public boolean hasNextIntermediateResult(long timeout, boolean realtime)
    {
    	boolean	ret;
    	boolean	suspend;
		ISuspendable caller = ISuspendable.SUSPENDABLE.get();
	   	if(caller==null)
	   	{
	   		caller = new ThreadSuspendable();
	   	}
	   	
    	synchronized(this)
    	{
    		Integer	index	= indices!=null ? indices.get(Thread.currentThread()) : null;
    		if(index==null)
    		{
    			index	= Integer.valueOf(0);
    		}
    		
    		ret	= (results!=null && results.size()>index.intValue()) || isDone() && getException()!=null;
    		suspend	= !ret && !isDone();
    		if(suspend)
    		{
	    	   	if(icallers==null)
	    	   	{
	    	   		icallers	= Collections.synchronizedMap(new HashMap<ISuspendable, String>());
	    	   	}
	    	   	icallers.put(caller, CALLER_QUEUED);
    		}
    	}
    	
    	if(suspend)
    	{
    		try
    		{
    			caller.getLock().lock();
    			Object	state	= icallers.get(caller);
    			if(CALLER_QUEUED.equals(state))
    			{
    	    	   	icallers.put(caller, CALLER_SUSPENDED);
    				caller.suspend(this, timeout, realtime);
    	    	   	icallers.remove(caller);
    			}
    			// else already resumed.
    		}
    		finally
    		{
    			caller.getLock().unlock();
    		}
	    	ret	= hasNextIntermediateResult(timeout, realtime);
    	}
    	
    	return ret;
    }	
	
    /**
     *  Iterate over the intermediate results in a blocking fashion.
     *  Manages results independently for different callers, i.e. when called
     *  from different threads, each thread receives all intermediate results.
     *  
     *  The operation is guaranteed to be non-blocking, if hasNextIntermediateResult()
     *  has returned true before for the same caller. Otherwise the caller is blocked
     *  until a result is available or the future is finished.
     *  
     *  @return	The next intermediate result.
     *  @throws NoSuchElementException when there are no more intermediate results and the future is finished. 
     */
    public E getNextIntermediateResult()
    {
    	return getNextIntermediateResult(false);
    }
    
    /**
     *  Iterate over the intermediate results in a blocking fashion.
     *  Manages results independently for different callers, i.e. when called
     *  from different threads, each thread receives all intermediate results.
     *  
     *  The operation is guaranteed to be non-blocking, if hasNextIntermediateResult()
     *  has returned true before for the same caller. Otherwise the caller is blocked
     *  until a result is available or the future is finished.
     *  
     *  @return	The next intermediate result.
     *  @throws NoSuchElementException when there are no more intermediate results and the future is finished. 
     */
    public E getNextIntermediateResult(boolean realtime)
    {
    	return getNextIntermediateResult(UNSET, realtime);
    }
    
    /**
     *  Iterate over the intermediate results in a blocking fashion.
     *  Manages results independently for different callers, i.e. when called
     *  from different threads, each thread receives all intermediate results.
     *  
     *  The operation is guaranteed to be non-blocking, if hasNextIntermediateResult()
     *  has returned true before for the same caller. Otherwise the caller is blocked
     *  until a result is available or the future is finished.
     *  
     *  @return	The next intermediate result.
     *  @throws NoSuchElementException when there are no more intermediate results and the future is finished. 
     */
    public E getNextIntermediateResult(long timeout)
    {
    	return getNextIntermediateResult(timeout, false);
    }
    
    /**
     *  Iterate over the intermediate results in a blocking fashion.
     *  Manages results independently for different callers, i.e. when called
     *  from different threads, each thread receives all intermediate results.
     *  
     *  The operation is guaranteed to be non-blocking, if hasNextIntermediateResult()
     *  has returned true before for the same caller. Otherwise the caller is blocked
     *  until a result is available or the future is finished.
     *  
     *  @return	The next intermediate result.
     *  @throws NoSuchElementException when there are no more intermediate results and the future is finished. 
     */
    public E getNextIntermediateResult(long timeout, boolean realtime)
    {
    	Integer	index;
    	synchronized(this)
    	{
			index	= indices!=null ? indices.get(Thread.currentThread()) : null;
			index	= index==null ? Integer.valueOf(1) : Integer.valueOf(index.intValue()+1);
			
			if(indices==null)
				indices	= new HashMap<Thread, Integer>();
			indices.put(Thread.currentThread(), index);
    	}
		return doGetNextIntermediateResult(index.intValue()-1, timeout, realtime);
    }
    
    /**
     *  Set the maximum number of results.
     *  @param max The maximum number of results.
     */
    public void setMaxResultCount(int max)
    {
       	boolean	notify	= false;
    	synchronized(this)
    	{	    	
	    	if(isDone())
	    	{
	    		throw new IllegalStateException("Future already finished.");
	    	}
	    	else if(maxresultcnt!=-1)
	    	{
	    		throw new IllegalStateException("Max result count must only be set once.");	    		
	    	}
	    	else
	    	{
	        	//System.out.println("max set: "+max);
	        	this.maxresultcnt = max;
	        	intermediate = intermediate | max!=-1;
	        	
	        	if(hasResultListener())
	        	{
		    		notify	= scheduleMaxNotification(null);
	        	}
	    	}
    	}
    	
    	if(notify)
    		startScheduledNotifications();
    	
    	if(getResultCount()==max)
    		setFinishedIfUndone();
    }
    
    /**
     * Schedule max notification
     * @param lis The result listener
     */
    protected boolean scheduleMaxNotification(IResultListener<Collection<E>> lis)
    {
    	boolean donotify = false;
    	
    	if(maxresultcnt!=-1)
    	{
    		donotify = true;
    		//System.out.println("donotify max: "+maxresultcnt+" "+lis);
    		
    		ICommand<IResultListener<Collection<E>>> com = new ICommand<IResultListener<Collection<E>>>()
			{
				@Override
				public void execute(IResultListener<Collection<E>> listener)
				{
        			((IIntermediateResultListener<E>)listener).maxResultCountAvailable(maxresultcnt);
				}
				
				@Override
				public String toString() 
				{
					return "notifyMaxCount";
				}
			};
    		
			// Important! two methods fitting scheduleNotification(lis, com) :-(
    		if(lis==null)
    			scheduleNotification(l -> l instanceof IIntermediateResultListener, com);
    		else
    			scheduleNotification(lis, com);
    	}
//    	else
//    	{
//    		System.out.println("max: "+maxresultcnt+" "+lis);
//    	}
    	
    	return donotify;
    }
    
    /** 
     *  Get the number of results already collected.
     *  @return The number of results.
     */
    protected int getResultCount()
    {
    	return results!=null? results.size(): 0;
    }
    
    /**
     *  Perform the get without increasing the index.
     */
    protected E doGetNextIntermediateResult(int index, long timeout, boolean realtime)
    {
       	E	ret	= null;
    	boolean	suspend	= false;
		ISuspendable	caller	= ISuspendable.SUSPENDABLE.get();
		if(caller==null)
			caller	= new ThreadSuspendable();

    	synchronized(this)
    	{
    		if(results!=null && results.size()>index)
    		{
    			// Hack!!! it there a better way to access the i-est element?
    			Iterator<E>	it	= results.iterator();
    			for(int i=0; i<=index; i++)
    			{
    				ret	= it.next();
    			}
    		}
    		else if(isDone())
    		{
    			if(getException()!=null)
    				throwException(getException());
    			else
    				throw new NoSuchElementException("No more intermediate results.");
    		}
    		else
    		{
    			suspend	= true;
	    	   	if(icallers==null)
	    	   	{
	    	   		icallers	= Collections.synchronizedMap(new HashMap<ISuspendable, String>());
	    	   	}
	    	   	icallers.put(caller, CALLER_QUEUED);
    		}
   		}
    	
    	if(suspend)
    	{
    		try
    		{
    			caller.getLock().lock();
    			Object	state	= icallers.get(caller);
    			if(CALLER_QUEUED.equals(state))
    			{
    	    	   	icallers.put(caller, CALLER_SUSPENDED);
    				caller.suspend(this, timeout, realtime);
    	    	   	icallers.remove(caller);
    			}
    			// else already resumed.
    		}
    		finally
    		{
    			caller.getLock().unlock();
    		}
	    	ret	= doGetNextIntermediateResult(index, timeout, realtime);
    	}
    	
    	return ret;
    }	
    
    /**
     *  Notify a result listener.
     *  @param listener The listener.
     */
    protected void notifyIntermediateResult(IIntermediateResultListener<E> listener, E result)
    {
    	if(undone && listener instanceof IUndoneIntermediateResultListener)
    	{
			IUndoneIntermediateResultListener<E>	ulistener	= (IUndoneIntermediateResultListener<E>)listener;
    		ulistener.intermediateResultAvailableIfUndone(result);
    	}
    	else
    	{
    		listener.intermediateResultAvailable(result);
    	}
    }

//    /**
//     *  Notify a result listener.
//     *  @param listener The listener.
//     */
//    protected void doNotifyListener(IResultListener<Collection<E>> listener)
//    {
////    	try
////    	{
//			if(exception!=null)
//			{
//				if(undone && listener instanceof IUndoneResultListener)
//				{
//					((IUndoneResultListener<E>)listener).exceptionOccurredIfUndone(exception);
//				}
//				else
//				{
//					listener.exceptionOccurred(exception);
//				}
//			}
//			else
//			{
//				if(listener instanceof IIntermediateResultListener)
//				{
//					IIntermediateResultListener lis = (IIntermediateResultListener)listener;
//					Object[] inter = null;
//					synchronized(this)
//					{
//						if(!intermediate && results!=null)
//						{
//							inter = results.toArray();
//						}
//					}
//					if(inter!=null)
//			    	{
//			    		for(int i=0; i<inter.length; i++)
//			    		{
//			    			notifyIntermediateResult(lis, (E)inter[i]);
//			    		}
//			    	}
//					if(undone && listener instanceof IUndoneIntermediateResultListener)
//					{
//						((IUndoneIntermediateResultListener<E>)listener).finishedIfUndone();
//					}
//					else
//					{
//						lis.finished();
//					}
//				}
//				else
//				{
//					if(undone && listener instanceof IUndoneResultListener)
//					{
//						((IUndoneResultListener)listener).resultAvailableIfUndone(results);
//					}
//					else
//					{
//						listener.resultAvailable(results); 
//					}
//				}
//			}
////    	}
////    	catch(Exception e)
////    	{
////    		e.printStackTrace();
////    	}
//    }
    

    /**
     *  Resume also intermediate waiters.
     */
    protected void resume()
    {
    	super.resume();
    	resumeIntermediate();
    }
    
	/**
	 *  Resume after intermediate result.
	 */
	protected void resumeIntermediate()
	{
		synchronized(this)
		{
			ISuspendable[]	callers	= icallers!=null ? icallers.keySet().toArray(new ISuspendable[0]) : null;
		   	if(callers!=null)
		   	{
				for(ISuspendable caller: callers)
		    	{
		    		try
		    		{
		    			caller.getLock().lock();
		    			String	state = icallers.get(caller);
		    			if(CALLER_SUSPENDED.equals(state))
		    			{
		    				// Only reactivate thread when previously suspended.
		    				caller.resume(this);
		    			}
		    			icallers.put(caller, CALLER_RESUMED);
					}
		    		finally
		    		{
		    			caller.getLock().unlock();
		    		}
		    	}
			}
		}
	}
	
	/**
	 *  Delegate the result and exception from another future.
	 *  @param source The source future.
	 */
	// Overwritten to always add intermediate listener
	public void delegateFrom(IFuture<Collection<E>> source)
	{
		if(source==null)
			throw new IllegalArgumentException("Source must not null");
		
		source.addResultListener(new IntermediateDelegationResultListener<>(this));
	}
	
	//-------- java 8 extensions --------
	
	public IIntermediateFuture<E> catchEx(final Consumer<? super Exception> consumer, Class<?> futuretype)
    {
		IResultListener<Collection<E>> reslis = new IntermediateEmptyResultListener<>()
		{
			public void exceptionOccurred(Exception exception)
			{
				 consumer.accept(exception);
			}
		};
		addResultListener(reslis);
		
		/*this.addResultListener(new IResultListener<E>()
		{
			@Override
			public void exceptionOccurred(Exception exception)
			{
				consumer.accept(exception);
			}
			
			@Override
			public void resultAvailable(E result)
			{
			}
		});*/
		
        return this;
    }
	
	/**
	 *  Called on exception.
	 *  @param delegate The future the exception will be delegated to.
	 */
	public <T> IIntermediateFuture<E> delegateEx(Future<T> delegate)
	{
		IResultListener<Collection<E>> reslis = new IntermediateEmptyResultListener<>()
		{
			public void exceptionOccurred(Exception exception)
			{
				 delegate.setException(exception);
			}
		};
		addResultListener(reslis);
		return this;
	}
	
	// todo: subscriptions need special treatment for first listener
	
	public IIntermediateFuture<E> then(Consumer<? super Collection<E>> function)
    {
		this.addResultListener(new IntermediateEmptyResultListener<E>()
		{
			public void resultAvailable(Collection<E> result)
        	{
        		 function.accept(result);
        	}
		});
        return this;
    }
	
	/**
     *  Called when the next intermediate value is available.
     *  @param function Called when value arrives.
     *  @return The future for chaining.
     */
	public IIntermediateFuture<? extends E> next(Consumer<? super E> function)
	{
		addResultListener(new IntermediateEmptyResultListener<E>()
		{
			public void intermediateResultAvailable(E result)
			{
				function.accept(result);
			}
		});
		return this;
	}
	
	/**
     *  Called when the maximum number of results is available.
     *  @param function Called when max value arrives.
     *  @return The future for chaining.
     */
	public IIntermediateFuture<? extends E> max(Consumer<Integer> function)
	{
		addResultListener(new IntermediateEmptyResultListener<E>()
		{
			public void maxResultCountAvailable(int max) 
			{
				function.accept(max);
			}
		});
		return this;
	}
	
	/**
     *  Called when the future is finished.
     *  @param function Called when max value arrives.
     *  @return The future for chaining.
     */
	public IIntermediateFuture<? extends E> finished(Consumer<Void> function)
	{
		addResultListener(new IntermediateEmptyResultListener<E>()
		{
			public void finished() 
			{
				function.accept(null);
			}
		});
		return this;
	}
	
	/**
     *  Called when the future is done (finished or exception occurred).
     *  Exception parameter will be set if the cause was an exception, null otherwise.
     *  
     *  @param consumer Called future is done.
     *  @return The future for chaining.
     */
	public IIntermediateFuture<? extends E> done(Consumer<? super Exception> consumer)
	{
		addResultListener(new IntermediateEmptyResultListener<E>()
		{
			public void exceptionOccurred(Exception exception)
			{
				consumer.accept(exception);
			}
			
			public void finished() 
			{
				consumer.accept(null);
			}
		});
		return this;
	}
	
	/**
	 *  Implements async loop and applies a an async function to each element.
	 *  @param function The function.
	 *  @return True result intermediate future.
	 * /
	public <R> IIntermediateFuture<R> mapAsync(final Function<E, IFuture<R>> function)
    {
       return mapAsync(function, null);
    }*/
	
	/**
	 *  Implements async loop and applies a an async function to each element.
	 *  @param function The function.
	 *  @return True result intermediate future.
	 * /
	public <R> IIntermediateFuture<R> mapAsync(final Function<E, IFuture<R>> function, Class<?> futuretype)
    {
        final IntermediateFuture<R> ret = futuretype==null? new IntermediateFuture<R>(): (IntermediateFuture)getFuture(futuretype);

        this.addResultListener(new IIntermediateResultListener<E>()
        {
            public void resultAvailable(Collection<E> result)
            {
                for(E v: result)
                {
                    intermediateResultAvailable(v);
                }
                finished();
            }

            public void intermediateResultAvailable(E result)
            {
                IFuture<R> res = function.apply(result);
                res.addResultListener(new IResultListener<R>()
                {
                    public void resultAvailable(R result)
                    {
                        ret.addIntermediateResult(result);
                    }

                    public void exceptionOccurred(Exception exception)
                    {
                        ret.setExceptionIfUndone(exception);
                    }
                });
            }

            public void finished()
            {
                ret.setFinished();
            }

            public void exceptionOccurred(Exception exception)
            {
                ret.setException(exception);
            }
            
            public void maxResultCountAvailable(int count) 
            {
            	ret.setMaxResultCount(count);
            }
        });

        return ret;
    }*/
	
	/**
	 *  Implements async loop and applies a an async multi-function to each element.
	 *  @param function The function.
	 *  @return True result intermediate future.
	 * /
	public <R> IIntermediateFuture<R> flatMapAsync(final Function<E, IIntermediateFuture<R>> function)
    {
        final IntermediateFuture<R> ret = new IntermediateFuture<R>();

        this.addResultListener(new IIntermediateResultListener<E>()
        {
        	boolean fin = false;
        	int cnt = 0;
        	int num = 0;
        	
            public void resultAvailable(Collection<E> result)
            {
                for(E v: result)
                {
                    intermediateResultAvailable(v);
                }
                finished();
            }

            public void intermediateResultAvailable(E result)
            {
            	cnt++;
                IIntermediateFuture<R> res = function.apply(result);
                res.addResultListener(new IIntermediateResultListener<R>()
                {
                    public void intermediateResultAvailable(R result)
                    {
                    	ret.addIntermediateResult(result);
                    }
                    
                    public void finished()
                    {
                    	if(++num==cnt && fin)
                    	{
                    		ret.setFinished();
                    	}
                    }
                    
                    public void resultAvailable(Collection<R> result)
                    {
                    	for(R r: result)
                        {
                            intermediateResultAvailable(r);
                        }
                        finished();
                    }
                    
                    public void exceptionOccurred(Exception exception)
                    {
                    	ret.setExceptionIfUndone(exception);
                    }
                    
                    public void maxResultCountAvailable(int max) 
                    {
                    	ret.setMaxResultCount(max);
                    }
                });
            }

            public void finished()
            {
            	fin = true;
            	if(num==cnt)
            		ret.setFinished();
            }

            public void exceptionOccurred(Exception exception)
            {
                ret.setException(exception);
            }
            
            public void maxResultCountAvailable(int max) 
            {
            	ret.setMaxResultCount(max);
            }
        });

        return ret;
    }*/
	
	/**
	 *  Return a stream of the results of this future.
	 *  Although this method itself is non-blocking,
	 *  all terminal stream methods (e.g. forEach) will block until the future is finished!
	 */
	public Stream<E>	asStream()
	{
		return asStream(UNSET, false);
	}

	/**
	 *  Return a stream of the results of this future.
	 *  Use the given timeout settings when waiting for elements in the stream.
	 *  Although this method itself is non-blocking,
	 *  all terminal stream methods (e.g. forEach) will block until the future is finished!
	 *  @param timeout The timeout in millis.
	 *  @param realtime Flag, if wait should be realtime (in constrast to simulation time).
	 */
	public Stream<E>	asStream(long timeout, boolean realtime)
	{
//		// First try: doesn't handle finished
//		return Stream.generate(() -> getNextIntermediateResult());
		
		// TODO: spliterator characteristics and parallel!?
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<E>()
		{
			@Override
			public boolean hasNext()
			{
				return hasNextIntermediateResult(timeout, realtime);
			}

			@Override
			public E next()
			{
				return getNextIntermediateResult(timeout, realtime);
			}
		}, 0), false);
	}
	
	/**
	 *  Print an exception.
	 */
	public IIntermediateFuture<E> printOnEx()
	{
		this.addResultListener(new IntermediateDefaultResultListener<E>()
		{
			@Override
			public void exceptionOccurred(Exception exception)
			{
				exception.printStackTrace();
			}
		});
		
		return this;
	}
}
