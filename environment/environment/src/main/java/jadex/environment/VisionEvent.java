package jadex.environment;

import java.util.Set;

public class VisionEvent extends EnvironmentEvent
{
	protected Set<SpaceObject> seen;

	public VisionEvent() 
	{
	}
	
	public VisionEvent(Set<SpaceObject> seen) 
	{
		this.seen = seen;
	}

	public Set<SpaceObject> getSeen() 
	{
		return seen;
	}

	public void setSeen(Set<SpaceObject> seen) 
	{
		this.seen = seen;
	}

	@Override
	public String toString() 
	{
		return "VisionEvent [seen=" + seen + "]";
	}
}
