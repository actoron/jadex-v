package jadex.injection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jadex.benchmark.BenchmarkHelper;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.impl.Component;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.injection.annotation.Inject;
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
		
		IComponentHandle	comp	= IComponentManager.get().create(new Object()
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
		}).get();
		
		// Check if start() is executed
		start.get(TIMEOUT);
		
		// Check if stop is correctly executed
		assertFalse(stop.isDone(), "stop() should not yet be executed.");
		comp.terminate().get();
		assertTrue(stop.isDone(), "stop() should have been executed.");
	}
	
	@Test
	public void testMethodParameters()
	{		
		// Check if component is injected
		Future<IComponent>	compfut	= new Future<>();
		IComponent	agent1	= Component.createComponent(Component.class, () -> new Component(new Object()
		{
			@OnStart
			public void	start(IComponent comp)
			{
				compfut.setResult(comp);
			}
		}));
		assertSame(agent1, compfut.get(TIMEOUT));
		
		// Check if execution feature is injected
		Future<IExecutionFeature>	exefut	= new Future<>();
		IComponent	agent2	= Component.createComponent(Component.class, () -> new Component(new Object()
		{
			@OnStart
			public void	start(IExecutionFeature exe)
			{
				exefut.setResult(exe);
			}
		}));
		assertSame(agent2.getFeature(IExecutionFeature.class), exefut.get(TIMEOUT));
	}
	
	@Test
	public void testFieldInjection()
	{		
		// Check if component is injected
		Future<IComponent>	compfut	= new Future<>();
		IComponent	agent1	= Component.createComponent(Component.class, () -> new Component(new Object()
		{
			@Inject
			IComponent comp;
			
			@OnStart
			public void	start()
			{
				compfut.setResult(comp);
			}
		}));
		assertSame(agent1, compfut.get(TIMEOUT));
		
		// Check if execution feature is injected
		Future<IExecutionFeature>	exefut	= new Future<>();
		IComponent	agent2	= Component.createComponent(Component.class, () -> new Component(new Object()
		{
			@Inject
			IExecutionFeature exe;
			
			@OnStart
			public void	start()
			{
				exefut.setResult(exe);
			}
		}));
		assertSame(agent2.getFeature(IExecutionFeature.class), exefut.get(TIMEOUT));
	}


	public static void main(String[] args)
	{
		BenchmarkHelper.benchmarkTime(() -> 
		{
			Future<Void>	ret	= new Future<>();
			IComponentHandle	comp	= IComponentManager.get().create(new Object()
			{
				@OnStart
				public void	start()
				{
					ret.setResult(null);
				}
			}).get();
			ret.get();
			comp.terminate().get();
		});
	}
}
