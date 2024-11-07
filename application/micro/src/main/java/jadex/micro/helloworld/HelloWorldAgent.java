package jadex.micro.helloworld;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Description;
import jadex.model.annotation.OnEnd;
import jadex.model.annotation.OnStart;

/**
 *  The micro version of the hello world agent.
 */
@Agent(type="micro")
@Description("This agent prints out a hello message.")
public class HelloWorldAgent
{
	//-------- attributes --------
	
	/** The micro agent class. */
	//@Agent
	//protected IComponent agent;
	
	/** The welcome text. */
	protected String text;
	
	//-------- methods --------
	
	public HelloWorldAgent(String text) 
	{
		this.text=text;
	}
	
	/**
	 *  Execute an agent step.
	 */
	@OnStart
	public void executeBody(IComponent agent)
	{
		System.out.println("started:"+agent.getId()+" text: "+text);
		agent.getFeature(IExecutionFeature.class).waitForDelay(2000).get();
		System.out.println("Good bye world.");
		agent.terminate();
	}
	
	@OnEnd
	public void end(IComponent agent)
	{
		System.out.println("end: "+agent.getId());
	}
	
	//-------- static part --------
	
	/**
	 *  Start the example.
	 * @throws InterruptedException 
	 */
	public static void main(String[] args)
	{
		IComponentManager.get().create(new HelloWorldAgent("007"));
		IComponentManager.get().waitForLastComponentTerminated();
	}
}
