package jadex.registry.coordinator;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import jadex.common.SGUI;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.SubscriptionIntermediateFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;
import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.impl.search.ServiceEvent;
import jadex.providedservice.impl.service.ServiceCall;
import jadex.publishservice.IPublishServiceFeature;
import jadex.publishservice.publish.annotation.Publish;
import jakarta.ws.rs.GET;

@Publish(publishid="http://localhost:8081/${cid}/coordinatorapi", publishtarget = ICoordinatorGuiService.class)
public class CoordinatorAgent implements ICoordinatorService, ICoordinatorGuiService
{
	/** The agent. */
	@Inject
	protected IComponent agent;
	
	/** The known registries. */
	protected Set<RegistryInfo> registries = new HashSet<>();
	
	// registerRegistry() futures
	protected Set<SubscriptionIntermediateFuture<CoordinatorServiceEvent>> clientlisteners = new LinkedHashSet<>();
	
	protected Set<SubscriptionIntermediateFuture<CoordinatorServiceEvent>> uilisteners = new LinkedHashSet<>();

	@OnStart
	protected void onStart()
	{
		System.getLogger(getClass().getName()).log(Level.INFO, "Coordinator started at "+agent.getId());
		
		IPublishServiceFeature ps = agent.getFeature(IPublishServiceFeature.class);
		ps.publishResources("http://localhost:8081/${cid}", "jadex/registry/coordinator/ui");
		
		String url = "http://localhost:8081/"+agent.getId().getLocalName();
		System.out.println("open in browser: "+url);
		SGUI.openInBrowser(url);
	}
	
	/**
	 *  Initiates the client registration procedure
	 *  (super peer will answer initially with an empty intermediate result,
	 *  client will send updates with backward commands).
	 *  
	 *  @return Does not return any more results while connection is running.
	 */
	public ISubscriptionIntermediateFuture<Void> registerRegistry(IServiceIdentifier reg, long starttime)
	{
		System.out.println("Coordinator has new registry client:"+reg+" at "+Instant.ofEpochMilli(starttime));
		
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
			CoordinatorServiceEvent sr = new CoordinatorServiceEvent(ri.serviceid(), ServiceEvent.SERVICE_REMOVED, ri.starttime());
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
			CoordinatorServiceEvent rse = new CoordinatorServiceEvent(reg.serviceid(), ServiceEvent.SERVICE_ADDED, reg.starttime());
			ret.addIntermediateResult(rse);
		});
		
		return ret;
	}
	
	/**
	 *  Subscribe to coordinator updates.
	 */
	@GET
	public ISubscriptionIntermediateFuture<CoordinatorServiceEvent> subscribe()
	{
		SubscriptionIntermediateFuture<CoordinatorServiceEvent> ret = new SubscriptionIntermediateFuture<>();

		ret.setTerminationCommand(ex ->
		{
			System.getLogger(getClass().getName()).log(Level.INFO, agent+": Coordinator UI connection terminated due to "+ex);
			uilisteners.remove(ret);
		});
		
		for(RegistryInfo reg: registries)
		{
			CoordinatorServiceEvent rse = new CoordinatorServiceEvent(reg.serviceid(), ServiceEvent.SERVICE_ADDED, reg.starttime());
			ret.addIntermediateResult(rse);
		}
		if(registries.isEmpty())
		{
			// If no registries are known, send an empty event
			ret.addIntermediateResult(new CoordinatorServiceEvent(null, ServiceEvent.SERVICE_ADDED, 0));
		}
		
		uilisteners.add(ret);
		
		return ret;
	}
	
}
