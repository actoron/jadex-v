package jadex.enginecore.component;

import jadex.common.IFilter;
import jadex.enginecore.service.types.monitoring.IMonitoringEvent;
import jadex.enginecore.service.types.monitoring.IMonitoringService.PublishEventLevel;
import jadex.enginecore.service.types.monitoring.IMonitoringService.PublishTarget;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;

/**
 *  External perspective of the monitoring feature.
 */
public interface IExternalMonitoringComponentFeature extends IExternalComponentFeature
{
	/**
	 *  Subscribe to component events.
	 *  @param filter An optional filter.
	 *  @param initial True, for receiving the current state.
	 */
//	@Timeout(Timeout.NONE)
	public ISubscriptionIntermediateFuture<IMonitoringEvent> subscribeToEvents(IFilter<IMonitoringEvent> filter, boolean initial, PublishEventLevel elm);

	/**
	 *  Publish a monitoring event. This event is automatically send
	 *  to the monitoring service of the platform (if any). 
	 */
	public IFuture<Void> publishEvent(IMonitoringEvent event, PublishTarget pt);
}
