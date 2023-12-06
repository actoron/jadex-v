package jadex.mj.feature.nfproperties.sensor.time;

import jadex.core.IComponent;
import jadex.mj.feature.nfproperties.impl.NFPropertyMetaInfo;
import jadex.mj.feature.nfproperties.impl.NFRootProperty;
import jadex.mj.feature.nfproperties.sensor.unit.TimeUnit;

/**
 *  Property for the startup of a component.
 */
public class ComponentUptimeProperty extends NFRootProperty<Long, TimeUnit>//extends TimedProperty
{
	/** The name of the property. */
	public static final String NAME = "uptime";
	
	protected long creationtime;
	
	/**
	 *  Create a new property.
	 */
	public ComponentUptimeProperty(final IComponent comp)
	{
		super(comp, new NFPropertyMetaInfo(NAME, long.class, TimeUnit.class, true));
		this.creationtime = System.currentTimeMillis();
	}
	
	/**
	 *  Measure the value.
	 */
	public Long measureValue()
	{
		//return System.currentTimeMillis()-comp.getDescription().getCreationTime();
		return System.currentTimeMillis()-creationtime;
	}
}
