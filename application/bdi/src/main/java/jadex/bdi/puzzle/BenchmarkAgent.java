package jadex.bdi.puzzle;

import jadex.bdi.runtime.IBDIAgent;
import jadex.core.IComponent;
import jadex.micro.annotation.Agent;

@Agent(type="bdip")
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
	
	public static void main(String[] args)
	{
		IBDIAgent.create(new BenchmarkAgent());
		IComponent.waitForLastComponentTerminated();
	}
}
