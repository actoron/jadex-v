package jadex.requiredservicemicro.tag;


import jadex.providedservice.annotation.Service;
import jadex.providedservice.annotation.Tag;

@Service
@Tag("hello")
@Tag("$cid")
@Tag("$host") 
@Tag("tagarg")
public interface ITestService2
{
}
