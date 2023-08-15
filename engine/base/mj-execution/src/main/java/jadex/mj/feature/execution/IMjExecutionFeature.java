package jadex.mj.feature.execution;

import java.util.function.Supplier;

import jadex.future.IFuture;
import jadex.mj.core.MjComponent;

public interface IMjExecutionFeature
{
	public static IMjExecutionFeature	of(MjComponent self)
	{
		return self.getFeature(IMjExecutionFeature.class);
	}
	
	public void scheduleStep(Runnable r);
	
	public <T> IFuture<T> scheduleStep(Supplier<T> s);
	
	public IFuture<Void>	waitForDelay(long millis);
}
