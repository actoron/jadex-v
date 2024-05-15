package jadex.micro.nfservicetags;

import jadex.core.IComponent;
import jadex.future.IFuture;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Argument;
import jadex.micro.annotation.Arguments;
import jadex.nfproperty.sensor.service.TagProperty;
import jadex.providedservice.annotation.ProvidedService;
import jadex.providedservice.annotation.ProvidedServices;
import jadex.providedservice.annotation.Service;


@Agent
@Arguments(
	//@Argument(name=TagProperty.NAME, clazz=String.class, defaultvalue="new String[]{\"mytag1\",\"mytag2\"}")
	@Argument(name=TagProperty.NAME, clazz=String[].class, defaultvalue="new String[]{jadex.bridge.sensor.service.TagProperty.PLATFORM_NAME, null}")
//	@Argument(name=TagProperty.NAME, clazz=String.class, defaultvalue="new String[]{"+TagProperty.PLATFORM_NAME+","+TagProperty.JADEX_VERSION+"}")
)
@ProvidedServices(@ProvidedService(type=ITestService.class))
@Service
public class ProviderAgent implements ITestService
{
	@Agent
	protected IComponent agent;
	
	public IFuture<Void> method(String msg)
	{
		return IFuture.DONE;
	}
	
//	@AgentBody
//	public void body()
//	{
////		ITestService ts = SServiceProvider.getTaggedService(agent, ITestService.class, ServiceScope.PLATFORM, "mytag1", "mytag2").get(); 
////		Collection<ITestService> ts = SServiceProvider.getTaggedServices(agent, ITestService.class, ServiceScope.PLATFORM, TagProperty.PLATFORM_NAME, TagProperty.JADEX_VERSION).get(); 
//
//		Collection<ITestService> ts = SServiceProvider.getTaggedServices(agent, ITestService.class, ServiceScope.PLATFORM, TagProperty.PLATFORM_NAME).get(); 
//		
//		System.out.println("Found: "+ts);
//		
////		fut.addIntermediateResultListener(new IIntermediateResultListener<ITestService>()
////		{
////			public void exceptionOccurred(Exception exception)
////			{
////				System.out.println("ex: "+exception);
////			}
////			
////			public void resultAvailable(Collection<ITestService> result)
////			{
////				System.out.println("ra: "+result);
////			}
////			
////			public void intermediateResultAvailable(ITestService result)
////			{
////				System.out.println("found ires: "+result);
////			}
////			
////			public void finished()
////			{
////				System.out.println("fini");
////			}
////		});
//	}
}
