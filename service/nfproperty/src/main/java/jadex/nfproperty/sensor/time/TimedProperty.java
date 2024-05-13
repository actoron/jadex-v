package jadex.nfproperty.sensor.time;

import jadex.core.IComponent;
import jadex.nfproperty.impl.NFPropertyMetaInfo;
import jadex.nfproperty.impl.SimpleValueNFProperty;
import jadex.nfproperty.sensor.unit.TimeUnit;

/**
 *  Base property for time properties.
 */
public abstract class TimedProperty extends SimpleValueNFProperty<Long, TimeUnit>
{
	/**
	 *  Create a new property.
	 */
	public TimedProperty(String name, final IComponent comp, long updaterate)
	{
		super(comp, new NFPropertyMetaInfo(name, long.class, TimeUnit.class, 
			updaterate>0? true: false, updaterate, true, null));
	}
	
	/**
	 *  Create a new property.
	 */
	public TimedProperty(String name, final IComponent comp, boolean dynamic)
	{
		super(comp, new NFPropertyMetaInfo(name, long.class, TimeUnit.class, 
			dynamic, -1, true, null));
	}
}
