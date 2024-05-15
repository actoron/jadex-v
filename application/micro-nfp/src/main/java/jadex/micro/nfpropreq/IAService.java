package jadex.micro.nfpropreq;

import jadex.future.IFuture;
import jadex.nfproperty.annotation.NFProperties;
import jadex.nfproperty.annotation.NFProperty;
import jadex.nfproperty.sensor.service.ExecutionTimeProperty;
import jadex.nfproperty.sensor.service.WaitqueueProperty;

/**
 * 
 */
public interface IAService 
{
	/**
	 *  Test method.
	 */
	@NFProperties(
	{
		@NFProperty(ExecutionTimeProperty.class),
		@NFProperty(WaitqueueProperty.class)
	})
	public IFuture<String> test();
}
