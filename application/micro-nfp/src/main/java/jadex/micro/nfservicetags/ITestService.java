package jadex.micro.nfservicetags;

import jadex.future.IFuture;
import jadex.nfproperty.annotation.NFProperties;
import jadex.nfproperty.annotation.NFProperty;
import jadex.nfproperty.sensor.service.TagProperty;

/**
 *  Example service interface.
 */
// per default use component argument 'tag' (shortcut for the second)
@NFProperties(@NFProperty(value=TagProperty.class)) 
//@NFProperties(@NFProperty(value=TagProperty.class, parameters=@NameValue(name=TagProperty.ARGUMENT, value="\"tag\""))) // == TagProperty.NAME

// directly add 'mytag'
//@NFProperties(@NFProperty(value=TagProperty.class, parameters=@NameValue(name=TagProperty.NAME, value="\"mytag\"")))

//@NFProperties(@NFProperty(value=TagProperty.class, parameters={
//	@NameValue(name=TagProperty.NAME, values={TagProperty.PLATFORM_NAME, TagProperty.JADEX_VERSION, "\"mytag\""}), 
//	@NameValue(name=TagProperty.ARGUMENT, value="\"tag\"") // additionally get tags from arguments 'tag'
//}))
public interface ITestService
{
	/**
	 *  A test method.
	 */
	public IFuture<Void> method(String msg);
}
