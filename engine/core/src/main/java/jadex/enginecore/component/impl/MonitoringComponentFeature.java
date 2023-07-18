package jadex.enginecore.component.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jadex.bridge.BulkMonitoringEvent;
import jadex.common.IFilter;
import jadex.common.Tuple2;
import jadex.enginecore.IInternalAccess;
import jadex.enginecore.SFuture;
import jadex.enginecore.component.ComponentCreationInfo;
import jadex.enginecore.component.IExecutionFeature;
import jadex.enginecore.component.IMonitoringComponentFeature;
import jadex.enginecore.service.ServiceScope;
import jadex.enginecore.service.component.IRequiredServicesFeature;
import jadex.enginecore.service.search.ServiceQuery;
import jadex.enginecore.service.types.monitoring.IMonitoringEvent;
import jadex.enginecore.service.types.monitoring.IMonitoringService;
import jadex.enginecore.service.types.monitoring.IMonitoringService.PublishEventLevel;
import jadex.enginecore.service.types.monitoring.IMonitoringService.PublishTarget;
import jadex.enginecore.service.types.monitoring.MonitoringEvent;
import jadex.future.DelegationResultListener;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.ITerminationCommand;
import jadex.future.SubscriptionIntermediateFuture;

/**
 *  Implementation of the monitoring feature.
 */
public class MonitoringComponentFeature extends AbstractComponentFeature implements IMonitoringComponentFeature
{
	/** The subscriptions (subscription future -> subscription info). */
	protected Map<SubscriptionIntermediateFuture<IMonitoringEvent>, Tuple2<IFilter<IMonitoringEvent>, PublishEventLevel>> subscriptions;
	
	/** The monitoring service getter. */
	//protected ServiceGetter<IMonitoringService> getter;
	protected IMonitoringService monser;

	/** The event emit level for subscriptions. */
	protected PublishEventLevel	emitlevelsub;
	
	/**
	 *  Create the feature.
	 */
	public MonitoringComponentFeature(IInternalAccess component, ComponentCreationInfo cinfo)
	{
		super(component, cinfo);
		this.emitlevelsub = cinfo.getComponentDescription().getMonitoring();
		if(emitlevelsub==null)
			emitlevelsub = PublishEventLevel.OFF;
		
//		System.out.println("mon is: "+cinfo.getComponentDescription().getName()+" "+emitlevelsub);
	}
	
	/**
	 *  Execute the main activity of the feature.
	 */
	public IFuture<Void> body()
	{
		// todo?! make all components use the same query via doing the search in platform component and writing result in platform data
		getComponent().getFeature(IRequiredServicesFeature.class).addQuery(new ServiceQuery<IMonitoringService>(IMonitoringService.class).setScope(ServiceScope.PLATFORM))
		.next(monser ->
		{
			System.out.println("setting moser: "+monser);
			this.monser = monser;
		}).catchEx(ex ->
		{
			ex.printStackTrace();
		});
		return IFuture.DONE;
	}
	
	/**
	 *  Publish a monitoring event. This event is automatically send
	 *  to the monitoring service of the platform (if any). 
	 */
	public IFuture<Void> publishEvent(IMonitoringEvent event, PublishTarget pt)
	{
//		if(event.getCause()==null)
//		{
//			ServiceCall call = CallAccess.getCurrentInvocation();
//			if(call!=null)
//			{
////				System.out.println("injecting call cause: "+call.getCause());
//				event.setCause(call.getCause());
//			}
//			else if(getComponent().getDescription().getCause()!=null)
//			{
////				System.out.println("injecting root cause: "+call.getCause());
//				event.setCause(getComponent().getDescription().getCause().createNext());//event.getSourceIdentifier().toString()));
//			}
//		}
		
		// Publish to local subscribers
		publishLocalEvent(event);
		
//		// Publish to monitoring service if monitoring is turned on
//		if((PublishTarget.TOALL.equals(pt) || PublishTarget.TOMONITORING.equals(pt) 
//			&& event.getLevel().getLevel()<=getPublishEmitLevelMonitoring().getLevel()))
//		{
			return publishEvent(event, monser);
//		}
//		else
//		{
//			return IFuture.DONE;
//		}
	}
	
	/**
	 *  Check if the feature potentially executed user code in body.
	 *  Allows blocking operations in user bodies by using separate steps for each feature.
	 *  Non-user-body-features are directly executed for speed.
	 *  If unsure just return true. ;-)
	 */
	public boolean	hasUserBody()
	{
		return false;
	}
	
	/**
	 *  Publish a monitoring event to the monitoring service.
	 */
	public static IFuture<Void> publishEvent(final IMonitoringEvent event, final IMonitoringService monser)
	{
//		return IFuture.DONE;
		
		final Future<Void> ret = new Future<Void>();
		
		if(monser!=null)
		{
//			System.out.println("Published: "+event);
			monser.publishEvent(event).addResultListener(new DelegationResultListener<Void>(ret)
			{
				public void exceptionOccurred(Exception exception)
				{
					//MonitoringComponentFeature.this.monser = null;
					System.out.println("Publish event problem: "+exception);
					ret.setException(exception);
				}
			});
		}
		else
		{
			//System.out.println("No monitoring service to publish event: "+event);
			ret.setResult(null);
		}
		
		return ret;
	}
	
	/**
	 * Get the monitoring event emit level for subscriptions. Is the maximum
	 * level of all subscriptions (cached for speed).
	 */
	public PublishEventLevel getPublishEmitLevelSubscriptions()
	{
		return emitlevelsub;
	}

	/**
	 * Get the monitoring service getter.
	 * 
	 * @return The monitoring service getter.
	 * /
	public ServiceGetter<IMonitoringService> getMonitoringServiceGetter()
	{
		if(getter == null)
			getter = new ServiceGetter<IMonitoringService>(getInternalAccess(), IMonitoringService.class, ServiceScope.PLATFORM);
		return getter;
	}*/

	/**
	 * Forward event to all currently registered subscribers.
	 */
	public void publishLocalEvent(IMonitoringEvent event)
	{
		if(subscriptions != null)
		{
			for(SubscriptionIntermediateFuture<IMonitoringEvent> sub : subscriptions.keySet().toArray(new SubscriptionIntermediateFuture[0]))
			{
				publishLocalEvent(event, sub);
			}
		}
	}

	/**
	 * Forward event to one subscribers.
	 */
	protected void publishLocalEvent(IMonitoringEvent event, SubscriptionIntermediateFuture<IMonitoringEvent> sub)
	{
		Tuple2<IFilter<IMonitoringEvent>, PublishEventLevel> tup = subscriptions.get(sub);
		try
		{
			PublishEventLevel el = tup.getSecondEntity();
			// System.out.println("rec ev: "+event);
			if(event.getLevel().getLevel() <= el.getLevel())
			{
				IFilter<IMonitoringEvent> fil = tup.getFirstEntity();
				if(fil == null || fil.filter(event))
				{
					// System.out.println("forward to: "+event+" "+sub);
					if(!sub.addIntermediateResultIfUndone(event))
					{
						subscriptions.remove(sub);
					}
				}
			}
		}
		catch(Exception e)
		{
			// catch filter exceptions
			e.printStackTrace();
		}
	}

	
	/**
	 *  Check if event targets exist.
	 */
	public boolean hasEventTargets(PublishTarget pt, PublishEventLevel pi)
	{
		boolean ret = false;

		if(pi.getLevel() <= getPublishEmitLevelSubscriptions().getLevel() && (PublishTarget.TOALL.equals(pt) || PublishTarget.TOSUBSCRIBERS.equals(pt)))
		{
			ret = subscriptions != null && !subscriptions.isEmpty();
		}
		if(!ret && pi.getLevel() <= getPublishEmitLevelMonitoring().getLevel() && (PublishTarget.TOALL.equals(pt) || PublishTarget.TOMONITORING.equals(pt)))
		{
			ret = true;
		}

		return ret;
	}
	
	/**
	 * Get the monitoring event emit level.
	 */
	public PublishEventLevel getPublishEmitLevelMonitoring()
	{
		return getComponent().getDescription().getMonitoring() != null ? getComponent().getDescription().getMonitoring() : PublishEventLevel.OFF;
		// return emitlevelmon;
	}
	
	/**
	 * Subscribe to monitoring events.
	 * 
	 * @param filter An optional filter.
	 */
	public ISubscriptionIntermediateFuture<IMonitoringEvent> subscribeToEvents(IFilter<IMonitoringEvent> filter, boolean initial, PublishEventLevel emitlevel)
	{
		final SubscriptionIntermediateFuture<IMonitoringEvent> ret = (SubscriptionIntermediateFuture<IMonitoringEvent>)
			SFuture.getNoTimeoutFuture(SubscriptionIntermediateFuture.class, getInternalAccess());
		
		ITerminationCommand tcom = new ITerminationCommand()
		{
			public void terminated(Exception reason)
			{
				//System.out.println("terminated subscribeToEvents");
				removeSubscription(ret);
			}

			public boolean checkTermination(Exception reason)
			{
				return true;
			}
		};
		ret.setTerminationCommand(tcom);

		// Signal that subscription has been done
		MonitoringEvent subscribed = new MonitoringEvent(getComponent().getId(), getComponent().getDescription().getCreationTime(), 
			IMonitoringEvent.TYPE_SUBSCRIPTION_START, System.currentTimeMillis(),PublishEventLevel.COARSE);
		boolean post = false;
		try
		{
			post = filter == null || filter.filter(subscribed);
		}
		catch(Exception e)
		{
		}
		if(post)
			ret.addIntermediateResult(subscribed);

		addSubscription(ret, filter, emitlevel);

		if(initial)
		{
			List<IMonitoringEvent> evs = getCurrentStateEvents();
			if(evs != null && evs.size() > 0)
			{
				BulkMonitoringEvent bme = new BulkMonitoringEvent(evs.toArray(new IMonitoringEvent[evs.size()]));
				ret.addIntermediateResult(bme);
			}
		}

		return ret;
	}

	/**
	 * Add a new subscription.
	 * 
	 * @param future The subscription future.
	 * @param si The subscription info.
	 */
	protected void addSubscription(SubscriptionIntermediateFuture<IMonitoringEvent> future, IFilter<IMonitoringEvent> filter, PublishEventLevel emitlevel)
	{
		//if(getComponent().getId().toString().toLowerCase().indexOf("cleaner")!=-1)
		//	System.out.println("monitoring add subscription: "+future);
			
		if(subscriptions == null)
			subscriptions = new LinkedHashMap<SubscriptionIntermediateFuture<IMonitoringEvent>, Tuple2<IFilter<IMonitoringEvent>, PublishEventLevel>>();
		if(emitlevel.getLevel() > emitlevelsub.getLevel())
			emitlevelsub = emitlevel;
		subscriptions.put(future, new Tuple2<IFilter<IMonitoringEvent>, PublishEventLevel>(filter, emitlevel));
	}

	/**
	 * Remove an existing subscription.
	 * 
	 * @param fut The subscription future to remove.
	 */
	protected void removeSubscription(SubscriptionIntermediateFuture<IMonitoringEvent> fut)
	{
		//if(getComponent().getId().toString().toLowerCase().indexOf("cleaner")!=-1)
		//	System.out.println("monitoring remove subscription: "+fut);
		
		if(subscriptions == null || !subscriptions.containsKey(fut))
			throw new RuntimeException("Subscriber not known: " + fut);
		subscriptions.remove(fut);
		emitlevelsub = PublishEventLevel.OFF;
		for(Tuple2<IFilter<IMonitoringEvent>, PublishEventLevel> tup : subscriptions.values())
		{
			if(tup.getSecondEntity().getLevel() > emitlevelsub.getLevel())
				emitlevelsub = tup.getSecondEntity();
			if(PublishEventLevel.COARSE.equals(emitlevelsub))
				break;
		}
	}
	
	/**
	 *  Get the current state as events.
	 */
	public List<IMonitoringEvent> getCurrentStateEvents()
	{
		List<IMonitoringEvent> ret = null;
		IExecutionFeature exef = getComponent().getFeature0(IExecutionFeature.class);
		if(exef instanceof ExecutionComponentFeature)
			ret = ((ExecutionComponentFeature)exef).getCurrentStateEvents();
		return ret;
	}
}
