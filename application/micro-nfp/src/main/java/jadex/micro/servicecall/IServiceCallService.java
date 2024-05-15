package jadex.micro.servicecall;

import jadex.future.IFuture;
import jadex.nfproperty.annotation.NFProperties;
import jadex.nfproperty.annotation.NFProperty;
import jadex.nfproperty.sensor.service.TagProperty;
import jadex.providedservice.annotation.Raw;

/**
 *  Service interface for service call benchmark.
 */
@NFProperties(@NFProperty(value=TagProperty.class)) 
public interface IServiceCallService
{
	/**
	 *  Dummy method for service call benchmark.
	 */
	public IFuture<Void> call();
	
	/**
	 *  Dummy method for service call benchmark.
	 */
	@Raw
	public IFuture<Void> rawcall();
}
