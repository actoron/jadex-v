package jadex.bdi.cleanerworld.environment;

import jadex.environment.SpaceObject;
import jadex.math.IVector2;

public class Chargingstation extends SpaceObject
{
	public Chargingstation(IVector2 position)
	{
		super(position);
	}
	
	public IVector2 getLocation()
	{
		return getPosition();
	}
	
	public String toString() 
	{
		return "Chargingstation(" + "id="+getId() + ", location="+getLocation() + ")";
	}
	
	public Chargingstation copy()
	{
		Chargingstation ret = new Chargingstation(this.getPosition());
		ret.setId(this.getId());
		return ret;
	}
	
	public void onUpdateFrom(SpaceObject source)
	{
		Chargingstation c = (Chargingstation)source;
		setPosition(c.getPosition());
	}
}
