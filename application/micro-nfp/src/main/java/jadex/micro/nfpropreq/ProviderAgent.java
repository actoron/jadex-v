package jadex.micro.nfpropreq;

import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.micro.annotation.Agent;
import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.annotation.ProvidedService;
import jadex.providedservice.annotation.ProvidedServices;
import jadex.providedservice.annotation.Service;
import jadex.providedservice.annotation.ServiceIdentifier;

/**
 * 
 */
@Agent
@Service
@ProvidedServices(@ProvidedService(type=IAService.class))
public class ProviderAgent implements IAService
{
	@Agent
	protected IComponent agent;
	
	@ServiceIdentifier
	protected IServiceIdentifier sid;
	
	/** The test string. */
	protected long wait = (long)(Math.random()*1000);
	
	/** The invocation counter. */
	protected int cnt;
	
	/**
	 *  Test method.
	 */
	public IFuture<String> test()
	{
		System.out.println("invoked service: "+sid.getProviderId()+" cnt="+(++cnt)+" wait="+wait);
		agent.getFeature(IExecutionFeature.class).waitForDelay(wait).get();
		return new Future<String>(sid.toString());
	}
}
