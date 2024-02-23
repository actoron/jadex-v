package jadex.bdi.puzzle;

import jadex.core.IComponent;
import jadex.micro.annotation.Agent;

@Agent(type="bdi")
public class BenchmarkAgent extends SokratesV3Agent
{
	/**
	 *  Overwrite wait time.
	 */
	public BenchmarkAgent()
	{
		delay	= 0;
	}

	/**
	 *  Overridden to skip gui creation.	
	 */
	protected void createGui(IComponent agent)
	{
	}
}
