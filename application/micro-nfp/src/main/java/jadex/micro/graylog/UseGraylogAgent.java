package jadex.micro.graylog;

import java.lang.System.Logger.Level;

import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;

@Agent
public class UseGraylogAgent 
{
	@OnStart
	protected void onStart(IComponent agent)
	{
		for(int i=0; ; i++)
		{
			agent.getFeature(IExecutionFeature.class).waitForDelay(1000).get();
			
			System.getLogger("logger").log(Level.INFO, agent.getId()+" "+i);
		}
	}
	
	public static void main(String[] args) 
	{
		IComponent.create(new UseGraylogAgent()).get();
		
		IComponent.waitForLastComponentTerminated();
	}
}
