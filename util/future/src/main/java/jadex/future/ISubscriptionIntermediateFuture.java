package jadex.future;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 *  The subscription intermediate future does not save the results but
 *  instead uses a fire and forget semantics.
 *  
 *  A listener gets current intermediate results in intermediateResultAvailable().
 *  In getIntermediateResults() null is returned unless blocking access is also used.
 */
public interface ISubscriptionIntermediateFuture<E> extends ITerminableIntermediateFuture<E>
{
	/**
	 *  Add a listener which does not consume the initial results.
	 *  I.e. even if it is the first listener to be added to this future,
	 *  a second listener will still receive the initial results.
	 */
	public void	addQuietListener(IResultListener<Collection<E>> listener);

    /**
     *  Get the intermediate results that are available.
     *  Note: The semantics of this method is different to the normal intermediate future
     *  due to the fire-and-forget-semantics!
     *  
     *  @return
     *  1) <i>Non-blocking</I> access only: An empty collection, unless if the future is in "store-for-first" mode (default)
     *  	and no listeners has yet been added, in which case the results until now are returned.<br>
     *  2) Also <i>blocking</i> access from same thread: All results since the first blocking access
     *  	that have not yet been consumed by getNextIntermediateResult().
     */
	public List<E> getIntermediateResults();
	
	@Override
	default IFuture<Collection<E>> then(Consumer<? super Collection<E>> function)
	{
		// then makes no sense as results are not stored and most futures will never complete.
		throw new UnsupportedOperationException("then() is not supported for subscription futures.");
	}
	
	@Override
	default <T> IFuture<T> thenApply(Function<? super Collection<E>, ? extends T> function)
	{
		// then makes no sense as results are not stored and most futures will never complete.
		throw new UnsupportedOperationException("then() is not supported for subscription futures.");
	}
	
	@Override
	default <T> IFuture<T> thenApply(Function<? super Collection<E>, ? extends T> function, Class<?> futuretype)
	{
		// then makes no sense as results are not stored and most futures will never complete.
		throw new UnsupportedOperationException("then() is not supported for subscription futures.");
	}
	
	@Override
	default <T> IFuture<T> thenCompose(Function<? super Collection<E>, IFuture<T>> function)
	{
		// then makes no sense as results are not stored and most futures will never complete.
		throw new UnsupportedOperationException("then() is not supported for subscription futures.");
	}
	
	@Override
	default <T> IFuture<T> thenCompose(Function<? super Collection<E>, IFuture<T>> function, Class<?> futuretype)
	{
		// then makes no sense as results are not stored and most futures will never complete.
		throw new UnsupportedOperationException("then() is not supported for subscription futures.");
	}
}
