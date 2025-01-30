package jadex.bdi.marsworld.environment;

import jadex.bdi.marsworld.math.IVector2;
import jadex.bdi.marsworld.math.Vector2Double;

public class Carry extends BaseObject
{
	public enum Status
	{
		Driving, Loading, Unloading
	}
	
	protected int ore;
	
	protected int capacity;
	
	protected Status status;
	
	public Carry(IVector2 position, int capacity) 
	{
		this(null, position, capacity);
	}
	
	public Carry(String name, IVector2 position, int capacity) 
	{
		super(position, name, 0.15, 0.05, new Vector2Double(-1.111, -1.111), 0.07, 0.07);
		this.capacity = capacity;
	}

	public Status getStatus() 
	{
		return status;
	}

	public void setStatus(Status status) 
	{
		this.status = status;
	}

	public int getOre() 
	{
		return ore;
	}

	public void setOre(int ore) 
	{
		this.ore = ore;
	}

	public int getCapacity() 
	{
		return capacity;
	}

	public void setCapacity(int capacity) 
	{
		this.capacity = capacity;
	}
	
	public Carry copy()
	{
		Carry ret = new Carry(this.getName(), this.getPosition(), this.getCapacity());
		ret.setHeight(this.getHeight());
		ret.setWidth(this.getWidth());
		ret.setOre(this.getOre());
		ret.setRotation(this.getRotation());
		ret.setStatus(this.getStatus());
		ret.setSpeed(this.getSpeed());
		ret.setVision(this.getVision());
		ret.setId(this.getId());
		return ret;
	}
	
}
