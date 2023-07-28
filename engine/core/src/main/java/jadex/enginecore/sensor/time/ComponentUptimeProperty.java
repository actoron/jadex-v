package jadex.enginecore.sensor.time;

import jadex.enginecore.IInternalAccess;
import jadex.enginecore.nonfunctional.NFPropertyMetaInfo;
import jadex.enginecore.nonfunctional.NFRootProperty;
import jadex.enginecore.sensor.unit.TimeUnit;

/**
 *  Property for the startup of a component.
 */
public class ComponentUptimeProperty extends NFRootProperty<Long, TimeUnit>//extends TimedProperty
{
	/** The name of the property. */
	public static final String NAME = "uptime";
	
	/**
	 *  Create a new property.
	 */
	public ComponentUptimeProperty(final IInternalAccess comp)
	{
		super(comp, new NFPropertyMetaInfo(NAME, long.class, TimeUnit.class, true));
	}
	
	/**
	 *  Measure the value.
	 */
	public Long measureValue()
	{
		return System.currentTimeMillis()-comp.getDescription().getCreationTime();
	}
}
