package jadex.micro.watchdog;

import jadex.mj.core.IComponent;
import jadex.feature.execution.IExecutionFeature;
import jadex.mj.micro.annotation.Agent;
import jadex.mj.model.annotation.OnStart;

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
			
			IComponent.create(new WatchdogAgent());
		}
	}
}
