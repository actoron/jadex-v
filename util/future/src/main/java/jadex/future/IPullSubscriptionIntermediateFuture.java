package jadex.future;

/**
 *  Intermediate future with pull mechanism.
 *  Allows for pulling results by the caller.
 *  In this way a pull intermediate future is 
 *  similar to an iterator.
 */
public interface IPullSubscriptionIntermediateFuture<E> extends ISubscriptionIntermediateFuture<E> //IIntermediateFuture<E>
{
	/**
	 *  Pull an intermediate result.
	 */
	public void pullIntermediateResult();
}