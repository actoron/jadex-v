package jadex.enginecore.sensor.mac;

import jadex.common.SUtil;
import jadex.enginecore.IInternalAccess;
import jadex.enginecore.nonfunctional.INFProperty.Target;
import jadex.enginecore.nonfunctional.NFPropertyMetaInfo;
import jadex.enginecore.nonfunctional.NFRootProperty;

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
	public MacAddressProperty(final IInternalAccess comp)
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
