package jadex.bdi.marsworld;

import jadex.bdi.marsworld.environment.SpaceObject;
import jadex.future.IFuture;

public interface ITargetAnnouncementService
{
	public IFuture<Void> announceNewTarget(SpaceObject target);
}
