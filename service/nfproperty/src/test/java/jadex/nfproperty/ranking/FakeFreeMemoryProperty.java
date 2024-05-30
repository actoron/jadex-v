package jadex.nfproperty.ranking;

import jadex.future.Future;
import jadex.future.IFuture;
import jadex.nfproperty.impl.AbstractNFProperty;
import jadex.nfproperty.impl.NFPropertyMetaInfo;
import jadex.nfproperty.sensor.unit.MemoryUnit;

/**
 *  Property simulating a random amount of free memory.
 *
 */
public class FakeFreeMemoryProperty extends AbstractNFProperty<Long, MemoryUnit>
{
	/**
	 * Creates the property.
	 */
	public FakeFreeMemoryProperty()
	{
		super(new NFPropertyMetaInfo("fakefreemem", Long.class, MemoryUnit.class, true, 3000, true, null));
	}
	
	/**
	 *  Returns the property value.
	 */
	public IFuture<Long> getValue(MemoryUnit unit)
	{
		long ret = (long) Math.round(Math.random() * 17179869184.0);
		if(unit!=null)
			ret = unit.convert(ret);
		
		return new Future<Long>(ret);
	}
}
