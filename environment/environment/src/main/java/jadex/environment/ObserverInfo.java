package jadex.environment;

import jadex.core.ComponentIdentifier;
import jadex.future.SubscriptionIntermediateFuture;

public class ObserverInfo 
{
	protected SubscriptionIntermediateFuture<? extends EnvironmentEvent> observer;
	
	protected ComponentIdentifier observerid;
	
	protected SpaceObject obj;
	
	protected Vision lastvision;

	public ObserverInfo(SubscriptionIntermediateFuture<? extends EnvironmentEvent> observer, ComponentIdentifier observerid, SpaceObject obj) 
	{
		this.observer = observer;
		this.observerid = observerid;
		this.obj = obj;
	}

	public SubscriptionIntermediateFuture<? extends EnvironmentEvent> getObserver() 
	{
		return observer;
	}

	public SpaceObject getSpaceObject() 
	{
		return obj;
	}
	
	public ComponentIdentifier getObserverId() 
	{
		return observerid;
	}

	public Vision getLastVision() 
	{
		return lastvision;
	}

	public void setLastVision(Vision lastvision) 
	{
		this.lastvision = lastvision;
	}
}