package jadex.micro.nfpropvis;

import jadex.future.IFuture;
import jadex.nfproperty.annotation.NFProperties;
import jadex.nfproperty.annotation.NFProperty;
import jadex.nfproperty.sensor.service.WaitqueueProperty;

/**
 * 
 */
public interface ICryptoService 
{
//	/**
//	 *  Test method.
//	 */
//	@NFProperties(
//	{
//		@NFProperty(ExecutionTimeProperty.class),
//		@NFProperty(WaitqueueProperty.class)
//	})
//	public IFuture<String> test();
	
	/**
	 *  Method for encrypting a text snippet.
	 */
	@NFProperties({@NFProperty(WaitqueueProperty.class)})
	public IFuture<String> encrypt(String cleartext);
}
