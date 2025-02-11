package jadex.bdi.marsworld.carry;

import jadex.bdi.marsworld.environment.SpaceObject;
import jadex.future.IFuture;

public interface ICarryService 
{
	public IFuture<Void> doCarry(SpaceObject target);
}
