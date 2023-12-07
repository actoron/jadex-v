package jadex.micro.example.helloworld;

import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.micro.MicroAgent;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnEnd;
import jadex.model.annotation.OnStart;

/**
 *  The micro version of the hello world agent.
 */
@Agent(type="micro")
public class HelloWorldAgent2
{
	//-------- attributes --------
	
	/** The micro agent class. */
	@Agent
	protected MicroAgent agent;
	
	/** The welcome text. */
	protected String text;
	
	//-------- methods --------
	
	public HelloWorldAgent2(String text) 
	{
		this.text=text;
	}
	
	/**
	 *  Execute an agent step.
	 */
	@OnStart
	public void executeBody()
	{
		System.out.println(text+" "+agent.getId());
		agent.getFeature(IExecutionFeature.class).waitForDelay(2000).get();
		System.out.println("Good bye world.");
		agent.terminate();
	}
	
	@OnEnd
	public void end()
	{
		System.out.println("end in pojo: "+agent.getId());
	}
	
	//-------- static part --------
	
	/**
	 *  Start the example.
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException 
	{
		MicroAgent.create(new HelloWorldAgent2("007"));
		
		IComponent.waitForLastComponentTerminated();
		
		System.out.println("last component terminated");
	}
	
}
