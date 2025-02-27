package jadex.bdi.marsworld.sentry;

import jadex.bdi.marsworld.environment.Target;
import jadex.future.IFuture;

public interface ITargetAnnouncementService
{
	public IFuture<Void> announceNewTarget(Target target);
}
