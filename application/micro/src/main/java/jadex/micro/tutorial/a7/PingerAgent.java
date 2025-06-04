package jadex.micro.tutorial.a7;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;
import jadex.providedservice.IService;

public class PingerAgent 
{	
	@OnStart
	protected void onStart(IComponent agent)
	{
		System.out.println("agent started: "+agent.getId().getLocalName());
	}
	
	@Inject
	protected void onPingServiceFound(IPingService service)
	{
		String name = ((IService)service).getServiceId().getProviderId().getLocalName();
		try
		{
			service.ping().get();
			System.out.println("Ping agent replied: "+name);
		}
		catch(Exception e)
		{
			System.out.println("ping agent did not respond: "+name);
		}
	}
	
	public static void main(String[] args) 
	{
		IComponentManager.get().create(new PingAgent());
		IComponentManager.get().create(new PingAgent());
		IComponentManager.get().create(new PingAgent());
		
		IComponentManager.get().create(new PingerAgent());
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}
