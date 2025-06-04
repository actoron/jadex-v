package jadex.benchmark;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.cleanerworld.cleaner.CleanerAgent;
import jadex.future.Future;
import jadex.injection.annotation.OnStart;

public class BenchmarkCleanerBDIAgent extends CleanerAgent
{
	public Future<Void> future;
	
	public BenchmarkCleanerBDIAgent()
	{
	}
	
	public BenchmarkCleanerBDIAgent(Future<Void> future, String envid)
	{
		super(envid, false);
		this.future = future;
	}
	
	@OnStart
	public void start(IBDIAgentFeature bdifeature)
	{
		super.start(bdifeature);
		future.setResult(null);
	}
}
