package jadex.execution.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import jadex.core.IComponent;
import jadex.core.impl.Component;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;

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
		Component	comp	= Component.createComponent(Component.class, () -> new Component(this));
		assertThrows(IllegalCallerException.class, () -> IExecutionFeature.get());
		
		// Test calling from inside thread
		IFuture<IExecutionFeature>	fut	= comp.getComponentHandle().scheduleStep(
			() -> IExecutionFeature.get());
		assertEquals(comp.getFeature(IExecutionFeature.class), fut.get(TIMEOUT));
	}
	
	@Test
	public void	testGetComponent()
	{
		// Test inside creation
		Future<IComponent>	fut	= new Future<>();
		IComponent	comp	= Component.createComponent(Component.class,
			() -> new Component(this)
		{
			{
				fut.setResult(IExecutionFeature.get().getComponent());
			}
		});
		assertEquals(comp, fut.get(TIMEOUT));
		
		// Test after creation
		IFuture<IComponent> result	= comp.getComponentHandle().scheduleStep(
			() -> IExecutionFeature.get().getComponent());
		assertEquals(comp, result.get(TIMEOUT));
				
		// Test after extra component creation
		Component	comp2	= Component.createComponent(Component.class, () -> new Component(this));
		result	= comp.getComponentHandle().scheduleStep(
				() -> IExecutionFeature.get().getComponent());
		IFuture<IComponent> result2	= comp2.getComponentHandle().scheduleStep(
				() -> IExecutionFeature.get().getComponent());
		assertEquals(comp, result.get(TIMEOUT));
		assertEquals(comp2, result2.get(TIMEOUT));
		
		// Test after creation inside component
		Component	comp3	= comp.getComponentHandle().scheduleStep(
			() -> Component.createComponent(Component.class, () -> new Component(this))).get(TIMEOUT);
		result	= comp.getComponentHandle().scheduleStep(
				() -> IExecutionFeature.get().getComponent());
		IFuture<IComponent> result3	= comp3.getComponentHandle().scheduleStep(
				() -> IExecutionFeature.get().getComponent());
		assertEquals(comp, result.get(TIMEOUT));
		assertEquals(comp3, result3.get(TIMEOUT));
		
		// Test plain creation (w/o bootstrap) inside component
		Component	comp4	= comp.getComponentHandle().scheduleStep(
		() -> new Component(this){}).get(TIMEOUT);
		result	= comp.getComponentHandle().scheduleStep(
				() -> IExecutionFeature.get().getComponent());
		IFuture<IComponent> result4	= comp4.getComponentHandle().scheduleStep(
				() -> IExecutionFeature.get().getComponent());
		assertEquals(comp, result.get(TIMEOUT));
		assertEquals(comp4, result4.get(TIMEOUT));
	}
	
	@Test
	public void	testFireAndForgetStep()
	{
		Component	comp	= Component.createComponent(Component.class, () -> new Component(this));
		// Test executing a simple fire-and-forget step (Runnable implementation).
		Future<Boolean>	result	= new Future<>();
		Runnable	step	= () -> result.setResult(true);
		comp.getComponentHandle().scheduleStep(step);
		assertTrue(result.get(TIMEOUT), "Wrong step result.");
	}
	
	@Test
	public void	testResultStep()
	{
		Component	comp	= Component.createComponent(Component.class, () -> new Component(this));
		// Test executing a simple result step (Supplier implementation).
		IFuture<Boolean>	result	= comp.getComponentHandle().scheduleStep(() -> true);
		assertTrue(result.get(TIMEOUT), "Wrong step result.");
	}
	
	@Test
	public void	testExceptionStep()
	{
		Component	comp	= Component.createComponent(Component.class, () -> new Component(this));
		// Test executing a simple result step (Supplier implementation).
		IFuture<Boolean>	result	= comp.getComponentHandle().scheduleStep(() -> {
			throw new IllegalCallerException("ex");
		});
		assertThrows(IllegalCallerException.class, () -> result.get(TIMEOUT), "Wrong step exception.");
	}
	
	@Test
	public void	testErrorStep()
	{
		Component	comp	= Component.createComponent(Component.class, () -> new Component(this));
		// Test executing a simple result step (Supplier implementation).
		IFuture<Boolean>	result	= comp.getComponentHandle().scheduleStep(() -> {
			throw new InternalError("err");
		});
		assertThrows(RuntimeException.class, () -> result.get(TIMEOUT), "Wrong step error.");
	}
	
	@Test
	public void	testBlockingStep()
	{
		Component	comp	= Component.createComponent(Component.class, () -> new Component(this));
		Future<Boolean>	blocker	= new Future<>();
		IFuture<Boolean>	result	= comp.getComponentHandle().scheduleStep(() -> {
			return blocker.get();
		});
		// Test that blocked step can be woken up by another step,
		// i.e., that a new thread was used to wake up the blocked thread.
		comp.getComponentHandle().scheduleStep(() -> blocker.setResult(true));
		assertTrue(result.get(TIMEOUT), "Wrong step result.");
	}
	
	@Test
	public void	testTwoBlockingSteps()
	{
		Component	comp	= Component.createComponent(Component.class, () -> new Component(this));
		// Test that two steps can wait for same result.
		Future<Boolean>	blocker	= new Future<>();
		IFuture<Boolean>	result1	= comp.getComponentHandle().scheduleStep(() -> blocker.get());
		IFuture<Boolean>	result2	= comp.getComponentHandle().scheduleStep(() -> blocker.get());
		comp.getComponentHandle().scheduleStep(() -> blocker.setResult(true));
		assertTrue(result1.get(TIMEOUT), "Wrong step result.");
		assertTrue(result2.get(TIMEOUT), "Wrong step result.");
	}
	
	@Test
	public void	testIsComponentThread()
	{
		// Test without component
		assertThrows(IllegalCallerException.class, () -> IExecutionFeature.get());
		
		// Test that the component thread is already detected during bootstrapping
		Future<Boolean>	bootstrap0	= new Future<>();
		Future<Boolean>	bootstrap1	= new Future<>();
		Component	comp	= Component.createComponent(Component.class, () ->
		{
			// Test before component creation
			bootstrap0.setResult(ExecutionFeature.LOCAL.get().isComponentThread());
			
			return new Component(this)
			{
				{
					// Test during component creation
					bootstrap1.setResult(IExecutionFeature.get().isComponentThread());
				}
			};
		});
		assertTrue(bootstrap0.get(TIMEOUT));
		assertTrue(bootstrap1.get(TIMEOUT));
		
		// Test during normal component operation.
		assertFalse(comp.getFeature(IExecutionFeature.class).isComponentThread());
		IFuture<Boolean>	instep	= comp.getComponentHandle().scheduleStep(()
			-> IExecutionFeature.get().isComponentThread());
		assertTrue(instep.get(TIMEOUT));
		
		// Test across two components
		Component	comp2	= Component.createComponent(Component.class, () -> new Component(this));
		IFuture<Boolean>	othercomp	= comp2.getComponentHandle().scheduleStep(()
				-> comp.getFeature(IExecutionFeature.class).isComponentThread());
		assertFalse(othercomp.get(TIMEOUT));
	}
	
	@Test
	public void	testIsAnyComponentThread()
	{
		// Test without component
		assertFalse(IExecutionFeature.isAnyComponentThread());
		
		// Test that the component thread is already detected during bootstrapping
		Future<Boolean>	bootstrap0	= new Future<>();
		Future<Boolean>	bootstrap1	= new Future<>();
		Component	comp	= Component.createComponent(Component.class, () ->
		{
			// Test before component creation
			bootstrap0.setResult(IExecutionFeature.isAnyComponentThread());
			
			return new Component(this)
			{
				{
					// Test during component creation
					bootstrap1.setResult(IExecutionFeature.isAnyComponentThread());
				}
			};
		});
		assertTrue(bootstrap0.get(TIMEOUT));
		assertTrue(bootstrap1.get(TIMEOUT));
		
		// Test during normal component operation.
		IFuture<Boolean>	instep	= comp.getComponentHandle().scheduleStep(()
			-> IExecutionFeature.isAnyComponentThread());
		assertTrue(instep.get(TIMEOUT));
	}
	
	@Test
	public void	testBootstrapping()
	{
		// Test that component creation is scheduled on different thread.
		Thread outer	= Thread.currentThread();
		Thread[] inner	= new Thread[2];
		Component	comp	= Component.createComponent(Component.class, () -> new Component(this)
		{
			{
				inner[0]	= Thread.currentThread();
			}
		});
		assertNotEquals(outer, inner[0], "Failed to switch threads.");
				
		// Test that component creation inside component is scheduled on different thread.
		comp.getComponentHandle().scheduleStep(() ->
		{
			inner[0]	= Thread.currentThread();
			Component.createComponent(Component.class, () -> new Component(this)
			{
				{
					inner[1]	= Thread.currentThread();
				}
			});
			return (Void)null;
		}).get(TIMEOUT);
		// Test that component creation was scheduled on different thread.
		assertNotEquals(inner[0], inner[1], "Failed to switch threads.");
	}
	
	// thread resuse test is difficult due to race conditions
	/*@Test
	public void	testThreadReuse()
	{
		Component	comp	= Component.createComponent(Component.class, () -> new Component(this));
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
		Component	comp	= Component.createComponent(Component.class, () -> new Component(this));
		AtomicInteger	num	= new AtomicInteger(0);
		@SuppressWarnings("unchecked")
		IFuture<Boolean>[]	steps	= new IFuture[Runtime.getRuntime().availableProcessors()];
		for(int i=0; i<steps.length; i++)
		{
			int	mynum	= i;
			steps[i]	= comp.getComponentHandle().scheduleStep(() -> num.getAndIncrement()==mynum);
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
		Component	comp	= Component.createComponent(Component.class, () -> new Component(this));
		AtomicInteger	numthreads	= new AtomicInteger(0);
		Future<Void>	blocker	= new Future<>();
		@SuppressWarnings("unchecked")
		IFuture<Boolean>[]	steps	= new IFuture[Runtime.getRuntime().availableProcessors()];
		for(int i=0; i<steps.length; i++)
		{
			steps[i]	= comp.getComponentHandle().scheduleStep(() ->
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
		Component	comp	= Component.createComponent(Component.class, () -> new Component(this));
		IFuture<Boolean>	test	= comp.getComponentHandle().scheduleStep(() ->
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
		Component	comp	= Component.createComponent(Component.class, () -> new Component(this));
		long	wait	= 50;
		long before	= comp.getComponentHandle().scheduleStep(
			() -> IExecutionFeature.get().getTime()).get(TIMEOUT);
		comp.getFeature(IExecutionFeature.class).waitForDelay(wait).get(TIMEOUT);
		long after	= comp.getComponentHandle().scheduleStep(
			() -> IExecutionFeature.get().getTime()).get(TIMEOUT);
		assertTrue(after >= before+wait, "Not enough time has passed.");
	}
}
