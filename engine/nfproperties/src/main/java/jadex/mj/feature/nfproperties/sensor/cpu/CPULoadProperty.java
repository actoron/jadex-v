package jadex.mj.feature.nfproperties.sensor.cpu;

import jadex.common.OperatingSystemMXBeanFacade;
import jadex.core.IComponent;
import jadex.mj.feature.nfproperties.impl.NFPropertyMetaInfo;
import jadex.mj.feature.nfproperties.impl.NFRootProperty;
import jadex.mj.feature.nfproperties.sensor.unit.PercentUnit;

/**
 *  The cpu load property.
 */
public class CPULoadProperty extends NFRootProperty<Double, Void>
{
	/** The name of the property. */
	public static final String NAME = "cpu load";
	
	/**
	 *  Create a new property.
	 */
	public CPULoadProperty(final IComponent comp)
	{
		super(comp, new NFPropertyMetaInfo(NAME, double.class, PercentUnit.class, true, 3000, true, Target.Root));
	}
	
	/**
	 *  Measure the value.
	 */
	public Double measureValue()
	{
		Double ret = OperatingSystemMXBeanFacade.getSystemCpuLoad();
		//System.out.println("measure value on cpu prop: "+ret);
		return ret;
	}
}

