package jadex.micro.servicecall;

import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

/**
 *  Implementation of a service.
 */
@Service
public class ServiceCallService	implements IServiceCallService
{
	/**
	 *  Dummy method for service call benchmark.
	 */
	public IFuture<Void> call()
	{
		return IFuture.DONE;
	}
	
	/**
	 *  Dummy method for service call benchmark.
	 */
	public IFuture<Void> rawcall()
	{
		return IFuture.DONE;
	}
}