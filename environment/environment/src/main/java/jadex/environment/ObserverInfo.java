package jadex.environment;

import jadex.future.SubscriptionIntermediateFuture;

public class ObserverInfo 
{
	protected SubscriptionIntermediateFuture<? extends EnvironmentEvent> observer;
	
	protected SpaceObject obj;
	
	protected Vision lastvision;

	public ObserverInfo(SubscriptionIntermediateFuture<? extends EnvironmentEvent> observer, SpaceObject obj) 
	{
		this.observer = observer;
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

	public Vision getLastVision() 
	{
		return lastvision;
	}

	public void setLastVision(Vision lastvision) 
	{
		this.lastvision = lastvision;
	}
}