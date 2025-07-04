package jadex.injection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadex.benchmark.BenchmarkHelper;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnEnd;
import jadex.injection.annotation.OnStart;
import jadex.injection.impl.IInjectionHandle;
import jadex.injection.impl.InjectionModel;

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
		IComponent	agent1	= IComponentManager.get().create(new Object()
		{
			@OnStart
			public void	start(IComponent comp)
			{
				compfut.setResult(comp);
			}
		}).get(TIMEOUT).scheduleStep(comp -> comp).get(TIMEOUT);
		assertSame(agent1, compfut.get(TIMEOUT));
		
		// Check if execution feature is injected
		Future<IExecutionFeature>	exefut	= new Future<>();
		IComponent	agent2	= IComponentManager.get().create(new Object()
		{
			@OnStart
			public void	start(IExecutionFeature exe)
			{
				exefut.setResult(exe);
			}
		}).get(TIMEOUT).scheduleStep(comp -> comp).get(TIMEOUT);
		assertSame(agent2.getFeature(IExecutionFeature.class), exefut.get(TIMEOUT));
	}
	
	@Test
	public void testFieldInjection()
	{		
		// Check if component is injected
		Future<IComponent>	compfut	= new Future<>();
		IComponent	agent1	= IComponentManager.get().create(new Object()
		{
			@Inject
			IComponent comp;
			
			@OnStart
			public void	start()
			{
				compfut.setResult(comp);
			}
		}).get(TIMEOUT).scheduleStep(comp -> comp).get(TIMEOUT);
		assertSame(agent1, compfut.get(TIMEOUT));
		
		// Check if execution feature is injected
		Future<IExecutionFeature>	exefut	= new Future<>();
		IComponent	agent2	= IComponentManager.get().create(new Object()
		{
			@Inject
			IExecutionFeature exe;
			
			@OnStart
			public void	start()
			{
				exefut.setResult(exe);
			}
		}).get(TIMEOUT).scheduleStep(comp -> comp).get(TIMEOUT);
		assertSame(agent2.getFeature(IExecutionFeature.class), exefut.get(TIMEOUT));
	}

	@Test
	public void	testSuperclass()
	{
		List<String>	invocations	= new ArrayList<>();
		class Baseclass
		{
			@OnStart
			void start1()
			{
				invocations.add("s1");
			}
			@OnEnd
			void end1()
			{
				invocations.add("e1");
			}
		}
		class Subclass	extends Baseclass
		{
			@OnStart
			void start2()
			{
				invocations.add("s2");
			}
			// Override to check if it is invoked twice, because it is declared twice.
			@OnEnd
			void end1()
			{
				super.end1();
			}

		}
		
		IComponentHandle	handle	= IComponentManager.get().create(new Subclass()).get();
		// schedule check as onStart runs on extra step.
		handle.scheduleStep(() ->
		{
			assertEquals("[s1, s2]", invocations.toString());
			return null;
		}).get(TIMEOUT);
		
		handle.terminate().get();
		assertEquals("[s1, s2, e1]", invocations.toString());
	}
	
	
	@Test
	public void	testBlockingStart()
	{
		// Also tests method injection.
		InjectionModel.addMethodInjection((pojotypes, method, contextfetchers) ->
		{
			if(method.getName().equals("injectMe"))
			{
				List<IInjectionHandle>	preparams	= new ArrayList<>();
				preparams.add((self, pojos, context, oldval) -> context);
				IInjectionHandle	invocation	= InjectionModel.createMethodInvocation(method, pojotypes, contextfetchers, preparams);
				return (comp, pojos, context, oldvale) ->
				{
					comp.getFeature(IExecutionFeature.class).waitForDelay(500).then(v ->
						invocation.apply(comp, pojos, "value", null));
					return null;
				};
			}
			else
			{
				return null;
			}
		}, Inject.class);
		
		Future<String>	injected	= new Future<>();
		IComponentManager.get().create(new Object()
		{
			@OnStart
			void	block()
			{
				new Future<Void>().get();
			}
			
			@Inject
			void injectMe(String value)
			{
				injected.setResult(value);
			}
		}).get(TIMEOUT);
		
		assertEquals("value", injected.get(TIMEOUT));
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
