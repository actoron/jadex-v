package jadex.enginecore.sensor.memory;

import jadex.enginecore.IInternalAccess;
import jadex.enginecore.nonfunctional.INFProperty.Target;
import jadex.enginecore.nonfunctional.NFPropertyMetaInfo;
import jadex.enginecore.nonfunctional.NFRootProperty;
import jadex.enginecore.sensor.unit.MemoryUnit;

/**
 *  Abstract base memory property.
 */
public abstract class MemoryProperty extends NFRootProperty<Long, MemoryUnit>
{
	/**
	 *  Create a new property.
	 */
	public MemoryProperty(String name, final IInternalAccess comp, long updaterate)
	{
		super(comp, new NFPropertyMetaInfo(name, long.class, MemoryUnit.class, 
			updaterate>0? true: false, updaterate, true, Target.Root));
	}
}
