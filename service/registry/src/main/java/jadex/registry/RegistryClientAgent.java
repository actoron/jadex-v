package jadex.registry;

import java.lang.System.Logger.Level;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnEnd;
import jadex.injection.annotation.OnStart;
import jadex.providedservice.impl.search.ServiceEvent;

public class RegistryClientAgent //implements IRegistryClientService 
{
    @Inject
    protected IComponent agent;

    /** Track currently known registries and their metadata (e.g. start time). */
    protected Set<RegistryInfo> registries = new HashSet<>();

    /** Subscription to coordinator. */
    protected ISubscriptionIntermediateFuture<CoordinatorServiceEvent> cosub;

    /** The connected coordinator service. */
    protected IRegistryCoordinatorService coordinator;

    @OnStart
    public void start() 
    {
    	findCoordinator();
        subscribeToCoordinator();
    }

    protected void findCoordinator()
    {
    	String coord = IRegistryCoordinatorService.REGISTRY_COORDINATOR_NAME+"@global@www.actoron.com";
    	StringTokenizer stok = new StringTokenizer(coord, "@");
    	String agentname = stok.nextToken();
    	String pid = stok.nextToken();
    	String hostname = stok.nextToken();
    	
    	/*ComponentIdentifier copid = new ComponentIdentifier(agentname, )
    	
		IServiceIdentifier rrsid = BasicService.createServiceIdentifier(new ComponentIdentifier(IRemoteRegistryService.REMOTE_REGISTRY_NAME, platform), new ClassInfo(IRemoteRegistryService.class), null, IRemoteRegistryService.REMOTE_REGISTRY_NAME, null, ServiceScope.NETWORK, null, true);
		IRemoteRegistryService rrs = (IRemoteRegistryService) RemoteMethodInvocationHandler.createRemoteServiceProxy(agent, rrsid);
    	*/
    }
    
    protected void subscribeToCoordinator() 
    {
        cosub = coordinator.getRegistries();

        cosub.next(event ->
        {
        	if(ServiceEvent.SERVICE_ADDED==event.getType()) 
        	{
        		handleRegistryAdded(event);
        	}
        	else if(ServiceEvent.SERVICE_REMOVED==event.getType())
        	{
        		handleRegistryRemoved(event);
        	}
        	else if(ServiceEvent.SERVICE_CHANGED==event.getType())
        	{
        		handleRegistryRemoved(event);
        		handleRegistryAdded(event);
        	}
        	else
        	{
                System.getLogger(getClass().getName()).log(Level.WARNING, "Unknown event type: " + event);
        	}
        }).finished(Void ->
        {
        	 System.getLogger(getClass().getName()).log(Level.INFO, agent + ": Subscription to coordinator finished.");
        }).printOnEx();
    }

    protected void handleRegistryAdded(CoordinatorServiceEvent event) 
    {
        RegistryInfo ri = new RegistryInfo(event.getService(), event.getStartTime());
        registries.add(ri);

        System.getLogger(getClass().getName()).log(Level.INFO, agent + ": Registry added: " + ri);
        
        // TODO: Optionally connect to that registry / synchronize services etc.
    }

    protected void handleRegistryRemoved(CoordinatorServiceEvent event) 
    {
    	registries.removeIf(ri -> ri.sid().equals(event.getService()));

        System.getLogger(getClass().getName()).log(Level.INFO, agent + ": Registry removed: " + event.getService());
    }

    @OnEnd
    public void stop() 
    {
        if (cosub != null) 
        {
            cosub.terminate();
            cosub = null;
        }
        registries.clear();
    }

    /*public Set<RegistryInfo> getKnownRegistries() 
    {
        return Set.copyOf(registries);
    }*/
}
