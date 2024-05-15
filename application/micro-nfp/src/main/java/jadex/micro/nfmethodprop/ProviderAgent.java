package jadex.micro.nfmethodprop;

import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.future.IFuture;
import jadex.micro.annotation.Agent;
import jadex.providedservice.annotation.ProvidedService;
import jadex.providedservice.annotation.ProvidedServices;
import jadex.providedservice.annotation.Service;

/**
 * 
 */
@Agent
@Service
@ProvidedServices(@ProvidedService(type=ITestService.class))
public class ProviderAgent implements ITestService
{
	@Agent
	protected IComponent agent;
	
	/**
	 * 
	 */
//	@NFProperties(@NFProperty(value=WaitingTimeProperty.class))
	public IFuture<Void> methodA(long wait)
	{
//		System.out.println("methodA impl called: "+wait);
		IFuture<Void> ret = agent.getFeature(IExecutionFeature.class).waitForDelay(wait);
		ret.then(a -> System.out.println("wait finished")).catchEx(ex -> ex.printStackTrace());
		return ret;
	}
	
	/**
	 * 
	 */
//	@NFProperties(@NFProperty(value=WaitingTimeProperty.class))
	public IFuture<Void> methodB(long wait)
	{
		return methodA(wait);
	}
}
