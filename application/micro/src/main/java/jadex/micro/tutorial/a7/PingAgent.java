package jadex.micro.tutorial.a7;

import jadex.future.IFuture;

public class PingAgent implements IPingService
{
	@Override
	public IFuture<Void> ping() 
	{
		return IFuture.DONE;
	}
}
