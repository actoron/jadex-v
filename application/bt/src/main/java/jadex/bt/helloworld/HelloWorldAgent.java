package jadex.bt.helloworld;

import java.lang.System.Logger.Level;

import jadex.bt.IBTProvider;
import jadex.bt.actions.UserAction;
import jadex.bt.nodes.ActionNode;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.Node.NodeState;
import jadex.core.Application;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.logger.ILoggingFeature;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnEnd;

@Agent(type="bt")
public class HelloWorldAgent implements IBTProvider
{
	public Node<IComponent> createBehaviorTree()
	{
		ActionNode<IComponent> an = new ActionNode<>("hello");
		an.setAction(new UserAction<IComponent>((e, agent) -> 
		{ 
			Future<NodeState> ret = new Future<>();
			System.out.println("Hello from behavior trees: "+agent.getId()+" "+agent.getAppId());
			agent.getFeature(IExecutionFeature.class).waitForDelay(2000).then(Void -> ret.setResult(NodeState.SUCCEEDED)).catchEx(ret);
			return ret;//new Future<>(NodeState.SUCCEEDED);
		}));
		return an;
	}
	
	@OnEnd
	public void end(IComponent self)
	{
		System.out.println("terminated: "+self.getId());
	}
	
	public static void main(String[] args)
	{
		IComponentManager.get().getFeature(ILoggingFeature.class).setDefaultSystemLoggingLevel(Level.ALL);
		
		//IComponentManager.get().create(new HelloWorldAgent()).get();
		//IComponentManager.get().waitForLastComponentTerminated();

		Application app1 = new Application("HelloWorld1");
		app1.create(new HelloWorldAgent()).get();
		Application app2 = new Application("HelloWorld2");
		app2.create(new HelloWorldAgent()).get();
		
		app1.waitForLastComponentTerminated();
		System.out.println("fini1");
		app2.waitForLastComponentTerminated();
		System.out.println("fini1&2");
		
		//IComponentManager.get().getFeature(IComponentFactory.class).create(new HelloWorldAgent());
		//IComponentManager.get().getFeature(IComponentFactory.class).waitForLastComponentTerminated();
		
		//IComponentManager.get().create(new HelloWorldAgent());
		//IComponentManager.get().waitForLastComponentTerminated();
		
		
		//IComponentManager.getFeature(ICreationFeature.class).create(new HelloWorldAgent());
		//IComponentManager.getFeature(ICreationFeature.class).waitForLastComponentTerminated();
	}
}
