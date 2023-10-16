package jadex.micro.watchdog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jadex.mj.core.ComponentIdentifier;
import jadex.mj.core.IComponent;
import jadex.mj.core.IExternalAccess;
import jadex.mj.core.annotation.OnStart;
import jadex.mj.feature.execution.IMjExecutionFeature;
import jadex.mj.feature.providedservice.IService;
import jadex.mj.micro.annotation.Agent;
import jadex.mj.requiredservice.IMjRequiredServiceFeature;

@Agent
public class TerminatorAgent 
{
	//-------- attributes --------
	
	/** The micro agent class. */
	@Agent
	protected IComponent agent;
	
	//-------- agent body --------
	
	/**
	 *  Agent startup.
	 */
	@OnStart
	public void onStart()
	{
		while(true)
		{
			long delay = (long)(Math.random()*2000);
			System.out.println("terminator waiting for: "+delay);
			agent.getFeature(IMjExecutionFeature.class).waitForDelay(delay).get();
			Collection<IWatchdogService> services = agent.getFeature(IMjRequiredServiceFeature.class).getServices(IWatchdogService.class).get();

			if(services.size()>0)
			{
				List<IWatchdogService> killlist = new ArrayList<IWatchdogService>(services);
				int idx = (int)(Math.random()*services.size());
				IService service = (IService)killlist.get(idx);
				ComponentIdentifier victim = service.getServiceId().getProviderId();
				
				IExternalAccess access = agent.getExternalAccess(victim);
				access.scheduleStep(agent ->
				{
					System.out.println("killing: "+victim);
					agent.getFeature(IMjExecutionFeature.class).terminate();
				});
			}
		}
	}
}
