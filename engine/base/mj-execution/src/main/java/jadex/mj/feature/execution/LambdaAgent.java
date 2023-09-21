package jadex.mj.feature.execution;

import java.util.function.Supplier;

import jadex.future.IFuture;
import jadex.mj.core.MjComponent;
import jadex.mj.core.impl.SComponentFactory;

/**
 *  Create minimal components, just from a lambda function.
 */
public class LambdaAgent
{
	/**
	 *  Create a fire-and-forget component.
	 *  @param body	The code to be executed in the new component.
	 */
	public static void	create(Runnable body)
	{
		MjComponent	comp	= SComponentFactory.createComponent(MjComponent.class, () -> new MjComponent(null) {});
		IMjExecutionFeature.getExternal(comp).scheduleStep(body);
	}
	
	/**
	 *  Create a component and receive a result, when the body finishes.
	 *  @param body	The code to be executed in the new component.
	 */
	public static <T> IFuture<T>	create(Supplier<T> body)
	{
		MjComponent	comp	= SComponentFactory.createComponent(MjComponent.class, () -> new MjComponent(null) {});
		return IMjExecutionFeature.getExternal(comp).scheduleStep(body);
	}
}
