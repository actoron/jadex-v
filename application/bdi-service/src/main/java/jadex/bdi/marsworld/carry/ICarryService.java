package jadex.bdi.marsworld.carry;

import jadex.bdi.marsworld.environment.Target;
import jadex.future.IFuture;

public interface ICarryService 
{
	public IFuture<Void> doCarry(Target target);
}
