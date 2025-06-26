package jadex.nfproperty.ranking;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;
import jadex.nfproperty.INFPropertyFeature;
import jadex.nfproperty.annotation.NFProperties;
import jadex.nfproperty.annotation.NFProperty;
import jadex.providedservice.IService;
import jadex.providedservice.annotation.ProvideService;
import jadex.providedservice.impl.search.ServiceQuery;
import jadex.requiredservice.IRequiredServiceFeature;

@NFProperties(@NFProperty(name="componentcores", value=CoreNumberProperty2.class))
public class NFPropertyTestAgent
{
	@Inject
	protected IComponent agent;
	
//	@ProvidedServices(@ProvidedService(type=ICoreDependentService.class, implementation=@Implementation(NFPropertyTestService.class)))
	@ProvideService
	protected ICoreDependentService	service	= new NFPropertyTestService();

	@OnStart
	public IFuture<Void> body()
	{
		System.out.println("started: "+agent.getId());
		
		ICoreDependentService cds = agent.getFeature(IRequiredServiceFeature.class).searchService(new ServiceQuery<>(ICoreDependentService.class)).get();
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
		
		// todo: would need parent nfprovider
		System.out.println("Component Value, requested from component: " + agent.getFeature(INFPropertyFeature.class).getNFPropertyValue("componentcores").get());
		System.out.println("Component Value, requested from service: " + agent.getFeature(INFPropertyFeature.class).getNFPropertyValue(iscds.getServiceId(), "componentcores").get());
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
	
	public static void main(String[] args) 
	{
		IComponentManager.get().create(new NFPropertyTestAgent()).get();
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}
