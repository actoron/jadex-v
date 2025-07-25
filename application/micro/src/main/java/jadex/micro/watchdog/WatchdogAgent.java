package jadex.micro.watchdog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jadex.core.Application;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.future.FutureBarrier;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;
import jadex.requiredservice.IRequiredServiceFeature;

/**
 *  The watchdog agent pings other watchdogs and issues an action,
 *  when a watchdog becomes unavailable.
 */
public class WatchdogAgent	implements IWatchdogService
{
	//-------- attributes --------
	
	/** The micro agent class. */
	@Inject
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
			Collection<IWatchdogService> services = agent.getFeature(IRequiredServiceFeature.class).searchServices(IWatchdogService.class).get();
			watchdogs.addAll(services);
			
			System.out.println("ping round: "+i);
			System.out.println("watchdogs: "+watchdogs);
			
			agent.getFeature(IExecutionFeature.class).waitForDelay(delay).get();
			
			FutureBarrier<Void> barrier = new FutureBarrier<Void>();
			services.stream().forEach(service ->
			{
				IFuture<Void> fut = service.ping();
				barrier.add(fut);
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
		Application app = new Application("Watchdog Example");
		
		app.create(new WatchdogAgent());
		app.create(new WatchdogAgent());
		app.create(new WatchdogAgent());
		
		app.create(new CreatorAgent());
		app.create(new TerminatorAgent());
		
		app.waitForLastComponentTerminated();
	}
}
