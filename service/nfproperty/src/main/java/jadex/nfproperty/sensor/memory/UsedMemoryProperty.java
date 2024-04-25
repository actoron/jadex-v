package jadex.nfproperty.sensor.memory;

import jadex.common.OperatingSystemMXBeanFacade;
import jadex.core.IComponent;

/**
 *  The used physical memory.
 */
public class UsedMemoryProperty extends MemoryProperty
{
	/** The name of the property. */
	public static final String NAME = "used memory";
	
	/**
	 *  Create a new property.
	 */
	public UsedMemoryProperty(final IComponent comp)
	{
		super(NAME, comp, 5000);
	}
	
	/**
	 *  Measure the value.
	 */
	public Long measureValue()
	{
		return Long.valueOf(OperatingSystemMXBeanFacade.getTotalPhysicalMemorySize() 
			- OperatingSystemMXBeanFacade.getFreePhysicalMemorySize());
	}
}
