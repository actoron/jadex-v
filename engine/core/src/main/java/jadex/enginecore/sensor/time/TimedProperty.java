package jadex.enginecore.sensor.time;

import jadex.enginecore.IInternalAccess;
import jadex.enginecore.nonfunctional.NFPropertyMetaInfo;
import jadex.enginecore.nonfunctional.SimpleValueNFProperty;
import jadex.enginecore.sensor.unit.TimeUnit;

/**
 *  Base property for time properties.
 */
public abstract class TimedProperty extends SimpleValueNFProperty<Long, TimeUnit>
{
	/**
	 *  Create a new property.
	 */
	public TimedProperty(String name, final IInternalAccess comp, long updaterate)
	{
		super(comp, new NFPropertyMetaInfo(name, long.class, TimeUnit.class, 
			updaterate>0? true: false, updaterate, true, null));
	}
	
	/**
	 *  Create a new property.
	 */
	public TimedProperty(String name, final IInternalAccess comp, boolean dynamic)
	{
		super(comp, new NFPropertyMetaInfo(name, long.class, TimeUnit.class, 
			dynamic, -1, true, null));
	}
}
