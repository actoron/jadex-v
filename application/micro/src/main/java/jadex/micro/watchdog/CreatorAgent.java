package jadex.micro.watchdog;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;

@Agent
public class CreatorAgent 
{
	@OnStart
	public void onStart(IComponent agent)
	{
		while(true)
		{
			long delay = (long)(Math.random()*2000);
			System.out.println("creator waiting for: "+delay);
			agent.getFeature(IExecutionFeature.class).waitForDelay(delay).get();
			
			IComponentManager.get().create(new WatchdogAgent());
		}
	}
}
