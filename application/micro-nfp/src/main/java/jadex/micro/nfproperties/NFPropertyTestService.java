package jadex.micro.nfproperties;

import jadex.future.IFuture;
import jadex.model.annotation.OnStart;
import jadex.nfproperty.annotation.NFProperties;
import jadex.nfproperty.annotation.NFProperty;
import jadex.providedservice.annotation.Service;

/**
 *  Service with nf props.
 */
@Service
public class NFPropertyTestService implements ICoreDependentService
{
	/**
	 *  Init method.
	 */
	//@ServiceStart
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
