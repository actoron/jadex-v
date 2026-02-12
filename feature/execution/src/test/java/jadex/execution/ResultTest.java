package jadex.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;

import jadex.common.SUtil;
import jadex.core.ChangeEvent;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.IThrowingFunction;
import jadex.core.annotation.NoCopy;
import jadex.future.ISubscriptionIntermediateFuture;

/**
 *  Test creating components with run() and fetching results.
 */
public class ResultTest
{
	public static final long	TIMEOUT	= 10000;
	
	@Test
	public void	testCallable()
	{
		assertEquals("hello", IComponentManager.get().run(() -> "hello").get(TIMEOUT));
	}

	@Test
	public void	testCallableCopy()
	{
		List<String>	value	= Collections.singletonList("hello");
		List<String>	result	= IComponentManager.get().run(() -> value).get(TIMEOUT);
		assertEquals(value, result);
		assertNotSame(value, result);

		IComponentHandle	comp	= IComponentManager.get().create((Callable<Object>) () -> value).get(TIMEOUT);
		Object	res	= comp.subscribeToResults().getNextIntermediateResult(TIMEOUT).value();
		assertEquals(value, res);
		assertNotSame(value, res);
	}
	
	@Test
	public void	testCallableNoCopy()
	{
		List<String>	value	= Collections.singletonList("hello");
		Callable<List<String>>	call	= new Callable<>()
		{
			@Override
			public @NoCopy List<String> call() throws Exception
			{
				return value;
			}
		};
		List<String>	result	= IComponentManager.get().run(call).get(TIMEOUT);
		assertEquals(value, result);
		assertSame(value, result);

		IComponentHandle	comp	= IComponentManager.get().create(call).get(TIMEOUT);
		Object	res	= comp.subscribeToResults().getNextIntermediateResult(TIMEOUT).value();
		assertEquals(value, res);
		assertSame(value, res);
	}

	@Test
	public void	testFunction()
	{
		assertEquals("hello", IComponentManager.get().run(comp -> "hello").get(TIMEOUT));
	}

	@Test
	public void	testFunctionCopy()
	{
		List<String>	value	= Collections.singletonList("hello");
		List<String>	result	= IComponentManager.get().run(comp -> value).get(TIMEOUT);
		assertEquals(value, result);
		assertNotSame(value, result);
		
		IComponentHandle	comp	= IComponentManager.get().create((IThrowingFunction<IComponent, Object>) c -> value).get(TIMEOUT);
		Object	res	= comp.subscribeToResults().getNextIntermediateResult(TIMEOUT).value();
		assertEquals(value, res);
		assertNotSame(value, res);
	}
	
	@Test
	public void	testFunctionNoCopy()
	{
		List<String>	value	= Collections.singletonList("hello");
		IThrowingFunction<IComponent, List<String>>	func	= new IThrowingFunction<>()
		{
			@Override
			public @NoCopy List<String> apply(IComponent comp) throws Exception
			{
				return value;
			}
		};
		List<String>	result	= IComponentManager.get().run(func).get(TIMEOUT);
		assertEquals(value, result);
		assertSame(value, result);
		
		IComponentHandle	comp	= IComponentManager.get().create(func).get(TIMEOUT);
		Object	res	= comp.subscribeToResults().getNextIntermediateResult(TIMEOUT).value();
		assertEquals(value, res);
		assertSame(value, res);
	}
	
	@Test
	public void	testSubscriptionBeforeTerminate()
	{
		IComponentHandle	comp	= IComponentManager.get().create((Callable<String>)() -> "hello").get(TIMEOUT);
		ISubscriptionIntermediateFuture<ChangeEvent>	sub	= comp.subscribeToResults();
		assertEquals("hello", sub.getNextIntermediateResult(TIMEOUT).value());
		// Schedule step to check that component is still alive.
		comp.scheduleStep(() -> "world").get(TIMEOUT);
		comp.terminate().get(TIMEOUT);
		assertFalse(sub.hasNextIntermediateResult(TIMEOUT, true));
	}
	
	@Test
	public void	testSubscriptionAfterTerminate()
	{
		IComponentHandle	comp	= IComponentManager.get().create((Callable<String>)() -> "hello").get(TIMEOUT);
		comp.terminate().get(TIMEOUT);
		ISubscriptionIntermediateFuture<ChangeEvent>	sub	= comp.subscribeToResults();
		assertEquals("hello", sub.getNextIntermediateResult(TIMEOUT).value());
		assertFalse(sub.hasNextIntermediateResult(TIMEOUT, true));
	}
	
	@Test
	public void	testSubscriptionAfterFailure()
	{
		SUtil.runWithoutOutErr(() ->
		{
			@SuppressWarnings("serial")
			class TestException	extends RuntimeException {}
			
			IComponentHandle	comp	= IComponentManager.get().create((Callable<String>)() -> {throw new TestException();}).get(TIMEOUT);
			ISubscriptionIntermediateFuture<ChangeEvent>	sub	= comp.subscribeToResults();
			assertThrows(TestException.class, () -> sub.getNextIntermediateResult(TIMEOUT).value());
		});
	}
}
