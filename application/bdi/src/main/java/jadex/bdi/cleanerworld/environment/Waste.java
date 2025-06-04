package jadex.bdi.cleanerworld.environment;

import jadex.environment.SpaceObject;
import jadex.math.IVector2;

public class Waste extends SpaceObject
{
	public Waste() 
	{
	}
	
	public Waste(IVector2 position)
	{
		super(position);
//		if(position==null)
//			System.out.println("created waste with pos: "+position);
	}
	
	public String toString() 
	{
		return "Waste(" + "id="+getId() + ", location="+getPosition() + ")";
	}
	
	public Waste copy()
	{
		Waste ret = new Waste(this.getPosition());
		ret.setId(this.getId());
		return ret;
	}
	
	public void onUpdateFrom(SpaceObject source)
	{
		if(source.getPosition()==null)
			System.out.println("waste loc to null");
		Waste w = (Waste)source;
		setPosition(w.getPosition());
	}
}
