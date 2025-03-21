package jadex.bdi.marsworld.environment;

import jadex.environment.SpaceObject;
import jadex.math.IVector2;

public class Homebase extends SpaceObject
{
	protected long missiontime;

	protected int ore;
	
	protected double width;
	
	protected double height;
	
	public Homebase(IVector2 position, long missiontime)
	{
		this(position, missiontime, 0.1, 0.1);
	}
	
	public Homebase(IVector2 position, long missiontime, double width, double height)
	{
		super(position);
		this.missiontime = missiontime;
		this.width = width;
		this.height = height;
	}

	public long getMissionTime() 
	{
		//System.out.println("missiontime: "+missiontime);
		return missiontime;
	}

	public void setMissionTime(long missiontime) 
	{
		this.missiontime = missiontime;
	}

	public int getOre() 
	{
		return ore;
	}

	public void setOre(int ore) 
	{
		this.ore = ore;
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
	
	public Homebase copy()
	{
		Homebase ret = new Homebase(this.getPosition(), this.getMissionTime());
		ret.setHeight(this.getHeight());
		ret.setWidth(this.getWidth());
		ret.setId(this.getId());
		ret.setOre(this.getOre());
		return ret;
	}
	
	public void onUpdateFrom(SpaceObject source)
	{
		Homebase h = (Homebase)source;
		setHeight(h.getHeight());
		setWidth(h.getWidth());
		setOre(h.getOre());
		setMissionTime(h.getMissionTime());
		//System.out.println("updated target: "+this);
	}
	
	public String getRemainingTime()
	{
		long rt = getMissionTime()-System.currentTimeMillis();
		if(rt<0) rt=0;
		return ""+rt/1000+"s";
	}
}
