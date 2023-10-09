package jadex.micro.tutorial.a7;

import jadex.future.IFuture;
import jadex.mj.feature.providedservice.annotation.Service;
import jadex.mj.micro.annotation.Agent;

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
