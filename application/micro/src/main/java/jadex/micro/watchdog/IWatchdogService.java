package jadex.micro.watchdog;

import jadex.future.IFuture;
import jadex.mj.feature.providedservice.annotation.Service;

/**
 *  Watchdogs observe each other and take actions
 *  when a watchdog becomes unavailable.
 */
@Service
public interface IWatchdogService
{
	/**
	 *  Test if this watchdog is alive.
	 */
	public IFuture<Void> ping();
}
