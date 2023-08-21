package jadex.mj.feature.execution;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jadex.future.Future;
import jadex.future.IFuture;
import jadex.mj.core.MjComponent;
import jadex.mj.core.impl.SComponentFactory;

public class ExecutionFeatureTest
{
	@Test
	public void	testStep()
	{
		MjComponent	comp	= SComponentFactory.createComponent(MjComponent.class, () -> new MjComponent(null){});
		IFuture<Boolean>	result	= IMjExecutionFeature.of(comp).scheduleStep(() -> true);
		assertTrue(result.get(), "Wrong step result.");
	}
	
	@Test
	public void	testBlockingStep()
	{
		MjComponent	comp	= SComponentFactory.createComponent(MjComponent.class, () -> new MjComponent(null){});
		Future<Boolean>	blocker	= new Future<>();
		IFuture<Boolean>	result	= IMjExecutionFeature.of(comp).scheduleStep(() -> blocker.get());
		IMjExecutionFeature.of(comp).scheduleStep(() -> blocker.setResult(true));
		assertTrue(result.get(3000), "Wrong step result.");
	}
	
	@Test
	public void	testBootstrapping()
	{
		Thread outer	= Thread.currentThread();
		Thread[] inner	= new Thread[1];
		SComponentFactory.createComponent(MjComponent.class, () -> new MjComponent(null){
			{
				inner[0]	= Thread.currentThread();
			}
		});
		assertNotEquals(outer, inner[0], "Failed to switch threads.");
	}
}
