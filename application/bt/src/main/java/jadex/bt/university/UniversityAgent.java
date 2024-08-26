package jadex.bt.university;

import jadex.bt.ActionNode;
import jadex.bt.Decorator;
import jadex.bt.IBTProvider;
import jadex.bt.Node;
import jadex.bt.Node.NodeState;
import jadex.bt.SelectorNode;
import jadex.bt.SequenceNode;
import jadex.bt.UserAction;
import jadex.bt.Val;
import jadex.bt.impl.BTAgentFeature;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;
import jadex.rules.eca.EventType;

@Agent(type="bt")
public class UniversityAgent implements IBTProvider
{
	/** The bdi agent. */
	@Agent
	protected IComponent agent;
	
	/** Belief if it is currently raining. Set through an agent argument. */
	protected Val<Boolean> raining = new Val<>(true);
	
	/** Belief if wait time is not too long. Set through an agent argument. */
	protected Val<Boolean> waiting = new Val<>(true);
	
	public UniversityAgent()
	{
	}
	
	public UniversityAgent(boolean raining, boolean waiting)
	{
		this.raining.set(raining);
		this.waiting.set(waiting);
	}
	
	public Node<IComponent> createBehaviorTree()
	{
		// train
		// tram
		// walk
		
		// take train precondition raining
		ActionNode<IComponent> train = new ActionNode<>();
		train.setAction(new UserAction<IComponent>((e, agent) -> 
		{ 
			System.out.println("Walking to train stop: "+agent.getId());
			if(!waiting.get())
			{
				System.out.println("failed waiting for train");
				return new Future<>(NodeState.FAILED);
			}
			System.out.println("Going by train");
			return new Future<>(NodeState.SUCCEEDED);
		}));
		Decorator<IComponent> rainy = new Decorator<>();
		rainy.setFunction((event, comp) -> raining.get()? NodeState.RUNNING: NodeState.FAILED);
		train.addBeforeDecorator(rainy);
		//train.setTrigger(null, new EventType[]{new EventType("raining", BTAgentFeature.VALUECHANGED)});
		train.setTriggerCondition((node, execontext) -> raining.get(), new EventType[]{new EventType("raining", BTAgentFeature.PROPERTYCHANGED)});
		
		// take tram always
		ActionNode<IComponent> tram = new ActionNode<>();
		tram.setAction(new UserAction<IComponent>((e, agent) -> 
		{ 
			System.out.println("Walking to tram stop: "+agent.getId());
			if(!waiting.get())
			{
				System.out.println("failed waiting for tram");
				if(!raining.get())
				{
					System.out.println("RAINING");
					raining.set(true);
					waiting.set(true);
				}
				return new Future<>(NodeState.FAILED);
			}
			System.out.println("Going by tram");
			return new Future<>(NodeState.SUCCEEDED);
		}));
		
		// walk when not raining
		ActionNode<IComponent> walk = new ActionNode<>();
		walk.setAction(new UserAction<IComponent>((e, agent) -> 
		{ 
			System.out.println("Walking to uni: "+agent.getId());
			return new Future<>(NodeState.SUCCEEDED);
		}));
		Decorator<IComponent> notrainy = new Decorator<>();
		notrainy.setFunction((event, comp) -> raining.get()? NodeState.FAILED: NodeState.RUNNING);
		walk.addBeforeDecorator(notrainy);
		//walk.setTrigger(null, new EventType[]{new EventType("raining", BTAgentFeature.VALUECHANGED)});
		walk.setTriggerCondition((node, execontext) -> !raining.get(), new EventType[]{new EventType("raining", BTAgentFeature.PROPERTYCHANGED)});
		
		SelectorNode<IComponent> sel = new SelectorNode<>();
		sel.addChild(train).addChild(tram).addChild(walk);

		ActionNode<IComponent> finish = new ActionNode<>();
		finish.setAction(new UserAction<IComponent>((e, agent) ->
		{
			System.out.println("Reached uni");
			agent.terminate();
			return new Future<>(NodeState.SUCCEEDED);
		}));
		
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
		// raining, waiting
		IComponent.create(new UniversityAgent(false, false));
		IComponent.waitForLastComponentTerminated();
	}
}
