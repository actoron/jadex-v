package jadex.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.core.IThrowingFunction;
import jadex.core.annotation.NoCopy;

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
	}
}
