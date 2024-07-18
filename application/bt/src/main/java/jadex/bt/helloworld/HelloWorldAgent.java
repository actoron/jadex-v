package jadex.bt.helloworld;

import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.micro.MicroAgent;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Description;
import jadex.model.annotation.OnEnd;
import jadex.model.annotation.OnStart;

@Agent(type="bt")
public class HelloWorldAgent
{
	/** The micro agent class. */
	//@Agent
	//protected IComponent agent;
	
	/** The welcome text. */
	protected String text;
	
	public HelloWorldAgent(String text) 
	{
		this.text=text;
	}
	
	@OnStart
	public void executeBody(IComponent agent)
	{
		System.out.println("started:"+agent.getId()+" text: "+text+" "+agent);
		agent.getFeature(IExecutionFeature.class).waitForDelay(2000).get();
		System.out.println("Good bye world.");
		agent.terminate();
	}
	
	@OnEnd
	public void end(IComponent agent)
	{
		System.out.println("end: "+agent.getId());
	}
	
	public static void main(String[] args)
	{
		IComponent.create(new HelloWorldAgent("007"));
		IComponent.waitForLastComponentTerminated();
	}
}
