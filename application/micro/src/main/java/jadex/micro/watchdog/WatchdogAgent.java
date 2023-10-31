package jadex.micro.watchdog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jadex.core.IComponent;
import jadex.feature.execution.IExecutionFeature;
import jadex.future.FutureBarrier;
import jadex.future.IFuture;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;
import jadex.providedservice.annotation.Service;
import jadex.requiredservice.IRequiredServiceFeature;

/**
 *  The watchdog agent pings other watchdogs and issues an action,
 *  when a watchdog becomes unavailable.
 */
@Service
@Agent
public class WatchdogAgent	implements IWatchdogService
{
	//-------- attributes --------
	
	/** The micro agent class. */
	@Agent
	protected IComponent agent;
	
	/** The found watchdogs. */
	protected List<IWatchdogService> watchdogs;
	
	/** The delay. */
	protected long delay;
	
	//-------- agent body --------
	
	public WatchdogAgent()
	{
		this(3000);
	}
	
	public WatchdogAgent(long delay)
	{
		this.delay = delay;
		this.watchdogs	= new ArrayList<IWatchdogService>();
	}
	
	/**
	 *  Agent startup.
	 */
	@OnStart
	public void onStart()
	{
		System.out.println("Created watchdog: "+agent.getId());

		Set<IWatchdogService> watchdogs = new HashSet<IWatchdogService>();

		for(int i=0; ; i++)
		{			
			Collection<IWatchdogService> services = agent.getFeature(IRequiredServiceFeature.class).getServices(IWatchdogService.class).get();
			watchdogs.addAll(services);
			
			System.out.println("ping round: "+i);
			System.out.println("watchdogs: "+watchdogs);
			
			agent.getFeature(IExecutionFeature.class).waitForDelay(delay).get();
			
			FutureBarrier<Void> barrier = new FutureBarrier<Void>();
			services.stream().forEach(service ->
			{
				IFuture<Void> fut = service.ping();
				barrier.addFuture(fut);
				fut.then(x -> System.out.println("ping ok: "+service))
				.catchEx(ex -> {System.out.println("Watchdog triggered: "+service); watchdogs.remove(service);} );
			});
			
			barrier.waitForResultsIgnoreFailures(null).get();
		}
	}
	
	/**
	 *  Test if this watchdog is alive.
	 */
	public IFuture<Void> ping()
	{
		return IFuture.DONE;
	}
	
	public static void main(String[] args) 
	{
		IComponent.create(new WatchdogAgent());
		IComponent.create(new WatchdogAgent());
		IComponent.create(new WatchdogAgent());
		
		IComponent.create(new CreatorAgent());
		IComponent.create(new TerminatorAgent());
		
		IComponent.waitForLastComponentTerminated();
	}
}
