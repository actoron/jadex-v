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

@Publish(publishid="http://${host}:${port}/${cid}/api", publishtarget = ICoordinatorGuiService.class)
public class CoordinatorAgent implements ICoordinatorService, ICoordinatorGuiService
{
	/** The agent. */
	@Inject
	protected IComponent agent;
	
	/** The known registries. */
	protected Set<RegistryInfo> registries = new HashSet<>();
	
	// registerRegistry() futures
	protected Set<SubscriptionIntermediateFuture<CoordinatorServiceEvent>> listeners = new LinkedHashSet<>();
	
	protected String host;
	
	protected int port;
	
	/**
	 *  Creates a new coordinator agent.
	 *  
	 *  @param host The host name.
	 *  @param port The port number.
	 */
	public CoordinatorAgent(String host, int port)
	{
		this.host = host;
		this.port = port;
	}
	
	public CoordinatorAgent()
	{
		this("localhost", 8081);
	}

	@OnStart
	protected void onStart()
	{
		System.getLogger(getClass().getName()).log(Level.INFO, "Coordinator started at "+agent.getId());
		
		IPublishServiceFeature ps = agent.getFeature(IPublishServiceFeature.class);
		ps.publishResources("http://${host}:${port}/${cid}", "jadex/registry/coordinator/ui");
		
		String url = "http://"+host+":"+port+"/"+agent.getId().getLocalName();
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
		System.out.println("Coordinator sending new registry info to clients: "+ri+" "+listeners.size());
		CoordinatorServiceEvent sa = new CoordinatorServiceEvent(reg, registries.contains(ri)? 
			ServiceEvent.SERVICE_CHANGED: ServiceEvent.SERVICE_ADDED, starttime);
		listeners.stream().forEach(lis ->
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
			listeners.stream().forEach(lis ->
			{
				lis.addIntermediateResultIfUndone(sr);
			});
		});
		
		// Initial register-ok response
		ret.addIntermediateResult(null);
		
		return ret;
	}
	
	/**
	 *  Subscribe to coordinator updates.
	 */
	@GET
	public ISubscriptionIntermediateFuture<CoordinatorServiceEvent> subscribe()
	{
		final ComponentIdentifier caller = ServiceCall.getCurrentInvocation().getCaller();
		
		SubscriptionIntermediateFuture<CoordinatorServiceEvent> ret = new SubscriptionIntermediateFuture<>();
		listeners.add(ret);

		ret.setTerminationCommand(ex ->
		{
			System.getLogger(getClass().getName()).log(Level.INFO, agent+": Coordinator UI connection terminated due to "+ex);
			listeners.remove(ret);
		});
		
		System.out.println("subscribed to coordinator initial values "+caller+" "+registries.size()+" "+listeners.size());
		
		for(RegistryInfo reg: registries)
		{
			CoordinatorServiceEvent rse = new CoordinatorServiceEvent(reg.serviceid(), ServiceEvent.SERVICE_ADDED, reg.starttime());
			ret.addIntermediateResult(rse);
		}
		
		if(registries.isEmpty())
		{
			// If no registries are known, send an empty event
			ret.addIntermediateResult(new CoordinatorServiceEvent(null, ServiceEvent.UNKNOWN, 0));
		}
		
		return ret;
	}
	
}
