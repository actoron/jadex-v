package jadex.micro.nfpropreq;

import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.providedservice.IServiceIdentifier;

/**
 * 
 */
public class ProviderAgent implements IAService
{
	@Inject
	protected IComponent agent;
	
	@Inject
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
