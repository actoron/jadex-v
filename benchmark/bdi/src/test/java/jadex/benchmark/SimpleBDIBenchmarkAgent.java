package jadex.benchmark;

import jadex.future.Future;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;

/**
 *  Just set a future in onStart -> no BDI reasoning.
 */
@Agent(type="bdip")
public class SimpleBDIBenchmarkAgent
{
	Future<Void>	inited	= new Future<>();
	
	@OnStart
	public void	start()
	{
		inited.setResult(null);
	}
}
