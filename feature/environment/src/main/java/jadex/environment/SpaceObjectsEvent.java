package jadex.environment;

import java.util.HashSet;
import java.util.Set;

public class SpaceObjectsEvent extends EnvironmentEvent 
{
	protected Set<SpaceObject> objects;

	public SpaceObjectsEvent() 
	{
	}
	
	public SpaceObjectsEvent(SpaceObject object) 
	{
		this.objects = new HashSet<SpaceObject>();
		this.objects.add(object);
	}
	
	public SpaceObjectsEvent(Set<SpaceObject> objects) 
	{
		this.objects = objects;
	}

	public Set<SpaceObject> getObjects() 
	{
		return objects;
	}

	public void setObjects(Set<SpaceObject> objects) 
	{
		this.objects = objects;
	}

	@Override
	public String toString() 
	{
		return "SpaceObjectsEvent [objects=" + objects + "]";
	}
}
