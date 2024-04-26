package jadex.nfproperty.sensor.memory;

import jadex.core.IComponent;
import jadex.nfproperty.impl.NFPropertyMetaInfo;
import jadex.nfproperty.impl.NFRootProperty;
import jadex.nfproperty.sensor.unit.MemoryUnit;

/**
 *  Abstract base memory property.
 */
public abstract class MemoryProperty extends NFRootProperty<Long, MemoryUnit>
{
	/**
	 *  Create a new property.
	 */
	public MemoryProperty(String name, final IComponent comp, long updaterate)
	{
		super(comp, new NFPropertyMetaInfo(name, long.class, MemoryUnit.class, 
			updaterate>0? true: false, updaterate, true, Target.Root));
	}
}
