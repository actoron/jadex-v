package jadex.bdi.hellopure;

import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Trigger;
import jadex.bdi.runtime.IBDIAgentFeature;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;

/**
 *  Simple hello agent that activates a plan based on a belief change.
 *  
 *  Pure BDI agent that is not bytecode enhanced. 
 *  This is achieved by using the baseclass BDIAgent that signals enhancement
 *  has already been done.
 */
@Agent(type="bdip")
public class HelloPureAgent
{
	/** The text that is printed. */
	@Belief
	private String sayhello;
	
	/**
	 *  The agent body.
	 */
	@OnStart
	public void body(IComponent agent, IBDIAgentFeature bdi)
	{		
		bdi.setBeliefValue("sayhello", "Hello BDI pure agent V3.");
		System.out.println("body end: "+getClass().getName());
		agent.getFeature(IExecutionFeature.class).waitForDelay(3000).get();
		agent.terminate();
	}
	
	@Plan(trigger=@Trigger(factchanged = "sayhello"))
	protected void printHello1()
	{
		System.out.println("plan activated: "+sayhello);
	}
	
	/**
	 *  Start a platform and the example.
	 */
	public static void main(String[] args) 
	{
		IComponent.create(new HelloPureAgent());
		IComponent.waitForLastComponentTerminated();
	}
}
