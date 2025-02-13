package jadex.bdi.marsworld.environment;

import jadex.bdi.marsworld.math.IVector2;

public class Target extends SpaceObject
{
	public enum Status
	{
		Unknown, Analyzing, Analyzed
	}
	
	protected int capacity;
	
	protected int ore;
	
	protected Status status;
	
	protected double width;
	
	protected double height;
	
	public Target(IVector2 position, int ore)
	{
		this(position, ore, 0.1, 0.1);
	}
	
	public Target(IVector2 position, int ore, double width, double height)
	{
		super(position);
		this.ore = ore;
		this.width = width;
		this.height = height;
		this.status = Status.Unknown;
	}

	public int getCapacity() 
	{
		return capacity;
	}

	public void setCapacity(int capacity) 
	{
		this.capacity = capacity;
	}

	public int getOre() 
	{
		return ore;
	}

	public void setOre(int ore) 
	{
		this.ore = ore;
	}

	public Status getStatus() 
	{
		return status;
	}

	public void setStatus(Status status) 
	{
		this.status = status;
	}

	public double getWidth() 
	{
		return width;
	}

	public void setWidth(double width) 
	{
		this.width = width;
	}

	public double getHeight() 
	{
		return height;
	}

	public void setHeight(double height) 
	{
		this.height = height;
	}
	
	public Target copy()
	{
		Target ret = new Target(this.getPosition(), this.getOre());
		ret.setHeight(this.getHeight());
		ret.setWidth(this.getWidth());
		ret.setId(this.getId());
		ret.setOre(this.getOre());
		ret.setCapacity(this.getCapacity());
		ret.setStatus(this.getStatus());
		return ret;
	}
	
	public void updateFrom(SpaceObject source)
	{
		super.updateFrom(source);
		Target t = (Target)source;
		setHeight(t.getHeight());
		setWidth(t.getWidth());
		setOre(t.getOre());
		setCapacity(t.getCapacity());
		setStatus(t.getStatus());
		//System.out.println("updated target: "+this);
	}

	@Override
	public String toString() 
	{
		return "Target [capacity=" + capacity + ", ore=" + ore + ", status=" + status + ", position=" + position
				+ ", id=" + id + "]";
	}
	
}
