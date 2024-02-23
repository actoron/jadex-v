package jadex.mj.feature.nfproperties.sensor.memory;

import jadex.core.IComponent;
import jadex.mj.feature.nfproperties.impl.INFProperty.Target;
import jadex.mj.feature.nfproperties.impl.NFPropertyMetaInfo;
import jadex.mj.feature.nfproperties.impl.NFRootProperty;
import jadex.mj.feature.nfproperties.sensor.unit.MemoryUnit;

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
