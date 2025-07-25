package jadex.registry.remote;

import java.lang.System.Logger.Level;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import jadex.collection.MultiCollection;
import jadex.common.ClassInfo;
import jadex.common.ICommand;
import jadex.common.IFilter;
import jadex.common.SReflect;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.impl.GlobalProcessIdentifier;
import jadex.execution.IExecutionFeature;
import jadex.execution.impl.ExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.IntermediateEmptyResultListener;
import jadex.future.SubscriptionIntermediateDelegationFuture;
import jadex.future.SubscriptionIntermediateFuture;
import jadex.future.TerminationCommand;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;
import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.ServiceScope;
import jadex.providedservice.impl.search.IServiceRegistry;
import jadex.providedservice.impl.search.QueryEvent;
import jadex.providedservice.impl.search.ServiceEvent;
import jadex.providedservice.impl.search.ServiceQuery;
import jadex.providedservice.impl.search.ServiceQueryInfo;
import jadex.providedservice.impl.search.ServiceRegistry;
import jadex.providedservice.impl.service.ServiceCall;
import jadex.providedservice.impl.service.ServiceIdentifier;
import jadex.registry.coordinator.ICoordinatorService;
import jadex.remoteservice.impl.RemoteMethodInvocationHandler;
import jadex.requiredservice.IRequiredServiceFeature;
import jadex.requiredservice.ServiceNotFoundException;


/**
 *  Registry collects services from client and answers search requests and queries.
 */
public class RemoteRegistryAgent implements IRemoteRegistryService
{
	/** Connection delay*/
    protected long delay = 10000;
	
	/** The agent. */
	@Inject
	protected IComponent agent;
	
	@Inject
	protected IServiceIdentifier sid;
	
	protected long starttime = System.currentTimeMillis();
	
	/** The superpeer service registry */
	protected IServiceRegistry serviceregistry = new ServiceRegistry();
	
	//protected boolean unrestricted = false;
	
	/** Queries received from client. */
	protected MultiCollection<ComponentIdentifier, ServiceQueryInfo<?>> clientqueries = new MultiCollection<>();
	
	// getRegisteredClients() futures
	protected Set<SubscriptionIntermediateFuture<ComponentIdentifier>> reglisteners = new LinkedHashSet<>();
	
	protected Set<ComponentIdentifier> clients = new LinkedHashSet<>();
	
	protected List<String> coordinatornames = List.of(ICoordinatorService.getCoordinatorServiceNames());
	
	protected Map<String, ISubscriptionIntermediateFuture<Void>> coordinators = new HashMap<>();
	
	@OnStart
    public void start() 
    {
		System.getLogger(getClass().getName()).log(Level.INFO, "Registry started: "+agent.getId());
		
		for(String coname: coordinatornames)
			connectToCoordinator(coname);
    }
    
    protected void connectToCoordinator(String coname)
    {
    	Runnable reconnect = () ->
		{
			System.getLogger(getClass().getName()).log(Level.INFO, "Reconnecting to coordinator: "+coname+" "+agent.getId());
    		agent.getFeature(IExecutionFeature.class).waitForDelay(delay)
    			.then(x -> agent.getFeature(IExecutionFeature.class).scheduleStep(() -> connectToCoordinator(coname)))
    			.printOnEx();
		};
    	
    	ISubscriptionIntermediateFuture<Void> cosub = coordinators.get(coname);
    	if(cosub!=null)
    		cosub.terminate();    
	
    	ICoordinatorService coser = ICoordinatorService.getCoordinatorServiceProxy(agent, coname);
    	
    	if(coser==null)
		{
			System.out.println("Could not get coordinator service proxy: "+coname+" "+agent.getId());
			reconnect.run();
			return;
		}
    	
    	System.out.println("Connecting to coordinator: "+agent.getId()+" "+coser);
		
    	cosub = coser.registerRegistry(sid, starttime);
    	coordinators.put(coname, cosub);
    	cosub.next(ignore ->
    	{
    		System.out.println("Connected to coordinator: "+agent.getId()+" "+coser);
    	})
    	.finished(Void ->
        {
        	System.getLogger(getClass().getName()).log(Level.INFO, agent + ": Subscription to coordinator finished.");
        	reconnect.run();
        })
    	.catchEx(ex ->
    	{
    		System.out.println("Could not connect to coordinator: "+coser+" "+ex);
    		reconnect.run();
    	});
    }
	
	/**
	 *  Initiates the client registration procedure
	 *  (super peer will answer initially with an empty intermediate result,
	 *  client will send updates with backward commands).
	 *  
	 *  @param networkname	Network for this connection. 
	 *  
	 *  @return Does not return any more results while connection is running.
	 */
	// TODO: replace internal commands with typed channel (i.e. bidirectional / reverse subscription future)
	// TODO: network name required for server?
	public ISubscriptionIntermediateFuture<Void> registerClient()//String networkname)
	{
		final ComponentIdentifier client = ServiceCall.getCurrentInvocation().getCaller();
		clients.add(client);
		System.getLogger(getClass().getName()).log(Level.INFO, "Client added: "+client);//+" "+networkname);
		
		// Listener notification as step to improve test behavior (e.g. AbstractSearchQueryTest)
		agent.getFeature(IExecutionFeature.class).scheduleStep(() ->
		{
			System.out.println("Initiated registry connection with client "+agent+" "+client);//+" for network "+networkname);
			for(SubscriptionIntermediateFuture<ComponentIdentifier> reglis: reglisteners)
			{
				System.getLogger(getClass().getName()).log(Level.INFO, "new connection: "+client);
				reglis.addIntermediateResult(client);
			}
		});
		
		SubscriptionIntermediateFuture<Void> ret = new SubscriptionIntermediateFuture<>(new TerminationCommand()
		{
			@Override
			public void terminated(Exception reason)
			{
				//System.out.println(agent+": Super peer connection with client "+client+" for network "+networkname+" terminated due to "+reason+(reason!=null?"/"+reason.getCause():""));
				System.getLogger(getClass().getName()).log(Level.INFO, agent+": Registry connection with client "+client+" terminated due to "+reason+(reason!=null?"/"+reason.getCause():""));
				// TODO: when connection is lost, remove all services and queries from client.
				// FIXME: Terminate on error/timeout?
				clients.remove(client);
				clientqueries.remove(client);
				serviceregistry.removeQueriesOfRuntime(client); // client.getRoot()
				serviceregistry.removeServices(client); // client.getRoot()
				
				/**for (IServiceRegistry reg : getApplicablePeers(null))
				{
					reg.removeQueriesOfPlatform(client.getRoot());
					reg.removeServices(client.getRoot());
				}*/
			}
		});
		
		//SFuture.avoidCallTimeouts(ret, agent);
		
		// Initial register-ok response
		ret.addIntermediateResult(null);
		
		// TODO: listen for changes and add new services locally.
		ret.addBackwardCommand(new IFilter<Object>()
		{
			public boolean filter(Object obj)
			{
				return obj instanceof ServiceEvent;
			}
		}, new ICommand<Object>()
		{
			public void execute(Object obj)
			{
				System.getLogger(getClass().getName()).log(Level.INFO, "Remote registry received client event: "+obj);
				ServiceEvent event = (ServiceEvent) obj;
				
				//if(debug(event.getService()))
				System.out.println(agent+" received client event: "+event);
					
				// propagate service changes to the registry
				dispatchEventToRegistry(serviceregistry, event);
			}
		});
		
		return ret;
	}
	
	/**
	 *  Search remote registry for a single service.
	 *  
	 *  @param query The search query.
	 *  @return The first matching service or null if not found.
	 */
	public IFuture<IServiceIdentifier> searchService(ServiceQuery<?> query)
	{
		//if(serviceregistry.getAllServices().size()>0)
		System.out.println("Remote registry searchService: "+query+" "+serviceregistry.getAllServices());
		
		IServiceIdentifier ret = serviceregistry.searchService(query);
		/*if(ret == null)
		{
			Iterator<IServiceRegistry> it = getApplicablePeers(query).iterator();			
			while(ret==null && it.hasNext())
			{
				IServiceRegistry reg = it.next();
				ret = reg.searchService(query);
			}
		}*/
		
		return ret==null && query.getMultiplicity().getFrom()!=0
			? new Future<>(new ServiceNotFoundException(query))
		    : new Future<>(ret);
	}
	
	/**
	 *  Search remote registry for services.
	 *  
	 *  @param query The search query.
	 *  @return The matching services or empty set if none are found.
	 */
	public IFuture<Set<IServiceIdentifier>> searchServices(ServiceQuery<?> query)
	{
		Set<IServiceIdentifier> ret = serviceregistry.searchServices(query);
		// Adding to set is allowed, registry returns copy...
		//for (IServiceRegistry reg : getApplicablePeers(query))
		//	ret.addAll(reg.searchServices(query));
		return new Future<>(ret);
	}
	
	/**
	 *  Add a service query to the registry.
	 *  
	 *  @param query The service query.
	 *  @return Subscription to matching services.
	 */
	public <T> ISubscriptionIntermediateFuture<Object> addQuery(ServiceQuery<T> query)
	{
		System.getLogger(getClass().getName()).log(Level.INFO, "addQuery: "+query);
		
		final ComponentIdentifier client = ServiceCall.getCurrentInvocation().getCaller();
		final SubscriptionIntermediateFuture<Object> ret = new SubscriptionIntermediateFuture<>();
		
		ServiceQueryInfo<T> info = new ServiceQueryInfo<>(query, ret);
		clientqueries.add(client, info);
		
		/*Set<IServiceRegistry> peercaches = getApplicablePeers(query);
		for (IServiceRegistry peercache : peercaches)
		{
			peercache.addQuery(query).addResultListener(new IntermediateEmptyResultListener<T>()
			{
				public void intermediateResultAvailable(T result)
				{
					ret.addIntermediateResultIfUndone(result);
				}
			});
		}*/
		
		ret.setTerminationCommand(ex -> doRemoveQuery(client, query));
		
		serviceregistry.addQuery(query).addResultListener(new IntermediateEmptyResultListener<Object>()
		{
			public void exceptionOccurred(Exception exception)
			{
				finished();
			}

			public void intermediateResultAvailable(Object result)
			{
				ret.addIntermediateResultIfUndone(result);
			}

			public void finished()
			{
				doRemoveQuery(client, query);
			}
		});
		
		//SFuture.avoidCallTimeouts(ret, agent.getExternalAccess());
		
		return ret;
	}
	
	/**
	 *  Removes a service query from the registry.
	 *  
	 *  @param query The service query.
	 *  @return Null, when done.
	 */
//	public <T> IFuture<Void> removeQuery(ServiceQuery<T> query)
//	{
//		IComponentIdentifier client = ServiceCall.getCurrentInvocation().getCaller();
//		
//		doRemoveQuery(client, query);
//		
//		return IFuture.DONE;
//	}
	
	/**
	 *  Search superpeer for a single service, restricted to the called superpeer.
	 *  
	 *  @param query The search query.
	 *  @return The first matching service or null if not found.
	 */
	public IFuture<IServiceIdentifier> intransitiveSearchService(ServiceQuery<?> query)
	{
		return new Future<>(serviceregistry.searchService(query));
	}
	
	/**
	 *  Search superpeer for services, restricted to the called superpeer.
	 *  
	 *  @param query The search query.
	 *  @return The matching services or empty set if none are found.
	 */
	public IFuture<Set<IServiceIdentifier>> intransitiveSearchServices(ServiceQuery<?> query)
	{
		return new Future<>(serviceregistry.searchServices(query));
	}
	
	/**
	 *  Add a service query to the superpeer registry only.
	 *  
	 *  @param query The service query.
	 *  @return Subscription to matching services.
	 */
	public <T> ISubscriptionIntermediateFuture<Object> addIntransitiveQuery(ServiceQuery<T> query)
	{
		return serviceregistry.addQuery(query);
	}
	
	protected void doRemoveQuery(ComponentIdentifier client, ServiceQuery<?> query)
	{
		//Set<IServiceRegistry> peercaches = getApplicablePeers(query);
		//for (IServiceRegistry peercache : peercaches)
		//	peercache.removeQuery(query);
		
		serviceregistry.removeQuery(query);
		
		clientqueries.removeObject(client, query);
	}
	
	/**
	 *  Dispatches a service event to a target registry.
	 *  
	 *  @param registry The registry.
	 *  @param event The service event.
	 */
	protected void dispatchEventToRegistry(IServiceRegistry registry, ServiceEvent event)
	{
		switch(event.getType())
		{
			case ServiceEvent.SERVICE_ADDED:
				System.out.println("Remote Registry adding service: " + event.getService().getServiceType()+" "+event.getService().getGroupNames());
				registry.addService(event.getService());
//				if(event.toString().indexOf("ITestService")!=-1)
//					System.out.println(agent+" added service: " + event.getService());
				break;
				
			case ServiceEvent.SERVICE_CHANGED:
				registry.updateService(event.getService());
				break;
				
			case ServiceEvent.SERVICE_REMOVED:
				registry.removeService(event.getService());
				break;
				
			default:
				System.getLogger(getClass().getName()).log(Level.ERROR, "Unknown ServiceEvent: " + event.getType());
		}
	}
	
	//-------- superpeer status service --------
		
	/**
	 *  Get the clients that are currently registered to super peer.
	 */
	public ISubscriptionIntermediateFuture<ComponentIdentifier> getRegisteredClients()
	{
		SubscriptionIntermediateFuture<ComponentIdentifier> reglis = new SubscriptionIntermediateFuture<>();
		reglis.setTerminationCommand(ex ->  reglisteners.remove(reglis));
		
		reglisteners.add(reglis);
		
		for(ComponentIdentifier client: clients)
		{
			System.getLogger(getClass().getName()).log(Level.INFO, "new connection: "+client+", "+reglis+", "+ExecutionFeature.LOCAL.get());
			reglis.addIntermediateResult(client);
		}
		
		//SFuture.avoidCallTimeouts(reglis, agent);
		return reglis;
	}

	/**
	 *  Get registered queries.
	 *  @return A stream of events for added/removed queries.
	 */
	public ISubscriptionIntermediateFuture<QueryEvent> subscribeToQueries()
	{
		ISubscriptionIntermediateFuture<QueryEvent>	fut	= serviceregistry.subscribeToQueries();
		SubscriptionIntermediateDelegationFuture<QueryEvent> ret = new SubscriptionIntermediateDelegationFuture<QueryEvent>(fut);
		//SFuture.avoidCallTimeouts(ret, agent);
		return ret;
	}
}