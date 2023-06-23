package jadex.commons.future;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jadex.commons.future.IntermediateFutureTest.TestListener;
import jadex.future.SubscriptionIntermediateFuture;

/**
 *  Test notification of quiet and non-quiet listeners.
 */
class SubscriptionFutureTest
{
	@Test
	void testListenerNotification()
	{
		List<String>	results1	= new ArrayList<String>();
		List<String>	results2	= new ArrayList<String>();
		List<String>	results3	= new ArrayList<String>();
		SubscriptionIntermediateFuture<String>	fut	= new SubscriptionIntermediateFuture<>();
		fut.addQuietListener(new TestListener<String>(results1));
		
		fut.addIntermediateResult("A");
		fut.addIntermediateResult("B");
		
		fut.addResultListener(new TestListener<String>(results2));

		fut.addIntermediateResult("C");
		fut.addIntermediateResult("D");
		
		fut.addResultListener(new TestListener<String>(results3));
		
		fut.addIntermediateResult("E");
		fut.addIntermediateResult("F");

		Assertions.assertArrayEquals(results1.toArray(), "ABCDEF".split(""), "quiet results");
		Assertions.assertArrayEquals(results2.toArray(), "ABCDEF".split(""), "first nonquiet results");
		Assertions.assertArrayEquals(results3.toArray(), "EF".split(""), "second nonquiet results");
	}
}