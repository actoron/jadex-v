package jadex.benchmark;


import org.junit.jupiter.api.Test;

import jadex.bt.IBTProvider;
import jadex.bt.actions.TerminableUserAction;
import jadex.bt.nodes.ActionNode;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.Node.NodeState;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.impl.ComponentManager;
import jadex.future.Future;
import jadex.future.TerminableFuture;

/**
 *  Benchmark creation and killing of bt agents.
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
					an.setAction(new TerminableUserAction<IComponent>((e, agent) -> 
					{ 
						TerminableFuture<NodeState> fut = new TerminableFuture<>();
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
					an.setAction(new TerminableUserAction<IComponent>((e, agent) -> 
					{ 
						TerminableFuture<NodeState> fut = new TerminableFuture<>();
						System.out.println("compos: "+ComponentManager.get().getNumberOfComponents());
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
