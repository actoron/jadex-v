package jadex.micro.helloworld;

import jadex.core.IComponentManager;

/**
 *  The micro version of the hello world agent.
 */
public class LambdaHelloWorld
{
	/**
	 *  Start the example.
	 */
	public static void main(String[] args)
	{
		System.out.println(IComponentManager.get().run(agent -> "Hello World from lambda agent: "+agent.getId()).get());
		IComponentManager.get().waitForLastComponentTerminated();
	}
}
