package jadex.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;

import jadex.core.IComponentManager;
import jadex.core.ISubscriptionStep;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.SubscriptionIntermediateFuture;

/**
 *  Test runAsync() functionality.
 */
public class AsyncLambdaTest
{
	@Test
	public void	testCallable()
	{
		assertEquals("Hello world!", IComponentManager.get().runAsync(new Callable<IFuture<String>>()
		{
			@Override
			public IFuture<String> call() throws Exception
			{
				return IExecutionFeature.get().scheduleStep(() -> "Hello world!");
			}
		}).get(AbstractExecutionFeatureTest.TIMEOUT));
	}
	
	@Test
	public void	testSubscription()
	{
		ISubscriptionIntermediateFuture<String>	ret	= IComponentManager.get().runAsync((ISubscriptionStep<String>)comp -> 
		{
			SubscriptionIntermediateFuture<String>	fut	= new SubscriptionIntermediateFuture<>();			IExecutionFeature.get().scheduleStep(() -> fut.addIntermediateResult("Hello"));
			IExecutionFeature.get().scheduleStep(() -> fut.addIntermediateResult("world"));
			IExecutionFeature.get().scheduleStep(() -> fut.addIntermediateResult("!"));
			IExecutionFeature.get().scheduleStep(() -> fut.setFinished());
			return fut;
		});
		assertEquals("Hello", ret.getNextIntermediateResult(AbstractExecutionFeatureTest.TIMEOUT));
		assertEquals("world", ret.getNextIntermediateResult(AbstractExecutionFeatureTest.TIMEOUT));
		assertEquals("!", ret.getNextIntermediateResult(AbstractExecutionFeatureTest.TIMEOUT));
		assertFalse(ret.hasNextIntermediateResult(AbstractExecutionFeatureTest.TIMEOUT, true));
	}
}
