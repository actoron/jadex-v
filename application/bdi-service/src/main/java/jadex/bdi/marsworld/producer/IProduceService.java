package jadex.bdi.marsworld.producer;

import jadex.bdi.marsworld.environment.Target;
import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

@Service
public interface IProduceService
{
	public IFuture<Void> doProduce(Target target);
}