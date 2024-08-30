package jadex.micro.helloworld;

import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;

@Agent
public class HelloMicro 
{
	@OnStart
	protected void start(IComponent agent)
	{
		System.out.println("agent started: "+agent.getId());
		agent.getFeature(IExecutionFeature.class).waitForDelay(1000).get();
		System.out.println("agent terminate: "+agent.getId());
		agent.terminate();
	}
	
	public static void main(String[] args) 
	{
		for(int i=0; i<100000; i++)
			IComponent.create(new HelloMicro());
		IComponent.waitForLastComponentTerminated();
	}
}