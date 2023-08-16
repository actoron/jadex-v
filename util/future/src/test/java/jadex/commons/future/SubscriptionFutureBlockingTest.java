package jadex.commons.future;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Assertions;

import jadex.future.SubscriptionIntermediateFuture;

/**
 *  Test blocking access to subscription futures.
 */
public class SubscriptionFutureBlockingTest
{
	@org.junit.jupiter.api.Test
	void testNonBlocking()
	{
		// Initialize future
		SubscriptionIntermediateFuture<String>	fut	= new SubscriptionIntermediateFuture<>();
		Assertions.assertEquals(Collections.emptySet(), new HashSet<>(fut.getIntermediateResults()), "initially empty");
		
		// Add and get some results
		List<String>	results	= new ArrayList<>();
		results.add("A");
		results.add("B");
		for(String result: results)
			fut.addIntermediateResult(result);
		Assertions.assertEquals(results, new ArrayList<>(fut.getIntermediateResults()), "nonblocking results");
		Assertions.assertEquals(results, new ArrayList<>(fut.getIntermediateResults()), "nonblocking results stay available");

		// Blocking fetch -> consumes results
		for(String result: results)
			Assertions.assertEquals(result, fut.getNextIntermediateResult(), "fetch result");
		Assertions.assertEquals(Collections.emptySet(), new HashSet<>(fut.getIntermediateResults()), "empty after blocking acess");

		// Add and get some more results
		results.clear();
		results.add("A");
		results.add("B");
		for(String result: results)
			fut.addIntermediateResult(result);
		Assertions.assertEquals(results, new ArrayList<>(fut.getIntermediateResults()), "nonblocking results");
		Assertions.assertEquals(results, new ArrayList<>(fut.getIntermediateResults()), "nonblocking results stay available");
	}
}