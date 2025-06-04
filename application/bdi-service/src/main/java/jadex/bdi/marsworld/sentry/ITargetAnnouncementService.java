package jadex.bdi.marsworld.sentry;

import jadex.bdi.marsworld.environment.Target;
import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

@Service
public interface ITargetAnnouncementService
{
	public IFuture<Void> announceNewTarget(Target target);
}
