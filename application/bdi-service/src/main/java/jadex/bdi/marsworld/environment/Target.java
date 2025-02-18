package jadex.bdi.marsworld.environment;

import jadex.environment.SpaceObject;
import jadex.math.IVector2;

public class Target extends SpaceObject
{
	public enum Status
	{
		Unknown, Analyzing, Analyzed
	}
	
	protected int ore; // produced ore
	
	protected int detectedOre;
	
	protected Status status;
	
	protected double width;
	
	protected double height;
	
	public Target(IVector2 position, int ore)
	{
		this(position, ore, 0.025, 0.05);
	}
	
	public Target(IVector2 position, int ore, double width, double height)
	{
		super(position);
		this.detectedOre = ore;
		this.width = width;
		this.height = height;
		this.status = Status.Unknown;
	}

	public int getDetectedOre() 
	{
		return detectedOre;
	}

	public void setDetectedOre(int detectedOre) 
	{
		this.detectedOre = detectedOre;
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
		ret.setDetectedOre(this.getDetectedOre());
		ret.setStatus(this.getStatus());
		return ret;
	}
	
	public void onUpdateFrom(SpaceObject source)
	{
		Target t = (Target)source;
		setHeight(t.getHeight());
		setWidth(t.getWidth());
		setOre(t.getOre());
		setDetectedOre(t.getDetectedOre());
		setStatus(t.getStatus());
		
		//System.out.println("updated target: "+this);
	}

	@Override
	public String toString() 
	{
		return "Target [capacity=" + detectedOre + ", ore=" + ore + ", status=" + status + ", position=" + position
				+ ", id=" + id + "]";
	}
	
}
