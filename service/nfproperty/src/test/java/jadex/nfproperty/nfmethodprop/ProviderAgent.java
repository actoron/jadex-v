package jadex.nfproperty.nfmethodprop;

import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;

/**
 * 
 */
public class ProviderAgent implements ITestService
{
	@Inject
	protected IComponent agent;
	
//	@NFProperties(@NFProperty(value=WaitingTimeProperty.class))
	public IFuture<Void> methodA(long wait)
	{
//		System.out.println("methodA impl called: "+wait);
		IFuture<Void> ret = agent.getFeature(IExecutionFeature.class).waitForDelay(wait);
		ret.then(a -> System.out.println("wait finished")).catchEx(ex -> ex.printStackTrace());
		return ret;
	}
	
//	@NFProperties(@NFProperty(value=WaitingTimeProperty.class))
	public IFuture<Void> methodB(long wait)
	{
		return methodA(wait);
	}
}
