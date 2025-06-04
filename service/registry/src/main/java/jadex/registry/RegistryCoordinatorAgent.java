package jadex.registry;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.SubscriptionIntermediateFuture;
import jadex.injection.annotation.Inject;
import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.impl.search.ServiceEvent;
import jadex.providedservice.impl.service.ServiceCall;

public class RegistryCoordinatorAgent implements IRegistryCoordinatorService 
{
	
	
	/** The agent. */
	@Inject
	protected IComponent agent;
	
	/** The known registries. */
	protected Set<RegistryInfo> registries = new HashSet<>();
	
	// registerRegistry() futures
	protected Set<SubscriptionIntermediateFuture<CoordinatorServiceEvent>> clientlisteners = new LinkedHashSet<>();

	/**
	 *  Initiates the client registration procedure
	 *  (super peer will answer initially with an empty intermediate result,
	 *  client will send updates with backward commands).
	 *  
	 *  @return Does not return any more results while connection is running.
	 */
	public ISubscriptionIntermediateFuture<Void> registerRegistry(IServiceIdentifier reg, long starttime)
	{
		//final ComponentIdentifier caller = ServiceCall.getCurrentInvocation().getCaller();
		RegistryInfo ri = new RegistryInfo(reg, starttime);
		
		// notify clients
		CoordinatorServiceEvent sa = new CoordinatorServiceEvent(reg, registries.contains(ri)? 
			ServiceEvent.SERVICE_CHANGED: ServiceEvent.SERVICE_ADDED, starttime);
		clientlisteners.stream().forEach(lis ->
		{
			lis.addIntermediateResultIfUndone(sa);
		});
		
		registries.add(ri);
		
		SubscriptionIntermediateFuture<Void> ret = new SubscriptionIntermediateFuture<>(ex ->
		{
			// on termination of registry 
			System.getLogger(getClass().getName()).log(Level.INFO, agent+": Super peer connection with registry "+reg+" terminated due to "+ex);
			registries.remove(ri);
			
			// notify clients
			CoordinatorServiceEvent sr = new CoordinatorServiceEvent(ri.sid(), ServiceEvent.SERVICE_REMOVED, ri.starttime());
			clientlisteners.stream().forEach(lis ->
			{
				lis.addIntermediateResultIfUndone(sr);
			});
		});
		
		// Initial register-ok response
		ret.addIntermediateResult(null);
		
		return ret;
	}
	
	// called by clients
	public ISubscriptionIntermediateFuture<CoordinatorServiceEvent> getRegistries()
	{
		final ComponentIdentifier caller = ServiceCall.getCurrentInvocation().getCaller();
		SubscriptionIntermediateFuture<CoordinatorServiceEvent> ret = new SubscriptionIntermediateFuture<>();
		clientlisteners.add(ret);
		
		ret.setTerminationCommand(ex ->
		{
			// on termination of registry 
			System.getLogger(getClass().getName()).log(Level.INFO, agent+": Coordinator connection with client "+caller+" terminated due to "+ex);
			clientlisteners.remove(ret);
		});
		
		registries.stream().forEach(reg ->
		{
			CoordinatorServiceEvent rse = new CoordinatorServiceEvent(reg.sid(), ServiceEvent.SERVICE_ADDED, reg.starttime());
			ret.addIntermediateResult(rse);
		});
		
		return ret;
	}
	
}
