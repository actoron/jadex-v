package jadex.benchmark;

import jadex.bdi.annotation.BDIAgent;
import jadex.future.Future;
import jadex.injection.annotation.OnStart;

/**
 *  Just set a future in onStart -> no BDI reasoning.
 */
@BDIAgent
public class SimpleBDIBenchmarkAgent
{
	Future<Void>	inited	= new Future<>();
	
	@OnStart
	public void	start()
	{
		inited.setResult(null);
	}
}
