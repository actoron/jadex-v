package jadex.micro.watchdog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.execution.IExecutionFeature;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;
import jadex.providedservice.IService;
import jadex.requiredservice.IRequiredServiceFeature;

public class TerminatorAgent 
{
	//-------- attributes --------
	
	/** The micro agent class. */
	@Inject
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
			agent.getFeature(IExecutionFeature.class).waitForDelay(delay).get();
			Collection<IWatchdogService> services = agent.getFeature(IRequiredServiceFeature.class).searchServices(IWatchdogService.class).get();

			if(services.size()>0)
			{
				List<IWatchdogService> killlist = new ArrayList<IWatchdogService>(services);
				int idx = (int)(Math.random()*services.size());
				IService service = (IService)killlist.get(idx);
				ComponentIdentifier victim = service.getServiceId().getProviderId();
				
				IComponentHandle access = agent.getApplication().getComponentHandle(victim);
				access.scheduleStep(agent ->
				{
					System.out.println("killing: "+victim);
					agent.terminate();
				});
			}
		}
	}
}
