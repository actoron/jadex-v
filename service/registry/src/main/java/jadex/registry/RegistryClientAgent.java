package jadex.registry;

import java.lang.System.Logger.Level;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jadex.core.IComponent;
import jadex.core.impl.Component;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.FutureTerminatedException;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.ITerminableFuture;
import jadex.future.ITerminableIntermediateFuture;
import jadex.future.SubscriptionIntermediateFuture;
import jadex.future.TerminableFuture;
import jadex.future.TerminableIntermediateFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnEnd;
import jadex.injection.annotation.OnStart;
import jadex.providedservice.IService;
import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.ServiceScope;
import jadex.providedservice.impl.search.ServiceEvent;
import jadex.providedservice.impl.search.ServiceQuery;
import jadex.providedservice.impl.search.ServiceRegistry;
import jadex.requiredservice.IRequiredServiceFeature;
import jadex.remoteservice.impl.RemoteMethodInvocationHandler;

public class RegistryClientAgent implements IRegistryClientService 
{
	/** Connection delay*/
    protected long delay = 10000;

	
    @Inject
    protected IComponent agent;

    /** Track currently known registries and their metadata (e.g. start time). */
    protected Set<RegistryInfo> registries = new HashSet<>();

    
    /** The connected coordinator service. */
    protected ICoordinatorService coordinator;

    /** Subscription to coordinator. */
    protected ISubscriptionIntermediateFuture<CoordinatorServiceEvent> cosub;
 
    
    /** The connected registry service. */
    protected IRemoteRegistryService registry;

    /** Subscription to coordinator. */
    protected ISubscriptionIntermediateFuture<Void> regsub;

    protected Future<IRemoteRegistryService> registryfut;
    
    
    protected IFuture<Void> evalfut;
    
    protected Set<QueryManager<?>> querymanagers = new HashSet<>();;
    
    protected List<String> coordinatornames = List.of(ICoordinatorService.getCoordinatorServiceNames());
        
    @OnStart
    public void start() 
    {
    	System.getLogger(getClass().getName()).log(Level.INFO, "Registry client started: "+agent.getId()+" "+coordinatornames);
    	connectToCoordinator(0);
    }
    
    protected void connectToCoordinator(int idx)
    {
    	Runnable reconnect = () ->
		{
			cosub = null;
    		agent.getFeature(IExecutionFeature.class).waitForDelay(delay)
    			.then(x -> agent.getFeature(IExecutionFeature.class).scheduleStep(() -> connectToCoordinator((idx+1)%coordinatornames.size())))
    			.printOnEx();
		};
    	
    	if(cosub!=null)
    		cosub.terminate();    
		
		coordinator = ICoordinatorService.getCoordinatorServiceProxy(agent, coordinatornames.get(idx));
		
		// Can happen in local case when service is not (yet) available.
		if(coordinator==null)
		{
			System.out.println("Could not connect to local coordinator: "+coordinatornames.get(idx)+" "+agent.getId());
			reconnect.run();
		}
		else
		{
	    	cosub = coordinator.getRegistries();
	    	cosub.next(event ->
	    	{
	    		System.out.println("Client connected to coordinator: "+agent.getId()+" "+event);
	    		
	    		if(ServiceEvent.SERVICE_ADDED==event.getType()) 
	        	{
	    			registries.add(new RegistryInfo(event.getService(), event.getStartTime()));
	        	}
	        	else if(ServiceEvent.SERVICE_REMOVED==event.getType())
	        	{
	        		registries.removeIf(ri -> ri.serviceid().equals(event.getService()));
	        	}
	        	else if(ServiceEvent.SERVICE_CHANGED==event.getType())
	        	{
	        		registries.removeIf(ri -> ri.serviceid().equals(event.getService()));
	        		registries.add(new RegistryInfo(event.getService(), event.getStartTime()));
	        	}
	        	else
	        	{
	                System.getLogger(getClass().getName()).log(Level.WARNING, "Unknown event type: " + event);
	        	}
	    		
	    		initRegistryReevaluation();
	    	})
	    	.finished(Void ->
	        {
	        	System.getLogger(getClass().getName()).log(Level.INFO, agent + ": Subscription to coordinator finished.");
	        	reconnect.run();
	        })
	    	.catchEx(ex ->
	    	{
	    		System.out.println("Could not connect to coordinator: "+coordinator+" "+ex);
	    		reconnect.run();
	    	});
		}
    }
   
    protected IFuture<Void> initRegistryReevaluation()
    {
    	if(evalfut==null || evalfut.isDone())
    	{
	    	evalfut = agent.getFeature(IExecutionFeature.class).waitForDelay(5000);
	    	
	    	evalfut.then(a->
	    	{
	    		RegistryInfo ri = evaluateRegistries();
	    		System.out.println("Registry reevaluation: "+agent.getId()+" "+ri);
	    		
	    		if (ri == null) 
	    			return;
	    		
	    		// check if current registry is still ok
	    		if(registry!=null && ((IService)registry).getServiceId().equals(ri.serviceid()))
	    			return;
	    		
	    		System.out.println("Found new best registry: "+ri);

	    		// terminate old registry
	    		if(regsub!=null)
	    			regsub.terminate();
	    		
	    		// create service proxy for new registry
	    	  	IRemoteRegistryService rreg = (IRemoteRegistryService)agent.getFeature(IRequiredServiceFeature.class).getServiceProxy(ri.serviceid());
	    	  	setRegistry(rreg);
	    	  	
	    		// connect to new registry
	    		regsub = registry.registerClient();
	    		
	    		regsub.next(x ->
	    		{
	    			System.out.println("Registry client successfully registered with registry: "+agent.getId()+" "+registry);
	    			
		    		// Local query uses registry directly (w/o feature) -> only service identifiers needed and also removed events
		    		ServiceQuery<ServiceEvent> lquery = new ServiceQuery<>((Class<IServiceIdentifier>)null)
		    			.setEventMode()
		    			.setOwner(agent.getId())
		    			.setScope(ServiceScope.GLOBAL)
		    			.setNetworkNames((String[])null);
		    			//.setSearchStart(spid);	// Only find services that are visible to SP
		    		
		    		ISubscriptionIntermediateFuture<ServiceEvent> localquery = (ISubscriptionIntermediateFuture)ServiceRegistry.getRegistry().addQuery(lquery);									

		    		localquery.next(event ->
		    		{
		    			System.out.println("Registry client received service event from internal registry: "+agent.getId()+" "+event+" "+event.getService().getScope());
		    			
		    			if(ServiceScope.GLOBAL.equals(event.getService().getScope())
		    				|| ServiceScope.HOST.equals(event.getService().getScope()))
		    			{
		    				agent.getFeature(IExecutionFeature.class).scheduleStep(agent ->
		    				{
		    					try
    							{
	    							System.out.println("Registry client sending service event to registry "+registry+": "+event);
    								regsub.sendBackwardCommand(event);
    							}
    							catch (Exception e)
    							{
    								e.printStackTrace();
    								initRegistryReevaluation();
    							}
		    				});
	    				}
		    		}).finished(result ->
		    		{
		    			System.out.println("Service event query finished!?: "+result);
	    				// Should not happen?
	    				assert false;
		    		}).catchEx(ex ->
		    		{
		    			assert ex instanceof FutureTerminatedException : ex;
		    		});
	    		});
	    		
	    		evalfut = null;
	    		
	    	}).printOnEx();
    	}
    	
    	return evalfut;
    }
    
    protected RegistryInfo evaluateRegistries()
    {
    	// Order registries by 
    	// a) tag or explicit registry wish?! // todo: how to specify? setRegistryWish() e.g. on IRemoteRegistryService?
    	// b) global Jadex registry
    	// c) oldest registry (longest alive)
    	
    	// todo: a) and b)
    	
    	List<RegistryInfo> sorted = registries.stream().sorted().collect(Collectors.toList());
    	
    	if(sorted.isEmpty()) 
    	{
    	    System.getLogger(getClass().getName()).log(Level.WARNING, "No registries available for evaluation.");
    	    return null;
    	}
    	
    	return sorted.get(0);
    }
    
    protected IFuture<IRemoteRegistryService> getRegistryService()
    {
    	if(registryfut==null)
    		registryfut = new Future<>();
    	
    	if(registry!=null && !registryfut.isDone())
    	{
    		//System.out.println("registryfut set: "+registry);
    		registryfut.setResult(registry);
    	}
    	else if(registry==null)
    	{
    		System.out.println("Registry client has no registry, reevaluating registries: "+agent.getId());
    		initRegistryReevaluation();
    	}
    	
    	return registryfut;
    }
    
    protected void setRegistry(IRemoteRegistryService registry)
    {
    	System.out.println("setRegistryService: "+agent.getId()+" "+registry);
    	
    	this.registry = registry;
    	
    	for(QueryManager<?> qman: querymanagers)
    	{
    		qman.updateQuery();
    	}
    	
    	//System.out.println("registryfut is: "+registryfut);
    	if(registryfut!=null)
    	{
    		if(registryfut.isDone())
    		{
    			if(!registryfut.get().equals(registry))
    				registryfut = new Future<>();
    		}
    		if(!registryfut.isDone())
    			registryfut.setResult(registry);
    	}
    }
    
    /**
	 *  Search for matching services using available remote information sources and provide first result.
	 *  @param query	The search query.
	 *  @return Future providing the corresponding service or ServiceNotFoundException when not found.
	 */
	public <T> ITerminableFuture<IServiceIdentifier> searchService(ServiceQuery<T> query)
	{
		System.out.println("RegistryClient: searching for service 1: "+agent.getId()+" "+query);
		TerminableFuture<IServiceIdentifier> ret = new TerminableFuture<>();
		getRegistryService().then(regser ->
		{
			System.out.println("RegistryClient: searching for service 2: "+agent.getId()+" "+query+" "+regser);
			regser.searchService(query).delegateTo(ret);
		}).catchEx(ret).printOnEx();
		return ret;
	}
	
	/**
	 *  Search for all matching services.
	 *  @param query	The search query.
	 *  @return Each service as an intermediate result or a collection of services as final result.
	 */
	public <T> ITerminableIntermediateFuture<IServiceIdentifier> searchServices(ServiceQuery<T> query)
	{
		TerminableIntermediateFuture<IServiceIdentifier> ret = new TerminableIntermediateFuture<>();
		getRegistryService().then(regser ->
		{
			IFuture<Set<IServiceIdentifier>> fut = regser.searchServices(query);
			fut.then(result -> 
			{
				for(IServiceIdentifier sid: result)
					ret.addIntermediateResult(sid);
			}).catchEx(ret);
		}).catchEx(ret);
		return ret;
	}
	
	/**
	 *  Add a service query.
	 *  Continuously searches for matching services using available remote information sources.
	 *  @param query	The search query.
	 *  @return Future providing the corresponding services as intermediate results.
	 */
	public <T> ISubscriptionIntermediateFuture<T> addQuery(ServiceQuery<T> query)
	{
		QueryManager<T> qman = new QueryManager<T>(query);
		
		querymanagers.add(qman);
		
		return qman.getReturnFuture();
	}
	
	/**
	 *  Get a remote service proxy.
	 *  @param sid The service id.
	 *  @return The service.
	 */
	public IFuture<IService> getRemoteServiceProxy(IComponent agent, IServiceIdentifier sid)
	{
		return new Future<>(RemoteMethodInvocationHandler.createRemoteServiceProxy(agent, sid));
	}
    
    @OnEnd
    public void stop() 
    {
    	if (regsub != null) 
        {
            regsub.terminate();
            regsub = null;
        }
    	
        if (cosub != null) 
        {
            cosub.terminate();
            cosub = null;
        }
        
        for(QueryManager<?> qman: querymanagers)
        {
        	qman.getReturnFuture().terminate();
        }
        
        registries.clear();
    }
    
    /**
	 *  Query manager.
	 */
	protected class QueryManager<T>
	{
		//-------- attributes --------
		
		/** The query itself. */
		protected ServiceQuery<T> query;
		
		/** The return future to the user. */
		protected SubscriptionIntermediateFuture<T> retfut;
		
		/** The auxiliary futures as received from remote registry. */
		protected ITerminableIntermediateFuture<Object> future;
		
		/** Filter for known results when not in event mode. */ 
		protected SlidingCuckooFilter filter;
		
		/** The current result set when in events mode. */ 
		protected Set<IServiceIdentifier> results;
		
		protected IRemoteRegistryService registryser;
		
		protected int retrycnt;
		
		//-------- constructors --------
		
		/**
		 *  Create a query info.
		 */
		protected QueryManager(ServiceQuery<T> query)
		{
			this.query	= query;
			this.retfut	= new SubscriptionIntermediateFuture<>();
			//SFuture.avoidCallTimeouts(retfut, agent);
			
			filter	= query.isEventMode() ? null : new SlidingCuckooFilter();
			results	= query.isEventMode() ? new LinkedHashSet<>() : null;

			// Start handling
//			updateQuery(getSearchableNetworks(query));
			//String[] networknames = getQueryNetworks(query);
			updateQuery();
			
			retfut.setTerminationCommand(ex ->
			{
				future.terminate();
				querymanagers.remove(this);
			});
		}
		
		//-------- methods --------
		
		/**
		 *  The return future for the user containing all the collected results from the internal queries.
		 */
		public ISubscriptionIntermediateFuture<T> getReturnFuture()
		{
			return retfut;
		}
		
		//-------- internal methods --------
		
		/**
		 *  Add/update query connections to relevant super peers for given networks.
		 */
		protected void updateQuery()
		{
			// Ignore when already terminated.
			if(!retfut.isDone())
			{
				getRegistryService().then(regser ->
				{
					if(regser.equals(registryser))
						retrycnt++;
					registryser = regser;
					
					if(future!=null && !future.isDone())
						future.terminate();
					
					future = regser.addQuery(query);
					future.next(result ->
					{
						// Forward result to user query
//						if((""+result).indexOf("ITestService")!=-1)
//						{
							//System.out.println("Received result: "+agent+", "+result+", "+query);
//						}
						// New event result?
						Object	res	= null;
						if(query.isEventMode())
						{
							// Forward event if consistent with current results.
							ServiceEvent	event	= (ServiceEvent)result;
							if(event.getType()==ServiceEvent.SERVICE_ADDED && results.add(event.getService())
								|| event.getType()==ServiceEvent.SERVICE_CHANGED && results.contains(event.getService())
								|| event.getType()==ServiceEvent.SERVICE_REMOVED && results.remove(event.getService()))
							{
//								System.out.println("Received SP event: "+result+"\n\t"+results);
								res = result;
							}
						}
						// New non-event result?
						else if(!filter.contains(result.toString()))
						{
							filter.insert(result.toString());
							res	= result;
						}	
					
						if(res!=null)
						{									
							// Forward result to user query
							@SuppressWarnings({"unchecked"})
							SubscriptionIntermediateFuture<Object> rawfut = (SubscriptionIntermediateFuture<Object>)retfut;
							rawfut.addIntermediateResultIfUndone(res);
						}
					}).catchEx(ex ->
					{
						// Reconnect query on error, if user query still active
						if(!retfut.isDone())
						{
							if(retrycnt<3)
							{
								long retrydelay = delay / 3 * (retrycnt + 1);
								agent.getFeature(IExecutionFeature.class).waitForDelay(retrydelay)
				        		.then(x -> agent.getFeature(IExecutionFeature.class).scheduleStep(() -> updateQuery()))
				        		.printOnEx();
							}
							else
							{
								System.out.println("Cannot connect to registry, max retries reached, reevaluating registries: "+registryser);
								initRegistryReevaluation();
							}
							
						}
					});
					
				}).catchEx(retfut);
			}
		}
	}	
}
