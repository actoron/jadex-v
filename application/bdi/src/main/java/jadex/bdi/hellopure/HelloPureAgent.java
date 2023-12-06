package jadex.bdi.hellopure;

import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Trigger;
import jadex.bdi.runtime.BDIBaseAgent;
import jadex.bdi.runtime.IBDIAgent;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;

/**
 *  Simple hello agent that activates a plan based on a belief change.
 *  
 *  Pure BDI agent that is not bytecode enhanced. 
 *  This is achieved by using the baseclass BDIAgent that signals enhancement
 *  has already been done.
 */
@Agent(type="bdi")
public class HelloPureAgent extends BDIBaseAgent
{
	/** The text that is printed. */
	@Belief
	private String sayhello;
	
	/**
	 *  The agent body.
	 * /
	@OnStart
	public void body()
	{		
		sayhello = "Hello BDI pure agent V3.";
		beliefChanged("sayhello", null, sayhello);
		System.out.println("body end: "+getClass().getName());
	}*/
	
	/**
	 *  The agent body.
	 */
	@OnStart
	public void body()
	{		
		setBeliefValue("sayhello", "Hello BDI pure agent V3.");
		System.out.println("body end: "+getClass().getName());
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
		IBDIAgent.create(new HelloPureAgent());
	}
}
