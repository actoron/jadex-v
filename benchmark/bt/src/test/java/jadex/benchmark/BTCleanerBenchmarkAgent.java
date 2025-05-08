package jadex.benchmark;

import jadex.bt.cleanerworld.BTCleanerAgent;
import jadex.future.Future;
import jadex.model.annotation.OnStart;

public class BTCleanerBenchmarkAgent extends BTCleanerAgent
{
	public Future<Void> future;
	
	public BTCleanerBenchmarkAgent()
	{
		// for components methods bytebuddy
	}
	
	public BTCleanerBenchmarkAgent(Future<Void> future, String envid)
	{
		super(envid, false);
		this.future = future;
	}
	
	@OnStart
	public void start()
	{
		super.start();
		future.setResult(null);
	}
}