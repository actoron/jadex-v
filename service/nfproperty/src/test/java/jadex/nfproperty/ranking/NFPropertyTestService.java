package jadex.nfproperty.ranking;

import jadex.future.IFuture;
import jadex.injection.annotation.OnStart;
import jadex.nfproperty.annotation.NFProperties;
import jadex.nfproperty.annotation.NFProperty;

/**
 *  Service with nf props.
 */
public class NFPropertyTestService implements ICoreDependentService
{
	/**
	 *  Init method.
	 */
	@OnStart
	public IFuture<Void> x()
	{
//		System.out.println("SSTASD");
		return IFuture.DONE;
	}

	/**
	 *  Example method.
	 */
	@NFProperties(@NFProperty(name="methodspeed", value=MethodSpeedProperty.class))
	public IFuture<Void> testMethod()
	{
		return IFuture.DONE;
	}
}
