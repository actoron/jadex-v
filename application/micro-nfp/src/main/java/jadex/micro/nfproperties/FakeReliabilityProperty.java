package jadex.micro.nfproperties;

import jadex.core.IComponent;
import jadex.nfproperty.impl.NFPropertyMetaInfo;
import jadex.nfproperty.impl.SimpleValueNFProperty;

/**
 *  Property simulating a random service reliability.
 *
 */
public class FakeReliabilityProperty extends SimpleValueNFProperty<Double, Void>
{
	/**
	 * Creates the property.
	 */
	public FakeReliabilityProperty(IComponent comp)
	{
		super(comp, new NFPropertyMetaInfo("fakereliability", Double.class, Void.class, true, 10000, true, null));
	}
	
	/**
	 *  Measure the value.
	 */
	public Double measureValue()
	{
		return Math.random() * 100.0;
	}
}
