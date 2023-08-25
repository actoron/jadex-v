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
import jadex.mj.core.MjComponent;
import jadex.mj.core.impl.SComponentFactory;
import jadex.mj.feature.execution.IMjExecutionFeature;

public abstract class AbstractExecutionFeatureTest
{
	@Test
	public void	testFeatureAccess()
	{
		// Test calling from outside thread
		assertThrows(IllegalCallerException.class, () -> IMjExecutionFeature.get());
		MjComponent	comp	= SComponentFactory.createComponent(MjComponent.class, () -> new MjComponent(null){});
		assertThrows(IllegalCallerException.class, () -> IMjExecutionFeature.get());
		
		// Test calling from inside thread
		IFuture<IMjExecutionFeature>	fut	= IMjExecutionFeature.getExternal(comp).scheduleStep(
			() -> IMjExecutionFeature.get());
		assertEquals(comp.getFeature(IMjExecutionFeature.class), fut.get(1000));
	}
	
	@Test
	public void	testGetComponent()
	{
		// Test inside creation
		Future<MjComponent>	fut	= new Future<>();
		MjComponent	comp	= SComponentFactory.createComponent(MjComponent.class,
			() -> new MjComponent(null)
		{
			{
				fut.setResult(IMjExecutionFeature.get().getComponent());
			}
		});
		assertEquals(comp, fut.get(1000));
		
		// Test after creation
		IFuture<MjComponent> result	= IMjExecutionFeature.getExternal(comp).scheduleStep(
			() -> IMjExecutionFeature.get().getComponent());
		assertEquals(comp, result.get(1000));
	}
	
	@Test
	public void	testFireAndForgetStep()
	{
		MjComponent	comp	= SComponentFactory.createComponent(MjComponent.class, () -> new MjComponent(null){});
		// Test executing a simple fire-and-forget step (Runnable implementation).
		Future<Boolean>	result	= new Future<>();
		Runnable	step	= () -> result.setResult(true);
		IMjExecutionFeature.getExternal(comp).scheduleStep(step);
		assertTrue(result.get(1000), "Wrong step result.");
	}
	
	@Test
	public void	testResultStep()
	{
		MjComponent	comp	= SComponentFactory.createComponent(MjComponent.class, () -> new MjComponent(null){});
		// Test executing a simple result step (Supplier implementation).
		IFuture<Boolean>	result	= IMjExecutionFeature.getExternal(comp).scheduleStep(() -> true);
		assertTrue(result.get(1000), "Wrong step result.");
	}
	
	@Test
	public void	testExceptionStep()
	{
		MjComponent	comp	= SComponentFactory.createComponent(MjComponent.class, () -> new MjComponent(null){});
		// Test executing a simple result step (Supplier implementation).
		IFuture<Boolean>	result	= IMjExecutionFeature.getExternal(comp).scheduleStep(() -> {
			throw new IllegalCallerException("ex");
		});
		assertThrows(IllegalCallerException.class, () -> result.get(1000), "Wrong step exception.");
	}
	
	@Test
	public void	testErrorStep()
	{
		MjComponent	comp	= SComponentFactory.createComponent(MjComponent.class, () -> new MjComponent(null){});
		// Test executing a simple result step (Supplier implementation).
		IFuture<Boolean>	result	= IMjExecutionFeature.getExternal(comp).scheduleStep(() -> {
			throw new InternalError("err");
		});
		assertThrows(RuntimeException.class, () -> result.get(1000), "Wrong step error.");
	}
	
	@Test
	public void	testBlockingStep()
	{
		MjComponent	comp	= SComponentFactory.createComponent(MjComponent.class, () -> new MjComponent(null){});
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
		MjComponent	comp	= SComponentFactory.createComponent(MjComponent.class, () -> new MjComponent(null){});
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
		// Test that the component thread is already detected during bootstrapping
		Future<Boolean>	bootstrap0	= new Future<>();
		Future<Boolean>	bootstrap1	= new Future<>();
		MjComponent	comp	= SComponentFactory.createComponent(MjComponent.class, () ->
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
		assertTrue(bootstrap0.get(1000));
		assertTrue(bootstrap1.get(1000));
		
		// Test during normal component operation.
		assertFalse(IMjExecutionFeature.getExternal(comp).isComponentThread());
		IFuture<Boolean>	instep	= IMjExecutionFeature.getExternal(comp).scheduleStep(()
			-> IMjExecutionFeature.get().isComponentThread());
		assertTrue(instep.get(1000));
		
		// Test across two components
		MjComponent	comp2	= SComponentFactory.createComponent(MjComponent.class, () -> new MjComponent(null){});
		IFuture<Boolean>	othercomp	= IMjExecutionFeature.getExternal(comp2).scheduleStep(()
				-> IMjExecutionFeature.getExternal(comp).isComponentThread());
		assertFalse(othercomp.get(1000));
	}
	
	@Test
	public void	testBootstrapping()
	{
		Thread outer	= Thread.currentThread();
		Thread[] inner	= new Thread[1];
		SComponentFactory.createComponent(MjComponent.class, () -> new MjComponent(null)
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
		MjComponent	comp	= SComponentFactory.createComponent(MjComponent.class, () -> new MjComponent(null){});
		Thread[]	first	= new Thread[1];
		@SuppressWarnings("unchecked")
		IFuture<Boolean>[]	steps	= new IFuture[Runtime.getRuntime().availableProcessors()];
		for(int i=0; i<steps.length; i++)
		{
			steps[i]	= IMjExecutionFeature.getExternal(comp).scheduleStep(() ->
			{
				// Remember thread of first step
				if(first[0]==null)
				{
					first[0]	= Thread.currentThread();
					return true;
				}
				
				// Check if subsequent steps use same thread.
				else
				{
					return first[0]==Thread.currentThread();
				}
			});
		}
		// Collect results from all steps.
		for(int i=0; i<steps.length; i++)
		{
			assertTrue(steps[i].get(1000), "New Thread detected.");
		}
	}
	
	@Test
	public void	testDoubleExecution()
	{
		MjComponent	comp	= SComponentFactory.createComponent(MjComponent.class, () -> new MjComponent(null){});
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
			assertTrue(steps[i].get(1000), "Double execution detected.");
		}
	}
	
	@Test
	public void	testWaitForDelay()
	{
		MjComponent	comp	= SComponentFactory.createComponent(MjComponent.class, () -> new MjComponent(null){});
		IFuture<Boolean>	test	= IMjExecutionFeature.getExternal(comp).scheduleStep(() ->
		{
			long	wait	= 50;
			long before	= IMjExecutionFeature.get().getTime();
			IMjExecutionFeature.get().waitForDelay(wait).get(1000);
			long after	= IMjExecutionFeature.get().getTime();
			return after >= before+wait;
		});
		assertTrue(test.get(1000), "Not enough time has passed.");
	}
	
	@Test
	public void	testExternalWaitForDelay()
	{
		MjComponent	comp	= SComponentFactory.createComponent(MjComponent.class, () -> new MjComponent(null){});
		long	wait	= 50;
		long before	= IMjExecutionFeature.getExternal(comp).scheduleStep(
			() -> IMjExecutionFeature.get().getTime()).get(1000);
		IMjExecutionFeature.getExternal(comp).waitForDelay(wait).get(1000);
		long after	= IMjExecutionFeature.getExternal(comp).scheduleStep(
			() -> IMjExecutionFeature.get().getTime()).get(1000);
		assertTrue(after >= before+wait, "Not enough time has passed.");
	}
}
