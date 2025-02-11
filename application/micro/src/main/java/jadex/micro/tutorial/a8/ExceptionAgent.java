package jadex.micro.tutorial.a8;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.micro.MicroAgent;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;

@Agent
public class ExceptionAgent 
{
	@OnStart
	public void onStart(IComponent agent)
	{
		throw new RuntimeException("Exception in body");
	}
	
	public static void main(String[] args) throws InterruptedException 
	{
		MicroAgent.create(new ExceptionAgent());
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}