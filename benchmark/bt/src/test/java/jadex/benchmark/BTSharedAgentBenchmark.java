package jadex.benchmark;

import org.junit.jupiter.api.Test;

import jadex.bt.BTCache;
import jadex.bt.IBTProvider;
import jadex.bt.actions.TerminableUserAction;
import jadex.bt.nodes.ActionNode;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.Node.NodeState;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.future.Future;
import jadex.future.TerminableFuture;

/**
 *  Benchmark creation and killing of bt agents with shared tree.
 */
public class BTSharedAgentBenchmark
{
	public static class TestAgent implements IBTProvider 
	{
		protected Future<Void> future;
		
		public TestAgent(Future<Void> fut)
		{
			this.future = fut;
		}
		
		public Node<IComponent> createBehaviorTree() 	
	    { 		
	       	return BTCache.createOrGet(TestAgent.class, TestAgent::buildBehaviorTree);
	       	//return BTCache.createOrGet(HelloSharedBTAgent.class, this::createBehaviorTree2);
	    }
		
		public static Node<IComponent> buildBehaviorTree()
		{
			System.out.println("build");
			ActionNode<IComponent> an = new ActionNode<>("hello");
			an.setAction(new TerminableUserAction<IComponent>((e, agent) -> 
			{ 
				TerminableFuture<NodeState> fut = new TerminableFuture<>();
//				System.out.println("compos: "+ComponentManager.get().getNumberOfComponents());
				//System.out.println("Hello from behavior trees: "+agent.getId()+" "+agent.getAppId());
				//fut.setResult(NodeState.SUCCEEDED);
				((TestAgent)agent.getPojo()).future.setResult(null);
				return fut;
			}));
			return an;
		}
	}
	
	@Test
	void benchmarkTime()
	{
		BenchmarkHelper.benchmarkTime(() -> 
		{
			Future<Void> ret = new Future<>();
			IComponentHandle agent = IComponentManager.get().create(new TestAgent(ret)).get();
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
			IComponentHandle agent = IComponentManager.get().create(new TestAgent(ret)).get();
			ret.get();
			return () -> agent.terminate().get();
		}, 50);
	}

}
