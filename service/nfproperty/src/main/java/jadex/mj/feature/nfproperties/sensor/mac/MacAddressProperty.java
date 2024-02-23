package jadex.mj.feature.nfproperties.sensor.mac;

import jadex.common.SUtil;
import jadex.core.IComponent;
import jadex.mj.feature.nfproperties.impl.NFPropertyMetaInfo;
import jadex.mj.feature.nfproperties.impl.NFRootProperty;

/**
 *  The (first) mac address.
 */
public class MacAddressProperty extends NFRootProperty<String, Void>
{
	/** The name of the property. */
	public static final String NAME = "mac address";
	
	/**
	 *  Create a new property.
	 */
	public MacAddressProperty(final IComponent comp)
	{
		super(comp, new NFPropertyMetaInfo(NAME, String.class, null, false, -1, false, Target.Root));
	}
	
	/**
	 *  Measure the value.
	 */
	public String measureValue()
	{
		return SUtil.getMacAddress();
	}
}
