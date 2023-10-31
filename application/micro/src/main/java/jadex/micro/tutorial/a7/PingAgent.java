package jadex.micro.tutorial.a7;

import jadex.future.IFuture;
import jadex.micro.annotation.Agent;
import jadex.providedservice.annotation.Service;

@Agent
@Service
public class PingAgent implements IPingService
{
	@Override
	public IFuture<Void> ping() 
	{
		return IFuture.DONE;
	}
}
