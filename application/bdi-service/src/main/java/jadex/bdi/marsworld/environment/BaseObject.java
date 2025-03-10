package jadex.bdi.marsworld.environment;

import jadex.environment.SpaceObject;
import jadex.math.IVector2;

public class BaseObject extends SpaceObject
{
	protected String name;
	
	protected double speed;
	
	protected double vision;
	
	protected IVector2 rotation;
	
	protected double width;
	
	protected double height;

	public BaseObject(IVector2 position, String name, double speed, double vision, IVector2 rotation, double width, double height)
	{
		super(position);
		this.name = name;
		this.speed = speed;
		this.vision = vision;
		this.rotation = rotation;
		this.width = width;
		this.height = height;
	}
	
	public String getName() 
	{
		return name;
	}

	public void setName(String name) 
	{
		this.name = name;
	}

	public double getSpeed() 
	{
		return speed;
	}

	public void setSpeed(double speed) 
	{
		this.speed = speed;
	}

	public double getVision() 
	{
		return vision;
	}

	public void setVision(double vision) 
	{
		this.vision = vision;
	}

	public IVector2 getRotation() 
	{
		return rotation;
	}

	public void setRotation(IVector2 rotation) 
	{
		this.rotation = rotation;
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
	
	public BaseObject copy()
	{
		BaseObject ret = new BaseObject(this.getPosition(), this.getName(), 
			this.getSpeed(), this.getVision(), this.getRotation(), 
			this.getWidth(), this.getHeight());
		ret.setId(this.getId());
		return ret;
	}
	
	public void onUpdateFrom(SpaceObject source)
	{
		BaseObject bo = (BaseObject)source;
		setHeight(bo.getHeight());
		setWidth(bo.getWidth());
		setName(bo.getName());
		setRotation(bo.getRotation());
		setSpeed(bo.getSpeed());
		setVision(bo.getVision());
		//System.out.println("updated: "+this);
	}
}
