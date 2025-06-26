package jadex.bt.cleanerworld.environment;

import jadex.environment.SpaceObject;
import jadex.math.IVector2;

public class Waste extends SpaceObject
{
	public Waste(IVector2 position)
	{
		super(position);
	}
	
	public IVector2 getLocation()
	{
		return getPosition();
	}
	
	public void setLocation(IVector2 loc)
	{
		setPosition(loc);
	}
	
	public String toString() 
	{
		return "Waste(" + "id="+getId() + ", location="+getLocation() + ")";
	}
	
	public Waste copy()
	{
		Waste ret = new Waste(this.getPosition());
		ret.setId(this.getId());
		return ret;
	}
	
	public void onUpdateFrom(SpaceObject source)
	{
		Waste w = (Waste)source;
		setPosition(w.getPosition());
	}
}
