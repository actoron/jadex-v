package jadex.requiredservicemicro.injections;

import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

@Service
public interface ITestService
{
	public IFuture<Void> method(String msg);
}
