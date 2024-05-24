package jadex.requiredservicemicro.tag;


import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;
import jadex.providedservice.annotation.Tag;

/**
 *  Example service interface.
 */

//@Tag("$component.getArguments().get(\"tagarg\")")

// per default use component argument 'tag' (shortcut for the second)
//@NFProperties(@NFProperty(value=TagProperty.class)) 
//@NFProperties(@NFProperty(value=TagProperty.class, parameters=@NameValue(name=TagProperty.ARGUMENT, value="\"tag\""))) // == TagProperty.NAME

// directly add 'mytag'
//@NFProperties(@NFProperty(value=TagProperty.class, parameters=@NameValue(name=TagProperty.NAME, value="\"mytag\"")))

//@NFProperties(@NFProperty(value=TagProperty.class, parameters={
//	@NameValue(name=TagProperty.NAME, values={TagProperty.PLATFORM_NAME, TagProperty.JADEX_VERSION, "\"mytag\""}), 
//	@NameValue(name=TagProperty.ARGUMENT, value="\"tag\"") // additionally get tags from arguments 'tag'
//}))

@Service
@Tag("hello") 
@Tag("$cid") 
@Tag("$host") 
@Tag("null") 
public interface ITestService
{
	/**
	 *  A test method.
	 */
	public IFuture<Void> method(String msg);
}
