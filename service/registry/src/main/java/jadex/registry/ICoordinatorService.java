package jadex.registry;

import java.util.StringTokenizer;

import jadex.common.ClassInfo;
import jadex.common.SReflect;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.impl.GlobalProcessIdentifier;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.ServiceScope;
import jadex.providedservice.annotation.Service;
import jadex.providedservice.impl.service.ServiceIdentifier;
import jadex.requiredservice.IRequiredServiceFeature;

@Service
public interface ICoordinatorService 
{
	public static final String COORDINATOR_SERVICE_NAMES = "jadex.coordinatornames";
	
	/** Name of the remote registry component and service. */
	public static final String REGISTRY_COORDINATOR_NAME = "RegistryCoordinator";
	
	public static final String REGISTRY_COORDINATOR_LOCAL = ICoordinatorService.REGISTRY_COORDINATOR_NAME+"@"+GlobalProcessIdentifier.getSelf();
	
	/**
	 *  Initiates the client registration procedure
	 *  (super peer will answer initially with an empty intermediate result,
	 *  client will send updates with backward commands).
	 *  
	 *  @param networkname	Network for this connection. 
	 *  
	 *  @return Does not return any more results while connection is running.
	 */
	public ISubscriptionIntermediateFuture<Void> registerRegistry(IServiceIdentifier reg, long starttime);
	

	public ISubscriptionIntermediateFuture<CoordinatorServiceEvent> getRegistries();
	
	
	public static ICoordinatorService getCoordinatorServiceProxy(IComponent agent, String name)
    {
    	StringTokenizer stok = new StringTokenizer(name, "@");
    	String agentname = stok.nextToken();
    	String pid = stok.nextToken();
    	String hostname = stok.nextToken();
    	ComponentIdentifier copid = new ComponentIdentifier(agentname, pid, hostname);
    	
    	IServiceIdentifier rrsid = new ServiceIdentifier(
			copid,//new ComponentIdentifier(IRegistryCoordinatorService.REGISTRY_COORDINATOR_NAME), // providerid
			new ClassInfo(ICoordinatorService.class), //type
			null, // supertypes
			SReflect.getUnqualifiedClassName(ICoordinatorService.class),
			//IRegistryCoordinatorService.REGISTRY_COORDINATOR_NAME, // sername
			ServiceScope.GLOBAL, // scope
			null, // networknames
			true, // unrestricted
			null); 
		
    	// Can null when service is local and not available.
    	ICoordinatorService ret = (ICoordinatorService)agent.getFeature(IRequiredServiceFeature.class).getServiceProxy(rrsid);
    	
    	//System.out.println("proxy for coordinator service: "+ret);
    	
		return ret;
    }
	
	public static String[] getCoordinatorServiceNames()
	{
		String coname = System.getProperty(ICoordinatorService.COORDINATOR_SERVICE_NAMES);
		if(coname == null)
			coname = System.getenv(ICoordinatorService.COORDINATOR_SERVICE_NAMES);
		
		if(coname != null)
		{
			StringTokenizer stok = new StringTokenizer(coname, ",");
			String[] names = new String[stok.countTokens()];
			int i = 0;
			while(stok.hasMoreTokens())
			{
				names[i++] = stok.nextToken().trim();
			}
			return names;
		}
		else
		{
			return new String[]{ICoordinatorService.REGISTRY_COORDINATOR_LOCAL};
		}
	}
}
