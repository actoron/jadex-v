package jadex.environment;

public class VisionEvent extends EnvironmentEvent
{
	protected Vision vision;

	public VisionEvent() 
	{
	}
	
	public VisionEvent(Vision vision) 
	{
	    this.vision = vision;
	}

	public Vision getVision() 
	{
		return vision;
	}

	public void setVision(Vision vision) 
	{
		this.vision = vision;
	}

	@Override
	public String toString() 
	{
		return "VisionEvent [vision=" + vision + "]";
	}
	
}
