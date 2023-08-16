package jadex.commons.future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jadex.future.IntermediateFuture;

/**
 *  Test using intermediate futures via java 8 stream API.
 */
public class StreamFutureTest
{
	/**
	 *  Test that results of a finished future can be accumulated. 
	 */
	@Test
	public void testFinishedFuture()
	{
		IntermediateFuture<Integer>	fut	= new IntermediateFuture<Integer>();
		Stream<Integer>	stream	= fut.asStream();
		
		fut.addIntermediateResult(1);
		fut.addIntermediateResult(2);
		fut.addIntermediateResult(3);
		fut.addIntermediateResult(4);
		fut.addIntermediateResult(5);
		fut.setFinished();
		
		Assertions.assertEquals("12345", stream.map(num -> num.toString()).reduce((result, next) -> result+next).get());	
	}
	
	/**
	 *  Test if results of an unfinished future are processed.
	 */
	@Test
	public void testUnfinishedFuture()
	{
		IntermediateFuture<Integer>	fut	= new IntermediateFuture<Integer>();
		Stream<Integer>	stream	= fut.asStream();
		
		fut.addIntermediateResult(1);
		fut.addIntermediateResult(2);
		fut.addIntermediateResult(3);
		fut.addIntermediateResult(4);
		fut.addIntermediateResult(5);
		new Thread(()->
		{
			try{ Thread.sleep(300); } catch (InterruptedException e) {}
			fut.setFinished();
		}).start();
		
		assertFalse(fut.isDone());	
		assertEquals("12345", stream.map(num -> num.toString()).reduce((result, next) -> result+next).get());	
	}

}