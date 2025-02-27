package jadex.bdi.marsworld.environment;

import jadex.environment.BaseObject;
import jadex.math.IVector2;
import jadex.math.Vector2Double;

public class Sentry extends BaseObject
{
	public Sentry(String name, IVector2 position) 
	{
		super(position, name, 0.1, 0.05, new Vector2Double(-1.111, -1.111), 0.07, 0.07);
	}
	
	public Sentry copy()
	{
		Sentry ret = new Sentry(this.getName(), this.getPosition());
		ret.setHeight(this.getHeight());
		ret.setWidth(this.getWidth());
		ret.setRotation(this.getRotation());
		ret.setSpeed(this.getSpeed());
		ret.setVision(this.getVision());
		ret.setId(this.getId());
		return ret;
	}
}
