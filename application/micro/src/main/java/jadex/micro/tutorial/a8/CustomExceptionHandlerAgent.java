package jadex.micro.tutorial.a8;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.core.IThrowingConsumer;
import jadex.core.impl.ComponentManager;
import jadex.errorhandling.IErrorHandlingFeature;
import jadex.execution.IExecutionFeature;
import jadex.injection.annotation.OnStart;

public class CustomExceptionHandlerAgent
{
	@OnStart
	public void onStart(IComponent agent)
	{
		throw new RuntimeException("Exception in body");
	}
	
	public static void main(String[] args) throws InterruptedException 
	{
		ComponentManager.get().getFeature(IErrorHandlingFeature.class).addExceptionHandler(RuntimeException.class, true, (ex, comp) ->
		{
			System.out.println("custom exception handler ignoring: "+ex);
			
			comp.getFeature(IExecutionFeature.class).scheduleStep((IThrowingConsumer<IComponent>)(self ->
			{
				System.out.println("after exception: "+self.getId());
				
				throw new UnsupportedOperationException();
			}));
		});
		
		IComponentManager.get().create(new CustomExceptionHandlerAgent());
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}