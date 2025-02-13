package jadex.bdi.marsworld.environment;

import jadex.bdi.marsworld.math.IVector2;
import jadex.bdi.marsworld.math.Vector2Double;

public class Producer extends BaseObject
{
	public enum Status
	{
		Driving, Producing
	}
	
	protected Status status;
	
	public Producer(String name, IVector2 position) 
	{
		super(position, name, 0.1, 0.05, new Vector2Double(-1.111, -1.111), 0.07, 0.07);
	}

	public Status getStatus() 
	{
		return status;
	}

	public void setStatus(Status status) 
	{
		this.status = status;
	}
	
	public Producer copy()
	{
		Producer ret = new Producer(this.getName(), this.getPosition());
		ret.setHeight(this.getHeight());
		ret.setWidth(this.getWidth());
		ret.setRotation(this.getRotation());
		ret.setStatus(this.getStatus());
		ret.setSpeed(this.getSpeed());
		ret.setVision(this.getVision());
		ret.setId(this.getId());
		return ret;
	}
	
	public void updateFrom(SpaceObject source)
	{
		super.updateFrom(source);
		Producer p = (Producer)source;
		setStatus(p.getStatus());
	}
	
}
