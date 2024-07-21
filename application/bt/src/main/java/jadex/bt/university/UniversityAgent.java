package jadex.bt.university;

import jadex.bt.ActionNode;
import jadex.bt.Decorator;
import jadex.bt.IBTProvider;
import jadex.bt.Node;
import jadex.bt.Node.NodeState;
import jadex.bt.SelectorNode;
import jadex.bt.SequenceNode;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;

public class UniversityAgent implements IBTProvider
{
	/** The bdi agent. */
	@Agent
	protected IComponent agent;
	
	/** Belief if it is currently raining. Set through an agent argument. */
	protected boolean raining = Boolean.TRUE;
	
	/** Belief if wait time is not too long. Set through an agent argument. */
	protected boolean waiting = Boolean.TRUE;
	
	public Node<IComponent> createBehaviorTree()
	{
		// goal cometouni takex 	public enum Type{TRAIN, TRAM};
		
		// plan WalkPlan precondition !raining
		// TrainPlan precondition raining
		// TakeTram always
		// takeX check waiting -> fail
		
		ActionNode<IComponent> train = new ActionNode<>();
		train.setAction((e, agent) -> 
		{ 
			System.out.println("Walking to train stop: "+agent.getId());
			if(!waiting)
				return new Future<>(NodeState.FAILED);
			System.out.println("Going by train");
			return new Future<>(NodeState.SUCCEEDED);
		});
		Decorator<IComponent> rainy = new Decorator<>();
		rainy.setFunction((event, comp) -> raining? NodeState.RUNNING: NodeState.FAILED);
		train.addBeforeDecorator(rainy);
		
		ActionNode<IComponent> tram = new ActionNode<>();
		train.setAction((e, agent) -> 
		{ 
			System.out.println("Walking to tram stop: "+agent.getId());
			if(!waiting)
				return new Future<>(NodeState.FAILED);
			System.out.println("Going by tram");
			return new Future<>(NodeState.SUCCEEDED);
		});
		
		ActionNode<IComponent> walk = new ActionNode<>();
		walk.setAction((e, agent) -> 
		{ 
			System.out.println("Walking to uni: "+agent.getId());
			return new Future<>(NodeState.SUCCEEDED);
		});
		Decorator<IComponent> notrainy = new Decorator<>();
		notrainy.setFunction((event, comp) -> raining? NodeState.FAILED: NodeState.RUNNING);
		walk.addBeforeDecorator(notrainy);
		
		SelectorNode<IComponent> sel = new SelectorNode<>();
		sel.addChild(train).addChild(tram).addChild(walk);

		ActionNode<IComponent> finish = new ActionNode<>();
		finish.setAction((e, agent) ->
		{
			System.out.println("Reached uni");
			agent.terminate();
			return new Future<>(NodeState.SUCCEEDED);
		});
		
		SequenceNode<IComponent> seq = new SequenceNode<>();
		seq.addChild(sel).addChild(finish);
		
		return seq;
	}
	
	/** 
	 *  The agent body is executed on startup.
	 *  It creates and dispatches a come to university goal.
	 */
	@OnStart
	public void body()
	{
		System.out.println("raining: "+raining);
		System.out.println("waiting: "+waiting);
	}
	
	public static void main(String[] args)
	{
		IComponent.create(new UniversityAgent());
		IComponent.waitForLastComponentTerminated();
	}
}
