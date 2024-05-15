package jadex.micro.nfproperties;

import jadex.core.IComponent;
import jadex.future.IFuture;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;
import jadex.nfproperty.INFPropertyFeature;
import jadex.nfproperty.annotation.NFProperties;
import jadex.nfproperty.annotation.NFProperty;
import jadex.providedservice.IService;
import jadex.providedservice.annotation.Implementation;
import jadex.providedservice.annotation.ProvidedService;
import jadex.providedservice.annotation.ProvidedServices;
import jadex.providedservice.impl.search.ServiceQuery;
import jadex.requiredservice.IRequiredServiceFeature;

@Agent
@ProvidedServices(@ProvidedService(type=ICoreDependentService.class, implementation=@Implementation(NFPropertyTestService.class)))
@NFProperties(@NFProperty(name="componentcores", value=CoreNumberProperty2.class))
public class NFPropertyTestAgent
{
	@Agent
	protected IComponent agent;
	
	//@AgentBody
	@OnStart
	public IFuture<Void> body()
	{
		ICoreDependentService cds = agent.getFeature(IRequiredServiceFeature.class).searchService(new ServiceQuery<>( ICoreDependentService.class)).get();
		IService iscds = (IService)cds;
//		INFPropertyProvider prov = (INFPropertyProvider)iscds.getExternalComponentFeature(INFPropertyComponentFeature.class);
//		String[] names = SNFPropertyProvider.getNFPropertyNames(agent.getExternalAccess(), iscds.getId()).get();
		//String[] names = agent.getNFPropertyNames(iscds.getServiceId()).get();
		String[] names = agent.getFeature(INFPropertyFeature.class).getNFPropertyNames().get();
		
		System.out.println("Begin list of non-functional properties:");
		for(String name : names)
		{
			System.out.println(name);
		}
		System.out.println("Finished list of non-functional properties.");
		
		System.out.println("Service Value: " + agent.getFeature(INFPropertyFeature.class).getNFPropertyValue(iscds.getServiceId(), "cores").get());
		
		System.out.println("Component Value, requested from Service: " + agent.getFeature(INFPropertyFeature.class).getNFPropertyValue(iscds.getServiceId(), "componentcores").get());
//		try
//		{
//			System.out.println("Speed Value for method: " +iscds.getNFPropertyValue(ICoreDependentService.class.getMethod("testMethod", new Class<?>[0]), "methodspeed").get());
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace();
//		}
		
		return IFuture.DONE;
	}
}
