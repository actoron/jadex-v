package jadex.benchmark;

import jadex.bt.BTCache;
import jadex.bt.cleanerworld.BTCleanerAgent;
import jadex.bt.nodes.Node;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.injection.annotation.OnStart;

public class BTSharedCleanerBenchmarkAgent extends BTCleanerAgent
{
	public Future<Void> future;
	
	public BTSharedCleanerBenchmarkAgent()
	{
		// for components methods bytebuddy
	}
	
	public BTSharedCleanerBenchmarkAgent(Future<Void> future, String envid)
	{
		super(envid, false);
		this.future = future;
	}
	
	public Node<IComponent> createBehaviorTree() 	
    { 	
       	return BTCache.createOrGet(BTSharedCleanerBenchmarkAgent.class, BTCleanerAgent::buildBehaviorTree);
    }
	
	@OnStart
	public void start()
	{
		super.start();
		future.setResult(null);
	}
}