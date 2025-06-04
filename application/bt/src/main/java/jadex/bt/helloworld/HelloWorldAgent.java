package jadex.bt.helloworld;

import java.lang.System.Logger.Level;

import jadex.bt.IBTProvider;
import jadex.bt.actions.TerminableUserAction;
import jadex.bt.nodes.ActionNode;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.Node.NodeState;
import jadex.core.Application;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.future.TerminableFuture;
import jadex.injection.annotation.OnEnd;
import jadex.logger.ILoggingFeature;

public class HelloWorldAgent implements IBTProvider
{
	public Node<IComponent> createBehaviorTree()
	{
		ActionNode<IComponent> an = new ActionNode<>("hello");
		an.setAction(new TerminableUserAction<IComponent>((e, agent) -> 
		{ 
			TerminableFuture<NodeState> ret = new TerminableFuture<>();
			System.out.println("Hello from behavior trees: "+agent.getId()+" "+agent.getAppId());
			System.getLogger(""+HelloWorldAgent.class).log(Level.INFO, "Hello from behavior trees 1: "+agent.getId()+" "+agent.getAppId());
			System.getLogger(""+HelloWorldAgent.class).log(Level.WARNING, "Hello from behavior trees 2: "+agent.getId()+" "+agent.getAppId());
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
		//System.setProperty(FluentdLogger.HOST, "localhost");
		//System.setProperty(FluentdLogger.PORT, "34224"); // default port for forward protocol is 24224
		
		//IComponentManager.get().create(new HelloWorldAgent()).get();
		//IComponentManager.get().waitForLastComponentTerminated();
		
		//System.setProperty(OpenTelemetryLogger.URL, "https://otel.actoron.com");
		
		//System.setProperty(OpenTelemetryLogger.LOGLEVEL, "WARNING");
		
		IComponentManager.get().getFeature(ILoggingFeature.class).setDefaultSystemLoggingLevel(Level.ERROR);
		//IComponentManager.get().getFeature(ILoggingFeature.class).setDefaultAppLoggingLevel(Level.WARNING);
		
		//IComponentManager.get().getFeature(ILoggingFeature.class).setDefaultSystemLoggingLevel(Level.ALL);

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
