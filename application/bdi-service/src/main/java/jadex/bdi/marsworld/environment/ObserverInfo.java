package jadex.bdi.marsworld.environment;

import java.util.Set;

import jadex.future.SubscriptionIntermediateFuture;

public class ObserverInfo 
{
	protected SubscriptionIntermediateFuture<? extends EnvironmentEvent> observer;
	
	protected SpaceObject obj;
	
	protected Set<SpaceObject> lastvision;

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

	public Set<SpaceObject> getLastVision() 
	{
		return lastvision;
	}

	public void setLastVision(Set<SpaceObject> lastvision) 
	{
		this.lastvision = lastvision;
	}
}