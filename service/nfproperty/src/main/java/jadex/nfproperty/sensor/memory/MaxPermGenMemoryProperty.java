package jadex.nfproperty.sensor.memory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;

import jadex.core.IComponent;

/**
 * 
 */
public class MaxPermGenMemoryProperty extends MemoryProperty
{
	/** The name of the property. */
	public static final String NAME = "max permgen memory";
	
	/**
	 *  Create a new property.
	 */
	public MaxPermGenMemoryProperty(final IComponent comp)
	{
		super(NAME, comp, -1);
	}
	
	/**
	 *  Measure the value.
	 */
	public Long measureValue()
	{
		final MemoryPoolMXBean pool = getPermGenMemoryPool();
		return pool!=null? pool.getUsage().getMax(): -1;
	}
	
	/**
	 *  Get the perm gen pool.
	 */
	protected static MemoryPoolMXBean getPermGenMemoryPool()
	{
		MemoryPoolMXBean ret = null;
		for(final MemoryPoolMXBean memoryPool : ManagementFactory.getMemoryPoolMXBeans())
		{
			if(memoryPool.getName().endsWith("Perm Gen"))
			{
				ret = memoryPool;
				break;
			}
		}
		return ret;
	}
}
