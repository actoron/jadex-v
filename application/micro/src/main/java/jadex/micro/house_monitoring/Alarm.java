package jadex.micro.house_monitoring;

import java.util.ArrayList;
import java.util.List;

import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.SubscriptionIntermediateFuture;

/**
 *  Component representing the alarm system in the smart home. 
 */
public class Alarm implements IAlarmService
{
	//-------- attributes --------
	
	/** The current state of the alarm system. */
	protected AlarmState	state = AlarmState.OFF;
	
	/** The subscribers to the alarm state. */
	protected List<SubscriptionIntermediateFuture<AlarmState>>	subscribers = new ArrayList<>(); 
	
	//-------- tool methods --------
	
	@Override
	public IFuture<Void> setAlarmState(AlarmState state)
	{
		this.state = state;
		for(SubscriptionIntermediateFuture<AlarmState> subscriber : subscribers)
		{
			try
			{
				subscriber.addIntermediateResult(state);
			}
			catch(Exception e)
			{
				System.err.println("Failed to notify subscriber: "+e);
			}
		}
		return IFuture.DONE;
	}
	
	@Override
	public IFuture<AlarmState> getAlarmState()
	{
		return new Future<>(state);
	}
	
	//-------- UI only methods --------
	
	@Override
	public ISubscriptionIntermediateFuture<AlarmState> subSubcribeToAlarmState()
	{
		SubscriptionIntermediateFuture<AlarmState> subscriber = new SubscriptionIntermediateFuture<>();
		subscriber.setTerminationCommand(ex -> subscribers.remove(subscriber));
		subscriber.addIntermediateResult(state);
		subscribers.add(subscriber);
		return subscriber;
	}
}
