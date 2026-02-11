package jadex.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import jadex.common.TimeoutException;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ITerminableFuture;
import jadex.future.TerminableFuture;

public abstract class AbstractExecutionFeatureTest
{
	// Timeout how long a test blocks on a future before giving up.
	// Does not affect test execution time for successful tests.
	protected long	TIMEOUT	= 10000;
	
	@Test
	public void	testFeatureAccess()
	{
		// Test calling from outside thread
		assertThrows(IllegalCallerException.class, () -> IExecutionFeature.get());
		IComponentHandle comp	= IComponentManager.get().create(null).get(TIMEOUT);
		assertThrows(IllegalCallerException.class, () -> IExecutionFeature.get());
		
		// Test calling from inside thread
		IFuture<IExecutionFeature>	fut	= comp.scheduleStep(
			() -> IExecutionFeature.get());
		IExecutionFeature exe = comp.scheduleStep(c->{return c.getFeature(IExecutionFeature.class);}).get(TIMEOUT);
		assertEquals(exe, fut.get(TIMEOUT));
	}
	
	@Test
	public void	testGetComponent()
	{
		// Inside creation does not work anymore, because init() is now called after constructor instead of inside it.
//		// Test inside creation
//		Future<IComponent>	fut	= new Future<>();
//		IComponentHandle comp	= Component.createComponent(Component.class,
//			() -> new Component(this)
//		{
//			{
//				fut.setResult(IExecutionFeature.get().getComponent());
//			}
//		}).get(TIMEOUT);
//		IComponent	icomp	= comp.scheduleStep(c->{return c;}).get(TIMEOUT);
//		assertEquals(icomp, fut.get(TIMEOUT));
		
		// Test after creation
		IComponentHandle comp	= IComponentManager.get().create(null).get(TIMEOUT);
		IFuture<IComponent> result	= comp.scheduleStep(
			() -> IExecutionFeature.get().getComponent());
		IComponent	icomp	= comp.scheduleStep(c->{return c;}).get(TIMEOUT);
		assertEquals(icomp, result.get(TIMEOUT));
				
		// Test after extra component creation
		IComponentHandle comp2	= IComponentManager.get().create(null).get(TIMEOUT);
		IComponent	icomp2	= comp2.scheduleStep(c->{return c;}).get(TIMEOUT);
		result	= comp.scheduleStep(
				() -> IExecutionFeature.get().getComponent());
		IFuture<IComponent> result2	= comp2.scheduleStep(
				() -> IExecutionFeature.get().getComponent());
		assertEquals(icomp, result.get(TIMEOUT));
		assertEquals(icomp2, result2.get(TIMEOUT));
		
		// Test after creation inside component
		IComponentHandle comp3	= comp.scheduleAsyncStep(
			() -> IComponentManager.get().create(null)).get(TIMEOUT);
		IComponent	icomp3	= comp3.scheduleStep(c->{return c;}).get(TIMEOUT);
		result	= comp.scheduleStep(
				() -> IExecutionFeature.get().getComponent());
		IFuture<IComponent> result3	= comp3.scheduleStep(
				() -> IExecutionFeature.get().getComponent());
		assertEquals(icomp, result.get(TIMEOUT));
		assertEquals(icomp3, result3.get(TIMEOUT));
		
		// Test plain creation (w/o bootstrap) inside component
		Component icomp4	= comp.scheduleStep(() ->
		{
			Component ret	= new Component(this, null, null);
			ret.init();
			return ret;
		}).get(TIMEOUT);
		result	= comp.scheduleStep(
				() -> IExecutionFeature.get().getComponent());
		IFuture<IComponent> result4	= icomp4.getComponentHandle().scheduleStep(
				() -> IExecutionFeature.get().getComponent());
		assertEquals(icomp, result.get(TIMEOUT));
		assertEquals(icomp4, result4.get(TIMEOUT));
	}
	
	@Test
	public void	testFireAndForgetStep()
	{
		IComponentHandle comp	= IComponentManager.get().create(null).get(TIMEOUT);
		// Test executing a simple fire-and-forget step (Runnable implementation).
		Future<Boolean>	result	= new Future<>();
		Runnable	step	= () -> result.setResult(true);
		comp.scheduleStep(step).get(TIMEOUT);
		assertTrue(result.get(TIMEOUT), "Wrong step result.");
	}
	
	@Test
	public void	testResultStep()
	{
		IComponentHandle comp	= IComponentManager.get().create(null).get(TIMEOUT);
		// Test executing a simple result step (Supplier implementation).
		IFuture<Boolean>	result	= comp.scheduleStep(() -> true);
		assertTrue(result.get(TIMEOUT), "Wrong step result.");
	}
	
	@Test
	public void	testExceptionStep()
	{
		IComponentHandle comp	= IComponentManager.get().create(null).get(TIMEOUT);
		// Test executing a simple result step (Supplier implementation).
		IFuture<Boolean>	result	= comp.scheduleStep(() -> {
			throw new IllegalCallerException("ex");
		});
		assertThrows(IllegalCallerException.class, () -> result.get(TIMEOUT), "Wrong step exception.");
	}
	
	@Test
	public void	testErrorStep()
	{
		IComponentHandle comp	= IComponentManager.get().create(null).get(TIMEOUT);
		// Test executing a simple result step (Supplier implementation).
		IFuture<Boolean>	result	= comp.scheduleStep(() -> {
			throw new InternalError("err");
		});
		assertThrows(RuntimeException.class, () -> result.get(TIMEOUT), "Wrong step error.");
	}
	
	@Test
	public void	testBlockingStep()
	{
		IComponentHandle comp	= IComponentManager.get().create(null).get(TIMEOUT);
		Future<Boolean>	blocker	= new Future<>();
		IFuture<Boolean>	result	= comp.scheduleStep(() -> {
			return blocker.get();
		});
		// Test that blocked step can be woken up by another step,
		// i.e., that a new thread was used to wake up the blocked thread.
		comp.scheduleStep(() -> blocker.setResult(true)).get(TIMEOUT);
		assertTrue(result.get(TIMEOUT), "Wrong step result.");
	}
	
	@Test
	public void	testTwoBlockingSteps()
	{
		IComponentHandle comp	= IComponentManager.get().create(null).get(TIMEOUT);
		// Test that two steps can wait for same result.
		Future<Boolean>	blocker	= new Future<>();
		IFuture<Boolean>	result1	= comp.scheduleStep(() -> blocker.get());
		IFuture<Boolean>	result2	= comp.scheduleStep(() -> blocker.get());
		comp.scheduleStep(() -> blocker.setResult(true)).get(TIMEOUT);
		assertTrue(result1.get(TIMEOUT), "Wrong step result.");
		assertTrue(result2.get(TIMEOUT), "Wrong step result.");
	}
	
	@Test
	public void	testIsComponentThread()
	{
		// Test without component
		assertThrows(IllegalCallerException.class, () -> IExecutionFeature.get());
		
		// Test that the component thread is already detected during bootstrapping
		Future<Boolean>	bootstrap1	= new Future<>();
		IComponentHandle comp	= Component.createComponent(new Component(this, null, null)
		{
			@Override
			public void init()
			{
				// Test during component init
				bootstrap1.setResult(IExecutionFeature.get().isComponentThread());
				super.init();
			}
		}).get(TIMEOUT);
		IExecutionFeature exe = comp.scheduleStep(c->{return c.getFeature(IExecutionFeature.class);}).get(TIMEOUT);
		assertTrue(bootstrap1.get(TIMEOUT));
		
		// Test during normal component operation.
		assertFalse(exe.isComponentThread());
		IFuture<Boolean>	instep	= comp.scheduleStep(()
			-> IExecutionFeature.get().isComponentThread());
		assertTrue(instep.get(TIMEOUT));
		
		// Test across two components
		IComponentHandle comp2	= IComponentManager.get().create(null).get(TIMEOUT);
		IFuture<Boolean>	othercomp	= comp2.scheduleStep(() ->	exe.isComponentThread());
		assertFalse(othercomp.get(TIMEOUT));
	}
	
	@Test
	public void	testIsAnyComponentThread()
	{
		// Test without component
		assertFalse(IExecutionFeature.isAnyComponentThread());
		
		// Test that the component thread is already detected during bootstrapping
		Future<Boolean>	bootstrap1	= new Future<>();
		IComponentHandle comp	= Component.createComponent(new Component(this, null, null)
		{
			@Override
			public void init()
			{
				// Test during component init
				bootstrap1.setResult(IExecutionFeature.isAnyComponentThread());
				super.init();
			}
		}).get(TIMEOUT);
		assertTrue(bootstrap1.get(TIMEOUT));
		
		// Test during normal component operation.
		IFuture<Boolean>	instep	= comp.scheduleStep(()
			-> IExecutionFeature.isAnyComponentThread());
		assertTrue(instep.get(TIMEOUT));
	}
	
	@Test
	public void	testBootstrapping()
	{
		// Test that component creation is scheduled on different thread.
		Thread outer	= Thread.currentThread();
		Thread[] inner	= new Thread[2];
		IComponentHandle comp	= Component.createComponent(new Component(this, null, null)
		{
			@Override
			public void init()
			{
				// Test during component init
				inner[0]	= Thread.currentThread();
				super.init();
			}
		}).get(TIMEOUT);
		assertNotEquals(outer, inner[0], "Failed to switch threads.");
				
		// Test that component creation inside component is scheduled on different thread.
		comp.scheduleStep(() ->
		{
			inner[0]	= Thread.currentThread();
			Component.createComponent(new Component(this, null, null)
			{
				@Override
				public void init()
				{
					// Test during component init
					inner[1]	= Thread.currentThread();
					super.init();
				}
			}).get(TIMEOUT);
			return (Void)null;
		}).get(TIMEOUT);
		// Test that component creation was scheduled on different thread.
		assertNotEquals(inner[0], inner[1], "Failed to switch threads.");
	}
	
	// thread resuse test is difficult due to race conditions
	/*@Test
	public void	testThreadReuse()
	{
		IComponentHandle comp	= IComponentManager.get().create(null);
		Thread[]	current	= new Thread[1];
		@SuppressWarnings("unchecked")
		IFuture<Boolean>[]	steps	= new IFuture[Runtime.getRuntime().availableProcessors()];
		for(int i=0; i<steps.length; i++)
		{
			steps[i]	= comp.getExternalAccess().scheduleStep(() ->
			{
				// Check if step reuses thread of previous step (if any).
				boolean ret	= current[0]==null || current[0]==Thread.currentThread();
				// Remember thread of this step
				current[0]	= Thread.currentThread();
				return ret;
			});
		}
		// Count how often a thread was reused.
		int	reused	= 0;
		for(int i=0; i<steps.length; i++)
		{
			reused	+= steps[i].get(TIMEOUT) ? 1 : 0;
		}
		// Check that thread is reused at least for every other step.
		// Might not get reused for all steps due to race condition between
		// - step execution (thread is released if step finishes and step queue is empty)
		// - scheduling of next step (new thread is acquired, if necessary)
		assertTrue(reused>steps.length/2, "Thread reuse only for "+reused+" of "+steps.length+" steps.");
	}*/
	
	@Test
	public void	testStepOrdering()
	{
		IComponentHandle comp	= IComponentManager.get().create(null).get(TIMEOUT);
		AtomicInteger	num	= new AtomicInteger(0);
		@SuppressWarnings("unchecked")
		IFuture<Boolean>[]	steps	= new IFuture[Runtime.getRuntime().availableProcessors()];
		for(int i=0; i<steps.length; i++)
		{
			int	mynum	= i;
			steps[i]	= comp.scheduleStep(() -> num.getAndIncrement()==mynum);
		}
		// Collect results from all steps.
		for(int i=0; i<steps.length; i++)
		{
			assertTrue(steps[i].get(TIMEOUT), "Step not executed in order "+i+".");
		}
	}
	
	@Test
	public void	testDoubleExecution()
	{
		IComponentHandle comp	= IComponentManager.get().create(null).get(TIMEOUT);
		AtomicInteger	numthreads	= new AtomicInteger(0);
		Future<Void>	blocker	= new Future<>();
		@SuppressWarnings("unchecked")
		IFuture<Boolean>[]	steps	= new IFuture[Runtime.getRuntime().availableProcessors()];
		for(int i=0; i<steps.length; i++)
		{
			steps[i]	= comp.scheduleStep(() ->
			{
				if(numthreads.incrementAndGet()>1)
				{
					return false;
				}
				
				try{ Thread.sleep((long)(20*Math.random())); }
				catch(InterruptedException e){}
				
				if(numthreads.decrementAndGet()>0)
				{
					return false;
				}
				blocker.get();
				if(numthreads.incrementAndGet()>1)
				{
					return false;
				}
				
				try{ Thread.sleep((long)(20*Math.random())); }
				catch(InterruptedException e){}
				
				return numthreads.decrementAndGet()==0;
			});
		}

		// Wait for threads to block and then wake up all at once
		try{ Thread.sleep(20*steps.length); }
		catch(InterruptedException e){}
		blocker.setResult(null);

		// Collect results from all steps.
		for(int i=0; i<steps.length; i++)
		{
			assertTrue(steps[i].get(TIMEOUT), "Double execution detected.");
		}
	}
	
	@Test
	public void	testWaitForDelay()
	{
		IComponentHandle comp	= IComponentManager.get().create(null).get(TIMEOUT);
		IFuture<Boolean>	test	= comp.scheduleStep(() ->
		{
			long	wait	= 50;
			long before	= IExecutionFeature.get().getTime();
			IExecutionFeature.get().waitForDelay(wait).get(TIMEOUT);
			long after	= IExecutionFeature.get().getTime();
			return after >= before+wait;
		});
		assertTrue(test.get(TIMEOUT), "Not enough time has passed.");
	}
	
	@Test
	public void	testExternalWaitForDelay()
	{
		IComponentHandle comp	= IComponentManager.get().create(null).get(TIMEOUT);
		long	wait	= 50;
		long before	= comp.scheduleStep(
			() -> IExecutionFeature.get().getTime()).get(TIMEOUT);
		IExecutionFeature exe = comp.scheduleStep(c->{return c.getFeature(IExecutionFeature.class);}).get(TIMEOUT);
		exe.waitForDelay(wait).get(TIMEOUT);
		long after	= comp.scheduleStep(
			() -> IExecutionFeature.get().getTime()).get(TIMEOUT);
		assertTrue(after >= before+wait, "Not enough time has passed.");
	}

	@Test
	public void	testTimeout()
	{
		IComponentHandle comp	= IComponentManager.get().create(null).get(TIMEOUT);
		long	wait	= 50;
		long before	= comp.scheduleStep(
			() -> IExecutionFeature.get().getTime()).get(TIMEOUT);
		assertThrows(TimeoutException.class, () -> comp.scheduleStep(()
			-> new Future<>().get(wait)).get(TIMEOUT));
		long after	= comp.scheduleStep(
			() -> IExecutionFeature.get().getTime()).get(TIMEOUT);
		assertTrue(after >= before+wait, "Not enough time has passed.");
	}

	@Test
	public void	testSyncResultSchedulingCallable() throws Exception
	{
		// Check that future result is scheduled on caller thread, if any.
		
		// Call from test -> check for global runner
		IComponentHandle agent = IComponentManager.get().create(null).get();
		Future<Void>	resfut	= new Future<>();
		IFuture<Void>	iresfut	= agent.scheduleStep(() -> 
		{
			resfut.get();
			return null;
		});
		Future<ComponentIdentifier>	compfut	= new Future<>();
		iresfut.then(res -> compfut.setResult(IComponentManager.get().getCurrentComponent().getId()));
		agent.scheduleStep(() -> resfut.setResult(null)).get(TIMEOUT);
		assertEquals(ComponentManager.get().getGlobalRunner().getId(), compfut.get(TIMEOUT));

		// Call from other agent -> check for caller agent
		IComponentHandle agent2 = IComponentManager.get().create(null).get();
		Future<Void>	resfut2	= new Future<>();
		Future<ComponentIdentifier>	compfut2	= new Future<>();
		agent2.scheduleStep(() ->
		{
			IFuture<Void>	iresfut2	= agent.scheduleStep(() -> 
			{
				resfut2.get();
				return null;
			});
			iresfut2.then(res -> compfut2.setResult(IComponentManager.get().getCurrentComponent().getId()));
		}).get(TIMEOUT);
		agent.scheduleStep(() -> resfut2.setResult(null)).get(TIMEOUT);
		assertEquals(agent2.getId(), compfut2.get(TIMEOUT));
		
		// Call from swing -> check for swing thread
		Future<Void>	resfut3	= new Future<>();
		Future<Boolean>	swingfut	= new Future<>();
		SwingUtilities.invokeAndWait(() ->
		{
			IFuture<Void>	iresfut3	= agent.scheduleStep(() -> 
			{
				resfut3.get();
				return null;
			});
			iresfut3.then(res -> swingfut.setResult(SwingUtilities.isEventDispatchThread()));
		});
		agent.scheduleStep(() -> resfut3.setResult(null)).get(TIMEOUT);
		assertTrue(swingfut.get(TIMEOUT));
	}
	
	@Test
	public void	testSyncResultSchedulingFunction() throws Exception
	{
		// Check that future result is scheduled on caller thread, if any.
		
		// Call from test -> check for global runner
		IComponentHandle agent = IComponentManager.get().create(null).get();
		Future<Void>	resfut	= new Future<>();
		IFuture<Void>	iresfut	= agent.scheduleStep(comp -> 
		{
			resfut.get();
			return null;
		});
		Future<ComponentIdentifier>	compfut	= new Future<>();
		iresfut.then(res -> compfut.setResult(IComponentManager.get().getCurrentComponent().getId()));
		agent.scheduleStep(() -> resfut.setResult(null)).get(TIMEOUT);
		assertEquals(ComponentManager.get().getGlobalRunner().getId(), compfut.get(TIMEOUT));

		// Call from other agent -> check for caller agent
		IComponentHandle agent2 = IComponentManager.get().create(null).get();
		Future<Void>	resfut2	= new Future<>();
		Future<ComponentIdentifier>	compfut2	= new Future<>();
		agent2.scheduleStep(() ->
		{
			IFuture<Void>	iresfut2	= agent.scheduleStep(comp -> 
			{
				resfut2.get();
				return null;
			});
			iresfut2.then(res -> compfut2.setResult(IComponentManager.get().getCurrentComponent().getId()));
		}).get(TIMEOUT);
		agent.scheduleStep(() -> resfut2.setResult(null)).get(TIMEOUT);
		assertEquals(agent2.getId(), compfut2.get(TIMEOUT));
		
		// Call from swing -> check for swing thread
		Future<Void>	resfut3	= new Future<>();
		Future<Boolean>	swingfut	= new Future<>();
		SwingUtilities.invokeAndWait(() ->
		{
			IFuture<Void>	iresfut3	= agent.scheduleStep(comp -> 
			{
				resfut3.get();
				return null;
			});
			iresfut3.then(res -> swingfut.setResult(SwingUtilities.isEventDispatchThread()));
		});
		agent.scheduleStep(() -> resfut3.setResult(null)).get(TIMEOUT);
		assertTrue(swingfut.get(TIMEOUT));
	}
	
	@Test
	public void	testAsyncResultSchedulingCallable() throws Exception
	{
		// Check that future result is scheduled on caller thread, if any.
		
		// Call from test -> check for global runner
		IComponentHandle agent = IComponentManager.get().create(null).get();
		Future<Void>	resfut	= new Future<>();
		IFuture<Void>	iresfut	= agent.scheduleAsyncStep(() -> resfut); 
		Future<ComponentIdentifier>	compfut	= new Future<>();
		iresfut.then(res -> compfut.setResult(IComponentManager.get().getCurrentComponent().getId()));
		agent.scheduleStep(() -> resfut.setResult(null)).get(TIMEOUT);
		assertEquals(ComponentManager.get().getGlobalRunner().getId(), compfut.get(TIMEOUT));
		
		// Call from other agent -> check for caller agent
		IComponentHandle agent2 = IComponentManager.get().create(null).get();
		Future<Void>	resfut2	= new Future<>();
		Future<ComponentIdentifier>	compfut2	= new Future<>();
		agent2.scheduleStep(() ->
		{
			IFuture<Void>	iresfut2	= agent.scheduleAsyncStep(() -> resfut2); 
			iresfut2.then(res -> compfut2.setResult(IComponentManager.get().getCurrentComponent().getId()));
		}).get(TIMEOUT);
		agent.scheduleStep(() -> resfut2.setResult(null)).get(TIMEOUT);
		assertEquals(agent2.getId(), compfut2.get(TIMEOUT));
		
		// Call from swing -> check for swing thread
		Future<Void>	resfut3	= new Future<>();
		Future<Boolean>	swingfut	= new Future<>();
		SwingUtilities.invokeAndWait(() ->
		{
			IFuture<Void>	iresfut3	= agent.scheduleAsyncStep(() -> resfut3); 
			iresfut3.then(res -> swingfut.setResult(SwingUtilities.isEventDispatchThread()));
		});
		agent.scheduleStep(() -> resfut3.setResult(null)).get(TIMEOUT);
		assertTrue(swingfut.get(TIMEOUT));
	}
	
	@Test
	public void	testAsyncResultSchedulingFunction() throws Exception
	{
		// Check that future result is scheduled on caller thread, if any.
		
		// Call from test -> check for global runner
		IComponentHandle agent = IComponentManager.get().create(null).get();
		Future<Void>	resfut	= new Future<>();
		IFuture<Void>	iresfut	= agent.scheduleAsyncStep(comp -> resfut); 
		Future<ComponentIdentifier>	compfut	= new Future<>();
		iresfut.then(res -> compfut.setResult(IComponentManager.get().getCurrentComponent().getId()));
		agent.scheduleStep(() -> resfut.setResult(null)).get(TIMEOUT);
		assertEquals(ComponentManager.get().getGlobalRunner().getId(), compfut.get(TIMEOUT));
		
		// Call from other agent -> check for caller agent
		IComponentHandle agent2 = IComponentManager.get().create(null).get();
		Future<Void>	resfut2	= new Future<>();
		Future<ComponentIdentifier>	compfut2	= new Future<>();
		agent2.scheduleStep(() ->
		{
			IFuture<Void>	iresfut2	= agent.scheduleAsyncStep(comp -> resfut2); 
			iresfut2.then(res -> compfut2.setResult(IComponentManager.get().getCurrentComponent().getId()));
		}).get(TIMEOUT);
		agent.scheduleStep(() -> resfut2.setResult(null)).get(TIMEOUT);
		assertEquals(agent2.getId(), compfut2.get(TIMEOUT));
		
		// Call from swing -> check for swing thread
		Future<Void>	resfut3	= new Future<>();
		Future<Boolean>	swingfut	= new Future<>();
		SwingUtilities.invokeAndWait(() ->
		{
			IFuture<Void>	iresfut3	= agent.scheduleAsyncStep(comp -> resfut3); 
			iresfut3.then(res -> swingfut.setResult(SwingUtilities.isEventDispatchThread()));
		});
		agent.scheduleStep(() -> resfut3.setResult(null)).get(TIMEOUT);
		assertTrue(swingfut.get(TIMEOUT));
	}
	
	@Test
	public void	testTerminationScheduling()
	{
		// Check for termination command executed on component thread. 
		Future<ComponentIdentifier>	compfut	= new Future<>();
		IComponentHandle agent = IComponentManager.get().create(null).get();
		ITerminableFuture<Object>	termfut	= agent.scheduleAsyncStep(new Callable<ITerminableFuture<Object>>()
		{
			@Override
			public ITerminableFuture<Object> call() throws Exception
			{
				return new TerminableFuture<>(ex ->
					compfut.setResult(IComponentManager.get().getCurrentComponent().getId()));
			}
		});
		// Step to make sure inner future is created and connected to delegation future before terminate().
		agent.scheduleStep(() -> null).get(TIMEOUT);
		termfut.terminate();
		assertEquals(agent.getId(), compfut.get(TIMEOUT));
	}
}
