package jadex.micro.tutorial.a8;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.injection.annotation.OnStart;

public class ExceptionAgent 
{
	@OnStart
	public void onStart(IComponent agent)
	{
		throw new RuntimeException("Exception in body");
	}
	
	public static void main(String[] args) throws InterruptedException 
	{
		IComponentManager.get().create(new ExceptionAgent());
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}