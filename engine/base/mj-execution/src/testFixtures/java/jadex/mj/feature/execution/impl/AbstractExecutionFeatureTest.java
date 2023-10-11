package jadex.mj.feature.execution.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import jadex.future.Future;
import jadex.future.IFuture;
import jadex.mj.core.IComponent;
import jadex.mj.core.MjComponent;
import jadex.mj.feature.execution.IMjExecutionFeature;

public abstract class AbstractExecutionFeatureTest
{
	// Timeout how long a test blocks on a future before giving up.
	// Does not affect test execution time for successful tests.
	protected long	TIMEOUT	= 10000;
	
	@Test
	public void	testFeatureAccess()
	{
		// Test calling from outside thread
		assertThrows(IllegalCallerException.class, () -> IMjExecutionFeature.get());
		MjComponent	comp	= IComponent.createComponent(MjComponent.class, () -> new MjComponent(null){});
		assertThrows(IllegalCallerException.class, () -> IMjExecutionFeature.get());
		
		// Test calling from inside thread
		IFuture<IMjExecutionFeature>	fut	= IMjExecutionFeature.getExternal(comp).scheduleStep(
			() -> IMjExecutionFeature.get());
		assertEquals(comp.getFeature(IMjExecutionFeature.class), fut.get(TIMEOUT));
	}
	
	@Test
	public void	testGetComponent()
	{
		// Test inside creation
		Future<MjComponent>	fut	= new Future<>();
		MjComponent	comp	= IComponent.createComponent(MjComponent.class,
			() -> new MjComponent(null)
		{
			{
				fut.setResult(IMjExecutionFeature.get().getComponent());
			}
		});
		assertEquals(comp, fut.get(TIMEOUT));
		
		// Test after creation
		IFuture<MjComponent> result	= IMjExecutionFeature.getExternal(comp).scheduleStep(
			() -> IMjExecutionFeature.get().getComponent());
		assertEquals(comp, result.get(TIMEOUT));
				
		// Test after extra component creation
		MjComponent	comp2	= IComponent.createComponent(MjComponent.class, () -> new MjComponent(null){});
		result	= IMjExecutionFeature.getExternal(comp).scheduleStep(
				() -> IMjExecutionFeature.get().getComponent());
		IFuture<MjComponent> result2	= IMjExecutionFeature.getExternal(comp2).scheduleStep(
				() -> IMjExecutionFeature.get().getComponent());
		assertEquals(comp, result.get(TIMEOUT));
		assertEquals(comp2, result2.get(TIMEOUT));
		
		// Test after creation inside component
		MjComponent	comp3	= IMjExecutionFeature.getExternal(comp).scheduleStep(
			() -> IComponent.createComponent(MjComponent.class, () -> new MjComponent(null){})).get();
		result	= IMjExecutionFeature.getExternal(comp).scheduleStep(
				() -> IMjExecutionFeature.get().getComponent());
		IFuture<MjComponent> result3	= IMjExecutionFeature.getExternal(comp3).scheduleStep(
				() -> IMjExecutionFeature.get().getComponent());
		assertEquals(comp, result.get(TIMEOUT));
		assertEquals(comp3, result3.get(TIMEOUT));
	}
	
	@Test
	public void	testFireAndForgetStep()
	{
		MjComponent	comp	= IComponent.createComponent(MjComponent.class, () -> new MjComponent(null){});
		// Test executing a simple fire-and-forget step (Runnable implementation).
		Future<Boolean>	result	= new Future<>();
		Runnable	step	= () -> result.setResult(true);
		IMjExecutionFeature.getExternal(comp).scheduleStep(step);
		assertTrue(result.get(TIMEOUT), "Wrong step result.");
	}
	
	@Test
	public void	testResultStep()
	{
		MjComponent	comp	= IComponent.createComponent(MjComponent.class, () -> new MjComponent(null){});
		// Test executing a simple result step (Supplier implementation).
		IFuture<Boolean>	result	= IMjExecutionFeature.getExternal(comp).scheduleStep(() -> true);
		assertTrue(result.get(TIMEOUT), "Wrong step result.");
	}
	
	@Test
	public void	testExceptionStep()
	{
		MjComponent	comp	= IComponent.createComponent(MjComponent.class, () -> new MjComponent(null){});
		// Test executing a simple result step (Supplier implementation).
		IFuture<Boolean>	result	= IMjExecutionFeature.getExternal(comp).scheduleStep(() -> {
			throw new IllegalCallerException("ex");
		});
		assertThrows(IllegalCallerException.class, () -> result.get(TIMEOUT), "Wrong step exception.");
	}
	
	@Test
	public void	testErrorStep()
	{
		MjComponent	comp	= IComponent.createComponent(MjComponent.class, () -> new MjComponent(null){});
		// Test executing a simple result step (Supplier implementation).
		IFuture<Boolean>	result	= IMjExecutionFeature.getExternal(comp).scheduleStep(() -> {
			throw new InternalError("err");
		});
		assertThrows(RuntimeException.class, () -> result.get(TIMEOUT), "Wrong step error.");
	}
	
	@Test
	public void	testBlockingStep()
	{
		MjComponent	comp	= IComponent.createComponent(MjComponent.class, () -> new MjComponent(null){});
		Future<Boolean>	blocker	= new Future<>();
		IFuture<Boolean>	result	= IMjExecutionFeature.getExternal(comp).scheduleStep(() -> blocker.get());
		// Test that blocked step can be woken up by another step,
		// i.e., that a new thread was used to wake up the blocked thread.
		IMjExecutionFeature.getExternal(comp).scheduleStep(() -> blocker.setResult(true));
		assertTrue(result.get(3000), "Wrong step result.");
	}
	
	@Test
	public void	testTwoBlockingSteps()
	{
		MjComponent	comp	= IComponent.createComponent(MjComponent.class, () -> new MjComponent(null){});
		// Test that two steps can wait for same result.
		Future<Boolean>	blocker	= new Future<>();
		IFuture<Boolean>	result1	= IMjExecutionFeature.getExternal(comp).scheduleStep(() -> blocker.get());
		IFuture<Boolean>	result2	= IMjExecutionFeature.getExternal(comp).scheduleStep(() -> blocker.get());
		IMjExecutionFeature.getExternal(comp).scheduleStep(() -> blocker.setResult(true));
		assertTrue(result1.get(3000), "Wrong step result.");
		assertTrue(result2.get(3000), "Wrong step result.");
	}
	
	@Test
	public void	testIsComponentThread()
	{
		// Test without component
		assertThrows(IllegalCallerException.class, () -> IMjExecutionFeature.get());
		
		// Test that the component thread is already detected during bootstrapping
		Future<Boolean>	bootstrap0	= new Future<>();
		Future<Boolean>	bootstrap1	= new Future<>();
		MjComponent	comp	= IComponent.createComponent(MjComponent.class, () ->
		{
			// Test before component creation
			bootstrap0.setResult(MjExecutionFeature.LOCAL.get().isComponentThread());
			
			return new MjComponent(null)
			{
				{
					// Test during component creation
					bootstrap1.setResult(IMjExecutionFeature.get().isComponentThread());
				}
			};
		});
		assertTrue(bootstrap0.get(TIMEOUT));
		assertTrue(bootstrap1.get(TIMEOUT));
		
		// Test during normal component operation.
		assertFalse(IMjExecutionFeature.getExternal(comp).isComponentThread());
		IFuture<Boolean>	instep	= IMjExecutionFeature.getExternal(comp).scheduleStep(()
			-> IMjExecutionFeature.get().isComponentThread());
		assertTrue(instep.get(TIMEOUT));
		
		// Test across two components
		MjComponent	comp2	= IComponent.createComponent(MjComponent.class, () -> new MjComponent(null){});
		IFuture<Boolean>	othercomp	= IMjExecutionFeature.getExternal(comp2).scheduleStep(()
				-> IMjExecutionFeature.getExternal(comp).isComponentThread());
		assertFalse(othercomp.get(TIMEOUT));
	}
	
	@Test
	public void	testIsAnyComponentThread()
	{
		// Test without component
		assertFalse(IMjExecutionFeature.isAnyComponentThread());
		
		// Test that the component thread is already detected during bootstrapping
		Future<Boolean>	bootstrap0	= new Future<>();
		Future<Boolean>	bootstrap1	= new Future<>();
		MjComponent	comp	= IComponent.createComponent(MjComponent.class, () ->
		{
			// Test before component creation
			bootstrap0.setResult(IMjExecutionFeature.isAnyComponentThread());
			
			return new MjComponent(null)
			{
				{
					// Test during component creation
					bootstrap1.setResult(IMjExecutionFeature.isAnyComponentThread());
				}
			};
		});
		assertTrue(bootstrap0.get(TIMEOUT));
		assertTrue(bootstrap1.get(TIMEOUT));
		
		// Test during normal component operation.
		IFuture<Boolean>	instep	= IMjExecutionFeature.getExternal(comp).scheduleStep(()
			-> IMjExecutionFeature.isAnyComponentThread());
		assertTrue(instep.get(TIMEOUT));
	}
	
	@Test
	public void	testBootstrapping()
	{
		Thread outer	= Thread.currentThread();
		Thread[] inner	= new Thread[1];
		IComponent.createComponent(MjComponent.class, () -> new MjComponent(null)
		{
			{
				inner[0]	= Thread.currentThread();
			}
		});
		// Test that component creation was scheduled on different thread.
		assertNotEquals(outer, inner[0], "Failed to switch threads.");
	}
	
	@Test
	public void	testThreadReuse()
	{
		MjComponent	comp	= IComponent.createComponent(MjComponent.class, () -> new MjComponent(null){});
		Thread[]	current	= new Thread[1];
		@SuppressWarnings("unchecked")
		IFuture<Boolean>[]	steps	= new IFuture[Runtime.getRuntime().availableProcessors()];
		for(int i=0; i<steps.length; i++)
		{
			steps[i]	= IMjExecutionFeature.getExternal(comp).scheduleStep(() ->
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
	}
	
	@Test
	public void	testStepOrdering()
	{
		MjComponent	comp	= IComponent.createComponent(MjComponent.class, () -> new MjComponent(null){});
		AtomicInteger	num	= new AtomicInteger(0);
		@SuppressWarnings("unchecked")
		IFuture<Boolean>[]	steps	= new IFuture[Runtime.getRuntime().availableProcessors()];
		for(int i=0; i<steps.length; i++)
		{
			int	mynum	= i;
			steps[i]	= IMjExecutionFeature.getExternal(comp).scheduleStep(() -> num.getAndIncrement()==mynum);
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
		MjComponent	comp	= IComponent.createComponent(MjComponent.class, () -> new MjComponent(null){});
		AtomicInteger	numthreads	= new AtomicInteger(0);
		Future<Void>	blocker	= new Future<>();
		@SuppressWarnings("unchecked")
		IFuture<Boolean>[]	steps	= new IFuture[Runtime.getRuntime().availableProcessors()];
		for(int i=0; i<steps.length; i++)
		{
			steps[i]	= IMjExecutionFeature.getExternal(comp).scheduleStep(() ->
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
		MjComponent	comp	= IComponent.createComponent(MjComponent.class, () -> new MjComponent(null){});
		IFuture<Boolean>	test	= IMjExecutionFeature.getExternal(comp).scheduleStep(() ->
		{
			long	wait	= 50;
			long before	= IMjExecutionFeature.get().getTime();
			IMjExecutionFeature.get().waitForDelay(wait).get(TIMEOUT);
			long after	= IMjExecutionFeature.get().getTime();
			return after >= before+wait;
		});
		assertTrue(test.get(TIMEOUT), "Not enough time has passed.");
	}
	
	@Test
	public void	testExternalWaitForDelay()
	{
		MjComponent	comp	= IComponent.createComponent(MjComponent.class, () -> new MjComponent(null){});
		long	wait	= 50;
		long before	= IMjExecutionFeature.getExternal(comp).scheduleStep(
			() -> IMjExecutionFeature.get().getTime()).get(TIMEOUT);
		IMjExecutionFeature.getExternal(comp).waitForDelay(wait).get(TIMEOUT);
		long after	= IMjExecutionFeature.getExternal(comp).scheduleStep(
			() -> IMjExecutionFeature.get().getTime()).get(TIMEOUT);
		assertTrue(after >= before+wait, "Not enough time has passed.");
	}
}
