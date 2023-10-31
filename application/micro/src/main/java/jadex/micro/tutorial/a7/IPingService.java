package jadex.micro.tutorial.a7;

import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

@Service
public interface IPingService 
{
	public IFuture<Void> ping();
}
