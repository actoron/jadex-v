package jadex.enginecore.sensor.cpu;

import jadex.common.OperatingSystemMXBeanFacade;
import jadex.enginecore.IInternalAccess;
import jadex.enginecore.nonfunctional.INFProperty.Target;
import jadex.enginecore.nonfunctional.NFPropertyMetaInfo;
import jadex.enginecore.nonfunctional.NFRootProperty;
import jadex.enginecore.sensor.unit.PercentUnit;

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
	public CPULoadProperty(final IInternalAccess comp)
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

