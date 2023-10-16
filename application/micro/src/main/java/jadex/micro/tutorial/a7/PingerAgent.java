package jadex.micro.tutorial.a7;

import jadex.common.SUtil;
import jadex.mj.core.IComponent;
import jadex.mj.feature.providedservice.IService;
import jadex.mj.micro.annotation.Agent;
import jadex.mj.requiredservice.annotation.OnService;

@Agent
public class PingerAgent 
{	
	/*@OnStart
	protected void onStart(IComponent agent)
	{
		System.out.println("agent started: "+agent.getId().getLocalName());
	}*/
	
	@OnService
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
		IComponent.create(new PingAgent());
		IComponent.create(new PingAgent());
		IComponent.create(new PingAgent());
		
		IComponent.create(new PingerAgent());
		
		IComponent.waitForLastComponentTerminated();
	}
}