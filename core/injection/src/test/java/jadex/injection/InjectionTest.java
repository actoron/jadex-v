package jadex.injection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jadex.benchmark.BenchmarkHelper;
import jadex.core.IComponent;
import jadex.core.impl.Component;
import jadex.future.Future;
import jadex.injection.annotation.OnEnd;
import jadex.injection.annotation.OnStart;

public class InjectionTest
{
	protected static final long	TIMEOUT	= 10000;
	
	@Test
	public void testOnStartEnd()
	{
		Future<Void>	start	= new Future<>();
		Future<Void>	stop	= new Future<>();
		
		IComponent	agent	= Component.createComponent(Component.class, () -> new Component(new Object()
		{
			@OnStart
			public void	start()
			{
				start.setResult(null);
			}
			
			@OnEnd
			public void	stop()
			{
				stop.setResult(null);
			}

		}));
		
		// Check if start() is executed
		start.get(TIMEOUT);
		
		// Check if stop is correctly executed
		assertFalse(stop.isDone(), "stop() should not yet be executed.");
		agent.getComponentHandle().terminate().get();
		assertTrue(stop.isDone(), "stop() should have been executed.");
	}

	public static void main(String[] args)
	{
		BenchmarkHelper.benchmarkTime(() -> 
		{
			Future<Void>	ret	= new Future<>();
			IComponent	agent	= Component.createComponent(Component.class, () -> new Component(new Object()
			{
				@OnStart
				public void	start()
				{
					ret.setResult(null);
				}
			}));
			ret.get();
			agent.terminate().get();
		});
	}
}
