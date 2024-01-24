package jadex.micro.example.helloworld;

import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.micro.MicroAgent;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Description;
import jadex.model.annotation.OnEnd;
import jadex.model.annotation.OnStart;

/**
 *  The micro version of the hello world agent.
 */
@Agent(type="micro")
@Description("This agent prints out a hello message.")
//@Arguments(@Argument(name="welcome text", description= "This parameter is the text printed by the agent.", 
//	clazz=String.class, defaultvalue="\"Hello world, this is a Jadex micro agent.\""))
public class HelloWorldAgent
{
	//-------- attributes --------
	
	/** The micro agent class. */
	@Agent
	protected MicroAgent agent;
	
	/** The welcome text. */
//	@AgentArgument("welcome text")
	protected String text;
	
	//-------- methods --------
	
	public HelloWorldAgent(String text) 
	{
		this.text=text;
	}
	
	/**
	 *  Execute an agent step.
	 */
//	@AgentBody
	@OnStart
	public void executeBody()
	{
		System.out.println(text+" "+agent);//.getId());
		IExecutionFeature.get().waitForDelay(2000).get();
		System.out.println("Good bye world.");
		IExecutionFeature.get().terminate();
	}
	
	@OnEnd
	public void end()
	{
		System.out.println("end in pojo: "+agent);//.getId());
	}
	
	public String	toString() 
	{
		return "Hello "+text;
	}
	
	//-------- static part --------
	
	/**
	 *  Start the example.
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException 
	{
		MicroAgent.create(new HelloWorldAgent("007"));
		
		IComponent.waitForLastComponentTerminated();
	}
}
