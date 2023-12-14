package jadex.micro.tutorial.a8;

import jadex.core.IComponent;
import jadex.core.IThrowingConsumer;
import jadex.core.impl.ComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;

@Agent
public class CustomExceptionHandlerAgent
{
	@OnStart
	public void onStart(IComponent agent)
	{
		throw new RuntimeException("Exception in body");
	}
	
	public static void main(String[] args) throws InterruptedException 
	{
		ComponentManager.get().addExceptionHandler(RuntimeException.class, true, (ex, comp) ->
		{
			System.out.println("custom exception handler ignoring: "+ex);
			
			comp.getFeature(IExecutionFeature.class).scheduleStep((IThrowingConsumer<IComponent>)(self ->
			{
				System.out.println("after exception: "+self.getId());
				
				throw new UnsupportedOperationException();
			}));
		});
		
		IComponent.create(new CustomExceptionHandlerAgent());
		
		IComponent.waitForLastComponentTerminated();
	}
}