package jadex.bdi.puzzle;

import jadex.bdi.runtime.IBDIAgent;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.micro.annotation.Agent;

@Agent(type="bdip")
public class BenchmarkAgent extends SokratesV3Agent
{
	/**
	 *  Overwrite wait time.
	 */
	public BenchmarkAgent(boolean printresults)
	{
		delay	= 0;
		printsteps	= false;
		this.printresults	= printresults;
	}

	/**
	 *  Overridden to skip gui creation.	
	 */
	protected void createGui(IComponent agent)
	{
	}
	
	public static void main(String[] args)
	{
		for(;;)
		{
			IBDIAgent.create(new BenchmarkAgent(true));
			IComponentManager.get().waitForLastComponentTerminated();
		}
	}
}
