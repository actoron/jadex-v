package jadex.nfproperty.ranking;

import jadex.future.Future;
import jadex.future.IFuture;
import jadex.nfproperty.impl.AbstractNFProperty;
import jadex.nfproperty.impl.NFPropertyMetaInfo;
import jadex.nfproperty.sensor.unit.MemoryUnit;

/**
 *  Property simulating a random amount of network bandwidth.
 *
 */
public class FakeNetworkBandwidthProperty extends AbstractNFProperty<Long, MemoryUnit>
{
	/** Persistent simulated bandwidth. */
	long fakenet = (long) Math.round(Math.random() * 1000.0);
	
	/**
	 * Creates the property.
	 */
	public FakeNetworkBandwidthProperty()
	{
		super(new NFPropertyMetaInfo("fakenetworkbandwith", Long.class, MemoryUnit.class, false, -1, false, null));
	}
	
	/**
	 *  Returns the property value.
	 */
	public IFuture<Long> getValue(MemoryUnit unit)
	{
		long ret = fakenet;
		if(unit!=null)
		{
			ret = unit.convert(ret);
		}
		
		return new Future<Long>(ret);
	}
}
