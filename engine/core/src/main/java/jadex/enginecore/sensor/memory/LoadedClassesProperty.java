package jadex.enginecore.sensor.memory;

import java.lang.management.ManagementFactory;

import jadex.enginecore.IInternalAccess;
import jadex.enginecore.nonfunctional.INFProperty.Target;
import jadex.enginecore.nonfunctional.NFPropertyMetaInfo;
import jadex.enginecore.nonfunctional.NFRootProperty;
import jadex.enginecore.sensor.unit.MemoryUnit;

/**
 *  Property for the number of loaded classes in the JVM.
 */
public class LoadedClassesProperty extends NFRootProperty<Integer, MemoryUnit>
{
	/** The name of the property. */
	public static final String NAME = "loaded classes";
	
	/**
	 *  Create a new property.
	 */
	public  LoadedClassesProperty(final IInternalAccess comp)
	{
		super(comp, new NFPropertyMetaInfo(NAME, int.class, null, 
			true, 5000, true, Target.Root));
	}
	
	/**
	 *  Measure the value.
	 */
	public Integer measureValue()
	{
		return Integer.valueOf(ManagementFactory.getClassLoadingMXBean().getLoadedClassCount());
	}
}
