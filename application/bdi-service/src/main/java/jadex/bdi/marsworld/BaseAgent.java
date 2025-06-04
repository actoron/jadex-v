package jadex.bdi.marsworld;

import java.util.Set;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.Val;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Capability;
import jadex.bdi.marsworld.environment.BaseObject;
import jadex.bdi.marsworld.environment.MarsworldEnvironment;
import jadex.bdi.marsworld.environment.Target;
import jadex.bdi.marsworld.movement.MovementCapability;
import jadex.core.IComponent;
import jadex.environment.Environment;
import jadex.environment.SpaceObject;
import jadex.environment.SpaceObjectsEvent;
import jadex.environment.VisionEvent;
import jadex.execution.IExecutionFeature;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;

@BDIAgent
public abstract class BaseAgent 
{
	@Inject 
	protected IComponent agent;
	
	@Belief
	protected Val<BaseObject> self	= new Val<>((BaseObject)null);
	
	protected MarsworldEnvironment env;
	
	@Capability
	protected MovementCapability movecapa = new MovementCapability();

	public MovementCapability getMoveCapa()
	{
		return movecapa;
	}

	public BaseAgent(String envid)
	{
		this.env = (MarsworldEnvironment)Environment.get(envid);
	}
	
	@OnStart
	public void body()
	{
		self.set((BaseObject)movecapa.getEnvironment().addSpaceObject(createSpaceObject()).get());
		movecapa.getEnvironment().observeObject(getSpaceObject()).next(e ->
		{
			agent.getFeature(IExecutionFeature.class).scheduleStep(() ->
			{
				if(e instanceof VisionEvent)
				{
					Set<SpaceObject> seen = ((VisionEvent)e).getVision().getSeen();
					for(SpaceObject obj: seen)
					{
						if(obj instanceof Target)
						{
							getMoveCapa().addTarget((Target)obj);
							//System.out.println("New target seen: "+agent.getId().getLocalName()+", "+obj);
						}
					}
				}
				else if(e instanceof SpaceObjectsEvent)
				{
					Set<SpaceObject> changed = ((SpaceObjectsEvent)e).getObjects();
					//System.out.println("update space objects: "+agent.getId()+" "+changed);
					for(SpaceObject obj: changed)
					{
						if(obj.equals(movecapa.getMyself()))
						{
							getSpaceObject().updateFrom(obj);
						}
						else if(obj instanceof Target)
						{
							movecapa.updateTarget((Target)obj);
						}
					}
				}
			});
		});
		agent.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(movecapa.new WalkAround());
	}

	public IComponent getAgent()
	{
		return agent;
	}
	
	public BaseObject getSpaceObject()
	{
		return self.get();
	}
	
	/*public BaseObject getSpaceObject(boolean renew)
	{
		if(renew)
			self = (BaseObject)env.getSpaceObject(self.getId()).get();
		return self;
	}*/
	
	public MarsworldEnvironment getEnvironment()
	{
		return env; 
	}
	
	protected abstract BaseObject createSpaceObject();
}
