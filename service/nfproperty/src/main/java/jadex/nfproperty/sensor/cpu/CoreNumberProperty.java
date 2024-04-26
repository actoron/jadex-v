package jadex.nfproperty.sensor.cpu;

import jadex.core.IComponent;
import jadex.nfproperty.impl.NFPropertyMetaInfo;
import jadex.nfproperty.impl.NFRootProperty;

/**
 *  The number of cores.
 */
public class CoreNumberProperty extends NFRootProperty<Integer, Void>
{
	/** The name of the property. */
	public static final String NAME = "cpu cores";
	
	/**
	 *  Create a new property.
	 */
	public CoreNumberProperty(final IComponent comp)
	{
		super(comp, new NFPropertyMetaInfo(NAME, int.class, null, false, -1, false, Target.Root));
		setValue(Integer.valueOf(Runtime.getRuntime().availableProcessors()));
	}
	
	/**
	 *  Measure the value.
	 */
	public Integer measureValue()
	{
		return Integer.valueOf(Runtime.getRuntime().availableProcessors());
	}
}
