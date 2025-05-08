package jadex.benchmark;

import jadex.bdi.cleanerworld.cleaner.CleanerAgent;
import jadex.bdi.runtime.IBDIAgentFeature;
import jadex.future.Future;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;

@Agent(type="bdip") // necessary due to BDI bug?!
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
