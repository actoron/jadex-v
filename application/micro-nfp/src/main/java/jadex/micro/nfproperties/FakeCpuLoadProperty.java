package jadex.micro.nfproperties;

import jadex.core.IComponent;
import jadex.nfproperty.impl.NFPropertyMetaInfo;
import jadex.nfproperty.impl.SimpleValueNFProperty;

/**
 *  Property simulating a random amount of CPU load.
 */
public class FakeCpuLoadProperty extends SimpleValueNFProperty<Double, Void>
{
	/**
	 * Creates the property.
	 */
	public FakeCpuLoadProperty(IComponent comp)
	{
		super(comp, new NFPropertyMetaInfo("fakecpuload", Double.class, Void.class, true, 10000, true, null));
	}
	
	/**
	 *  Measure the value.
	 */
	public Double measureValue()
	{
		return Math.random() * 100.0;
	}
}
