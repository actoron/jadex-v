package jadex.micro.nfproperties;

import jadex.future.Future;
import jadex.future.IFuture;
import jadex.nfproperty.impl.AbstractNFProperty;
import jadex.nfproperty.impl.NFPropertyMetaInfo;

/**
 *  Non-functional property reporting the CPU core count with a different name.
 *
 */
public class CoreNumberProperty2 extends AbstractNFProperty<Integer, Void>
{
	/** CPU core count. */
	protected int cores;
	
	/**
	 *  Create the property.
	 */
	public CoreNumberProperty2()
	{
		super(new NFPropertyMetaInfo("componentcores", int.class, null, false, -1, false, null));
		cores = Runtime.getRuntime().availableProcessors();
		
//		Properties props = System.getProperties();
//		for (Object key : props.keySet())
//		{
//			System.out.println(System.getProperty((String) key));
//		}
	}

//	public IFuture<Integer> getValue(Class<Void> unit)
	/**
	 *  Gets the value.
	 */
	public IFuture<Integer> getValue(Void unit)
	{
		return new Future<Integer>(cores);
	}
}
