package jadex.future;


import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 *  Future that support intermediate results.
 */
//@Reference
public interface IIntermediateFuture<E> extends IFuture<Collection <E>>
{
	//-------- constants --------

	/**
	 *  A future representing a completed action. Can be used as direct return
	 *  value of methods that do not perform asynchronous operations and do not
	 *  return a result value.
	 */
	public static final IntermediateFuture<Void> DONE = new IntermediateFuture<Void>((Collection<Void>)null);

	//-------- methods --------
	
    /**
     *  Get the intermediate results that are currently available.
     *  Non-blocking method.
     *  @return The future result.
     */
    public Collection<E> getIntermediateResults();

    /**
     *  Check if there are more results for iteration for the given caller.
     *  If there are currently no unprocessed results and future is not yet finished,
     *  the caller is blocked until either new results are available and true is returned
     *  or the future is finished, thus returning false.
     *  
     *  @return	True, when there are more intermediate results for the caller.
     */
    public boolean hasNextIntermediateResult();
	
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
    public boolean hasNextIntermediateResult(long timeout, boolean realtime);
	
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
    public E getNextIntermediateResult();
    
    /**
     *  Iterate over the intermediate results in a blocking fashion.
     *  Manages results independently for different callers, i.e. when called
     *  from different threads, each thread receives all intermediate results.
     *  
     *  The operation is guaranteed to be non-blocking, if hasNextIntermediateResult()
     *  has returned true before for the same caller. Otherwise the caller is blocked
     *  until a result is available or the future is finished.
     *  
	 *  @param timeout The timeout in millis.
     *  @return	The next intermediate result.
     *  @throws NoSuchElementException when there are no more intermediate results and the future is finished. 
     */
    public E getNextIntermediateResult(long timeout);
    
    /**
     *  Iterate over the intermediate results in a blocking fashion.
     *  Manages results independently for different callers, i.e. when called
     *  from different threads, each thread receives all intermediate results.
     *  
     *  The operation is guaranteed to be non-blocking, if hasNextIntermediateResult()
     *  has returned true before for the same caller. Otherwise the caller is blocked
     *  until a result is available or the future is finished.
     *  
	 *  @param timeout The timeout in millis.
	 *  @param realtime Flag, if wait should be realtime (in constrast to simulation time).
     *  @return	The next intermediate result.
     *  @throws NoSuchElementException when there are no more intermediate results and the future is finished. 
     */
    public E getNextIntermediateResult(long timeout, boolean realtime);
    
    /**
     *  Called when the next intermediate value is available.
     *  @param function Called when value arrives.
     *  @return The future for chaining.
     */
	public IIntermediateFuture<? extends E> next(Consumer<? super E> function);
	
	/**
     *  Called when the maximum number of results is available.
     *  @param function Called when max value arrives.
     *  @return The future for chaining.
     */
	public IIntermediateFuture<? extends E> max(Consumer<Integer> function);
	
	/**
     *  Called when the future is finished.
     *  @param function Called when max value arrives.
     *  @return The future for chaining.
     */
	public IIntermediateFuture<? extends E> finished(Consumer<Void> function);
	
	/**
     *  Called when the future is done (finished or exception occurred).
     *  Exception parameter will be set if the cause was an exception, null otherwise.
     *  
     *  @param function Called future is done.
     *  @return The future for chaining.
     */
	public IIntermediateFuture<? extends E> done(Consumer<? super Exception> function);
    
    /**
	 *  Implements async loop and applies a an async function to each element.
	 *  @param function The function.
	 *  @return True result intermediate future.
	 */
	//public <R> IIntermediateFuture<R> mapAsync(Function<E, IFuture<R>> function);
	
	/**
	 *  Implements async loop and applies a an async function to each element.
	 *  @param function The function.
	 *  @return True result intermediate future.
	 */
	//public <R> IIntermediateFuture<R> mapAsync(Function<E, IFuture<R>> function, Class<?> futuretype);
	
	/**
	 *  Implements async loop and applies a an async multi-function to each element.
	 *  @param function The function.
	 *  @return True result intermediate future.
	 */
	//public <R> IIntermediateFuture<R> flatMapAsync(Function<E, IIntermediateFuture<R>> function);

	/**
	 *  Return a stream of the results of this future.
	 *  Although this method itself is non-blocking,
	 *  all terminal stream methods (e.g. forEach) will block until the future is finished!
	 */
	public Stream<E>	asStream();

	/**
	 *  Return a stream of the results of this future.
	 *  Use the given timeout settings when waiting for elements in the stream.
	 *  Although this method itself is non-blocking,
	 *  all terminal stream methods (e.g. forEach) will block until the future is finished!
	 *  @param timeout The timeout in millis.
	 *  @param realtime Flag, if wait should be realtime (in constrast to simulation time).
	 */
	public Stream<E>	asStream(long timeout, boolean realtime);
}
