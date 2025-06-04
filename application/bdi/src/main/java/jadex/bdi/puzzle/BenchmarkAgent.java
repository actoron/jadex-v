package jadex.bdi.puzzle;

import jadex.core.IComponent;
import jadex.core.IComponentManager;

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
			IComponentManager.get().create(new BenchmarkAgent(true)).get();
			IComponentManager.get().waitForLastComponentTerminated();
		}
	}
}
