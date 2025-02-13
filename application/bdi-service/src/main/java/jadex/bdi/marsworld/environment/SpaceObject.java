package jadex.bdi.marsworld.environment;

import java.util.Objects;

import jadex.bdi.marsworld.math.IVector2;
import jadex.bdi.marsworld.math.Vector2Double;
import jadex.common.SReflect;

public class SpaceObject 
{
	protected IVector2 position;

	protected String id;
	
	public SpaceObject() 
	{
	}
	
	public SpaceObject(IVector2 position) 
	{
		this.position = position;
	}

	public IVector2 getPosition() 
	{
		return position;
	}

	public void setPosition(IVector2 position) 
	{
		this.position = position;
		//System.out.println("pos is: "+id+" "+position);
	}

	public String getId() 
	{
		return id;
	}

	public void setId(String id) 
	{
		this.id = id;
	}
	
	public String getType()
	{
		return SReflect.getUnqualifiedClassName(this.getClass());
	}

	@Override
	public int hashCode() 
	{
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) 
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SpaceObject other = (SpaceObject) obj;
		return Objects.equals(id, other.id);
	}

	@Override
	public String toString() 
	{
		return "SpaceObject [position=" + position + ", id=" + id + "]";
	}
	
	public SpaceObject copy()
	{
		SpaceObject ret = new SpaceObject(new Vector2Double(this.getPosition()));
		ret.setId(this.getId());
		return ret;
	}
	
	public void updateFrom(SpaceObject source)
	{
		this.position = source.getPosition();
		this.id = source.getId();
	}
}
