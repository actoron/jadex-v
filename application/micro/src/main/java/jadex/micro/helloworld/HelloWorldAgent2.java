package jadex.micro.helloworld;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnEnd;
import jadex.injection.annotation.OnStart;

/**
 *  The micro version of the hello world agent.
 */
public class HelloWorldAgent2
{
	//-------- attributes --------
	
	/** The micro agent class. */
	@Inject
	protected IComponent agent;
	
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
		System.out.println(text+" "+agent.getId().getLocalName());
		agent.getFeature(IExecutionFeature.class).waitForDelay(2000).get();
		System.out.println("Good bye world.");
		agent.terminate();
	}
	
	@OnEnd
	public void end()
	{
		System.out.println("end in pojo: "+agent.getId().getLocalName());
	}
	
	//-------- static part --------
	
	/**
	 *  Start the example.
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException 
	{
		//ComponentManager.get().setComponentIdNumberMode(true);
		
		IComponentManager.get().create(new HelloWorldAgent2("007"));
		
		IComponentManager.get().waitForLastComponentTerminated();
		
		//System.out.println("last component terminated");
	}
	
}
