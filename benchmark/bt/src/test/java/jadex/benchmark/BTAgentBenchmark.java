package jadex.benchmark;


import org.junit.jupiter.api.Test;

import jadex.bt.IBTProvider;
import jadex.bt.actions.UserAction;
import jadex.bt.nodes.ActionNode;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.Node.NodeState;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.future.Future;

/**
 *  Benchmark creation and killing of micro agents.
 */
public class BTAgentBenchmark 
{
	@Test
	void benchmarkTime()
	{
		BenchmarkHelper.benchmarkTime(() -> 
		{
			Future<Void> ret = new Future<>();
			IComponentHandle agent = IComponentManager.get().create(new IBTProvider()
			{
				public Node<IComponent> createBehaviorTree()
				{
					ActionNode<IComponent> an = new ActionNode<>("hello");
					an.setAction(new UserAction<IComponent>((e, agent) -> 
					{ 
						Future<NodeState> fut = new Future<>();
						//System.out.println("Hello from behavior trees: "+agent.getId()+" "+agent.getAppId());
						//fut.setResult(NodeState.SUCCEEDED);
						ret.setResult(null);
						return fut;
					}));
					return an;
				}
			}).get();
			ret.get();
			agent.terminate().get();
		});
	}

	@Test
	void benchmarkMemory()
	{
		BenchmarkHelper.benchmarkMemory(() -> 
		{
			Future<Void> ret = new Future<>();
			IComponentHandle agent = IComponentManager.get().create(new IBTProvider()
			{
				public Node<IComponent> createBehaviorTree()
				{
					ActionNode<IComponent> an = new ActionNode<>("hello");
					an.setAction(new UserAction<IComponent>((e, agent) -> 
					{ 
						Future<NodeState> fut = new Future<>();
						//System.out.println("Hello from behavior trees: "+agent.getId()+" "+agent.getAppId());
						//fut.setResult(NodeState.SUCCEEDED);
						ret.setResult(null);
						return fut;
					}));
					return an;
				}
			}).get();
			ret.get();
			return () -> agent.terminate().get();
		});
	}

}
