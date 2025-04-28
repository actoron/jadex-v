package jadex.bdi.cleanerworld.environment;

import jadex.environment.SpaceObject;
import jadex.math.IVector2;

public class Cleaner extends SpaceObject
{
	protected String name;
	
	protected double speed;
	
	protected double vision;
	
	protected double chargestate;
	
	protected Waste carriedwaste;
	
	protected double size;
	
	public Cleaner(IVector2 position, String name, double speed, double vision, double chargestate)
	{
		super(position);
		this.name = name;
		this.speed = speed;
		this.vision = vision;
		this.chargestate = chargestate;
	}
	
	public Waste getCarriedWaste() 
	{
		return carriedwaste;
	}

	public void setCarriedWaste(Waste carriedwaste) 
	{
		this.carriedwaste = carriedwaste;
	}

	public IVector2 getLocation()
	{
		return getPosition();
	}
	
	public void setLocation(IVector2 loc)
	{
		setPosition(loc);
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

	public double getVisionRange() 
	{
		return vision;
	}

	public void setVisionRange(double vision) 
	{
		this.vision = vision;
	}

	public double getSize() 
	{
		return size;
	}

	public void setSize(double size) 
	{
		this.size = size;
	}

	public double getChargestate()
	{
		return chargestate;
	}
	
	public void setChargestate(double chargestate) 
	{
		this.chargestate = chargestate;
	}

	public Cleaner copy()
	{
		Cleaner ret = new Cleaner(this.getPosition(), this.getName(), 
			this.getSpeed(), this.getVisionRange(), this.getChargestate());
		ret.setId(this.getId());
		ret.setSize(this.getSize());
		ret.setCarriedWaste(this.getCarriedWaste());
		return ret;
	}
	
	public void onUpdateFrom(SpaceObject source)
	{
		Cleaner c = (Cleaner)source;
		setSize(c.getSize());
		setName(c.getName());
		setSpeed(c.getSpeed());
		setVisionRange(c.getVisionRange());
		setCarriedWaste(c.getCarriedWaste());
		setChargestate(c.getChargestate());
		//System.out.println("updated: "+this);
	}

	@Override
	public String toString() 
	{
		return "Cleaner [name=" + name + ", speed=" + speed + ", vision=" + vision + ", chargestate=" + chargestate
				+ ", carriedwaste=" + carriedwaste + ", size=" + size + ", position=" + position + ", id=" + id + "]";
	}

	
}
