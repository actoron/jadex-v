package jadex.micro.servicecall;

import jadex.core.ComponentIdentifier;
import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;
import jadex.providedservice.impl.service.BasicService;

/**
 *  Implementation of a service.
 */
@Service
public class RawServiceCallService extends BasicService	implements IServiceCallService
{
	/**
	 *  Basic service constructor.
	 */
	public RawServiceCallService(ComponentIdentifier providerid)
	{
		super(providerid, IServiceCallService.class, null);
	}
	
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