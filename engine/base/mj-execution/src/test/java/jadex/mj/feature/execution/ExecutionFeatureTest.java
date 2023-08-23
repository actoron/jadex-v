package jadex.mj.feature.execution;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import jadex.future.Future;
import jadex.future.IFuture;
import jadex.mj.core.MjComponent;
import jadex.mj.core.impl.SComponentFactory;

public class ExecutionFeatureTest
{
	@Test
	public void	testFireAndForgetStep()
	{
		MjComponent	comp	= SComponentFactory.createComponent(MjComponent.class, () -> new MjComponent(null){});
		// Test executing a simple fire-and-forget step (Runnable implementation).
		Future<Boolean>	result	= new Future<>();
		Runnable	step	= () -> result.setResult(true);
		IMjExecutionFeature.of(comp).scheduleStep(step);
		assertTrue(result.get(), "Wrong step result.");
	}
	
	@Test
	public void	testResultStep()
	{
		MjComponent	comp	= SComponentFactory.createComponent(MjComponent.class, () -> new MjComponent(null){});
		// Test executing a simple result step (Supplier implementation).
		IFuture<Boolean>	result	= IMjExecutionFeature.of(comp).scheduleStep(() -> true);
		assertTrue(result.get(), "Wrong step result.");
	}
	
	@Test
	public void	testExceptionStep()
	{
		MjComponent	comp	= SComponentFactory.createComponent(MjComponent.class, () -> new MjComponent(null){});
		// Test executing a simple result step (Supplier implementation).
		IFuture<Boolean>	result	= IMjExecutionFeature.of(comp).scheduleStep(() -> {
			throw new IllegalCallerException("ex");
		});
		assertThrows(IllegalCallerException.class, () -> result.get(), "Wrong step exception.");
	}
	
	@Test
	public void	testErrorStep()
	{
		MjComponent	comp	= SComponentFactory.createComponent(MjComponent.class, () -> new MjComponent(null){});
		// Test executing a simple result step (Supplier implementation).
		IFuture<Boolean>	result	= IMjExecutionFeature.of(comp).scheduleStep(() -> {
			throw new InternalError("err");
		});
		assertThrows(RuntimeException.class, () -> result.get(), "Wrong step error.");
	}
	
	@Test
	public void	testBlockingStep()
	{
		MjComponent	comp	= SComponentFactory.createComponent(MjComponent.class, () -> new MjComponent(null){});
		Future<Boolean>	blocker	= new Future<>();
		IFuture<Boolean>	result	= IMjExecutionFeature.of(comp).scheduleStep(() -> blocker.get());
		// Test that blocked step can be woken up by another step,
		// i.e., that a new thread was used to wake up the blocked thread.
		IMjExecutionFeature.of(comp).scheduleStep(() -> blocker.setResult(true));
		assertTrue(result.get(3000), "Wrong step result.");
	}
	
	@Test
	public void	testTwoBlockingSteps()
	{
		MjComponent	comp	= SComponentFactory.createComponent(MjComponent.class, () -> new MjComponent(null){});
		// Test that two steps can wait for same result.
		Future<Boolean>	blocker	= new Future<>();
		IFuture<Boolean>	result1	= IMjExecutionFeature.of(comp).scheduleStep(() -> blocker.get());
		IFuture<Boolean>	result2	= IMjExecutionFeature.of(comp).scheduleStep(() -> blocker.get());
		IMjExecutionFeature.of(comp).scheduleStep(() -> blocker.setResult(true));
		assertTrue(result1.get(3000), "Wrong step result.");
		assertTrue(result2.get(3000), "Wrong step result.");
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
			steps[i]	= IMjExecutionFeature.of(comp).scheduleStep(() ->
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
			assertTrue(steps[i].get(), "New Thread detected.");
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
			steps[i]	= IMjExecutionFeature.of(comp).scheduleStep(() ->
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
			assertTrue(steps[i].get(), "Double execution detected.");
		}
	}
}
