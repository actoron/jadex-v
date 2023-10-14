package jadex.micro.watchdog;

import jadex.mj.core.IComponent;
import jadex.mj.core.annotation.OnStart;
import jadex.mj.feature.execution.IMjExecutionFeature;
import jadex.mj.micro.annotation.Agent;

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
			agent.getFeature(IMjExecutionFeature.class).waitForDelay(delay).get();
			
			IComponent.create(new WatchdogAgent());
		}
	}
}
