package jadex.bt.envexample;

import com.badlogic.gdx.math.Vector2;

import jadex.bt.IBTProvider;
import jadex.bt.actions.TerminableUserAction;
import jadex.bt.decorators.RepeatDecorator;
import jadex.bt.nodes.ActionNode;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.Node.NodeState;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.future.ITerminableFuture;
import jadex.future.TerminableFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnEnd;
import jadex.injection.annotation.OnStart;

public class BTRandomAgent implements IBTProvider
{
	@Inject
	protected IComponent agent;
	
	protected float size = 0.05f;
	
	protected volatile Vector2 waypoint = generateRandomPos();
	
	protected volatile Vector2 position = generateRandomPos();
	
	public BTRandomAgent()
	{
	}
	
	public Node<IComponent> createBehaviorTree()
	{
		ActionNode<IComponent> movetowaypoint = new ActionNode<>("gotowaypoint");
		movetowaypoint.setAction(new TerminableUserAction<IComponent>((e, agent) ->
		{
			TerminableFuture<NodeState> ret = new TerminableFuture<>();
			//System.out.println("Go to waypoint");
			
			moveTo(waypoint).then(Void -> 
			{
				//System.out.println("reached waypoint: "+waypoint+" "+position);
				waypoint = generateRandomPos(); 
				ret.setResult(NodeState.SUCCEEDED);
			})
			.catchEx(ex -> 
			{
				ex.printStackTrace(); 
				ret.setResult(NodeState.FAILED);
			});
			
			return ret;
		}));
		movetowaypoint.addDecorator(new RepeatDecorator<IComponent>(10));
		
		return movetowaypoint;
	}
	
	protected ITerminableFuture<Void> moveTo(Vector2 target)
	{
		TerminableFuture<Void> ret = new TerminableFuture<>();
		
		final boolean[] aborted = new boolean[1];
		
		Runnable step = new Runnable()
		{
			@Override
			public void run() 
			{
				if(!aborted[0])
				{
					Vector2 center = position.cpy().add(size/2, size/2);

	                if(center.dst(target) < 0.01)
	                {
	                    ret.setResultIfUndone(null);
	                }
	                else 
	                {
	                    Vector2 dir = target.cpy().sub(center).nor(); 
	                    position.add(dir.scl(0.001f)); 
	                    
	                    agent.getFeature(IExecutionFeature.class).waitForDelay(10)
	                        .then(Void -> this.run()).catchEx(ex -> ret.setExceptionIfUndone(ex));
	                }
				}
			}
		};
		
		step.run();
		
		ret.setTerminationCommand(ex -> {System.out.println("terminate move"); aborted[0]=true;});
		
		return ret;
	}
	
	protected Vector2 generateRandomPos()
	{
		return new Vector2((float)Math.random(), (float)Math.random());
	}
	
	@OnStart
	protected void onStart()
	{
		Environment.addAgent(this);
	}
	
	@OnEnd
	protected void onEnd()
	{
		Environment.removeAgent(this);
	}
	
	public Vector2 getWaypoint() 
	{
		return waypoint;
	}

	public Vector2 getPosition() 
	{
		return position;
	}
	
	public Vector2 getCenterPosition()
	{
		return position.cpy().add(size*0.5f, size*0.5f);
	}
	
	public float getSize() 
	{
		return size;
	}

	@Override
	public String toString() 
	{
		return ""+agent.getId().getLocalName();
	}

	public static void main(String[] args)
	{
		//IComponentManager.get().getFeature(ILoggingFeature.class).setDefaultSystemLoggingLevel(Level.INFO);
		
		int max = 5;
		for(int i=0; i<max; i++)
			IComponentManager.get().create(new BTRandomAgent());
		
		EnvGui.createEnv();
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}