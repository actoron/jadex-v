package jadex.bdi.hellopure;

import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Trigger;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.injection.Val;
import jadex.injection.annotation.OnStart;

/**
 *  Simple hello agent that activates a plan based on a belief change.
 */
@BDIAgent
public class HelloPureAgent
{
	/** The text that is printed. */
	@Belief
	private Val<String> sayhello;
	
	/**
	 *  The agent body.
	 */
	@OnStart
	public void body(IComponent agent)
	{		
		sayhello.set("Hello BDI pure agent V3.");
		agent.getFeature(IExecutionFeature.class).waitForDelay(3000).get();
		System.out.println("terminating");
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
		IComponentManager.get().create(new HelloPureAgent());
		IComponentManager.get().waitForLastComponentTerminated();
	}
}
