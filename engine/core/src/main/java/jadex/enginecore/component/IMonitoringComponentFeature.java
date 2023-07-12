package jadex.enginecore.component;

import jadex.enginecore.service.types.monitoring.IMonitoringService.PublishEventLevel;
import jadex.enginecore.service.types.monitoring.IMonitoringService.PublishTarget;

/**
 * 
 */
public interface IMonitoringComponentFeature extends IExternalMonitoringComponentFeature
{
	/**
	 *  Check if event targets exist.
	 */
	public boolean hasEventTargets(PublishTarget pt, PublishEventLevel pi);
}
