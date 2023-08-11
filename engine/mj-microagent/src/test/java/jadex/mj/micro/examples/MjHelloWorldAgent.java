package jadex.mj.micro.examples;

import jadex.mj.core.service.annotation.OnEnd;
import jadex.mj.core.service.annotation.OnStart;
import jadex.mj.micro.MjMicroAgent;
import jadex.mj.micro.annotation.Agent;
import jadex.mj.micro.annotation.AgentArgument;
import jadex.mj.micro.annotation.Argument;
import jadex.mj.micro.annotation.Arguments;
import jadex.mj.micro.annotation.Description;

/**
 *  The micro version of the hello world agent.
 */
@Agent(type="micro")
@Description("This agent prints out a hello message.")
@Arguments(@Argument(name="welcome text", description= "This parameter is the text printed by the agent.", 
	clazz=String.class, defaultvalue="\"Hello world, this is a Jadex micro agent.\""))
public class MjHelloWorldAgent
{
//	//-------- attributes --------
//	
//	/** The micro agent class. */
//	@MjAgent
	protected MjMicroAgent agent;
//	
//	/** The welcome text. */
	@AgentArgument("welcome text")
	protected String text;
//	
//	//-------- methods --------
//	
//	/**
//	 *  Execute an agent step.
//	 */
//	//@AgentBody
	@OnStart
	public void executeBody()
	{
		System.out.println(text+" "+agent);//.getId());
		//agent.getFeature(IExecutionFeature.class).waitForDelay(2000).get();
		System.out.println("Good bye world.");
		//agent.killComponent();
	}
	
	@OnEnd
	public void end()
	{
		System.out.println("end in pojo: "+agent);//.getId());
	}
	
	//-------- static part --------
	
	/**
	 *  Start the example.
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException 
	{
		MjMicroAgent.create(new MjHelloWorldAgent("007"));
		
		/*for(int i=0; i<1000000; i++)
		{
			System.out.println("Creating: "+i);
			MjMicroAgent.create(new MjHelloWorldAgent(Integer.toString(i)));
		}*/
	}
	
	public MjHelloWorldAgent(String text) {this.text=text;}
	public String	toString() {return "Hello "+text;}
}
