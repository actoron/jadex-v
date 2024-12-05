package jadex.micro.tutorial.a8;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.core.impl.ComponentManager;
import jadex.errorhandling.IErrorHandlingFeature;
import jadex.execution.IExecutionFeature;
import jadex.execution.LambdaAgent;

public class LambdaExceptions 
{
	public static void main(String[] args) throws InterruptedException 
	{
		ComponentManager.get().getFeature(IErrorHandlingFeature.class).addExceptionHandler(RuntimeException.class, true, (ex, comp) ->
		{
			System.out.println("handler 1 for runtime: "+ex);
		});
		
		ComponentManager.get().getFeature(IErrorHandlingFeature.class).addExceptionHandler(UnsupportedOperationException.class, true, (ex, comp) ->
		{
			System.out.println("handler 2 for unsupported: "+ex);
		});
		
		LambdaAgent.create(comp ->
		{
			final IComponent self = comp;
			Runnable action = new Runnable()
			{
				@Override
				public void run() 
				{
					self.getFeature(IExecutionFeature.class).waitForDelay(1000).then(v ->
					{
						run();
					});
						
					if(Math.random()>0.66)
						throw new RuntimeException("Exception in body");
					else if(Math.random()>0.33)
						throw new UnsupportedOperationException("Exception in body");
					else
						throw new IllegalStateException("Exception in body");
				}
			};
			
			action.run();
		});
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}
