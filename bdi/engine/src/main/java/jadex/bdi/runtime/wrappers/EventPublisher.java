package jadex.bdi.runtime.wrappers;

import java.beans.PropertyChangeEvent;

import jadex.bdi.model.MBelief;
import jadex.bdi.model.MElement;
import jadex.bdi.runtime.impl.BDIAgentFeature;
import jadex.bdi.runtime.impl.IInternalBDIAgentFeature;
import jadex.collection.IEventPublisher;
import jadex.common.IResultCommand;
import jadex.core.ComponentTerminatedException;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.future.DelegationResultListener;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.rules.eca.ChangeInfo;
import jadex.rules.eca.Event;
import jadex.rules.eca.EventType;
import jadex.rules.eca.RuleSystem;

/**
 *  Helper object for publishing change events (beliefs, parameters).
 */
public class EventPublisher implements IEventPublisher
{
	/** The agent interpreter. */
	protected IComponent agent;
	
	/** The add event name. */
	protected EventType addevent;
	
	/** The remove event name. */
	protected EventType remevent;
	
	/** The change event name. */
	protected EventType changeevent;
	
	/** The belief model. */
	protected MElement melement;
	
	/** The event adder. */
	protected IResultCommand<IFuture<Void>, PropertyChangeEvent> eventadder;

	/**
	 *  Create a new publisher.
	 */
	public EventPublisher(IComponent agent, EventType changeevent, MElement melement)
	{
		this(agent, null, null, changeevent, melement);
	}
	
	/**
	 *  Create a new publisher.
	 */
	public EventPublisher(IComponent agent, 
		EventType addevent, EventType remevent, EventType changeevent, MElement melement)
	{
		this.agent = agent;
		this.addevent = addevent;
		this.remevent = remevent;
		this.changeevent = changeevent;
		this.melement = melement;
		
		eventadder = new IResultCommand<IFuture<Void>, PropertyChangeEvent>()
		{
			final IResultCommand<IFuture<Void>, PropertyChangeEvent> self = this;
			public IFuture<Void> execute(final PropertyChangeEvent event)
			{
				final Future<Void> ret = new Future<Void>();
				try
				{
					if(!agent.getFeature(IExecutionFeature.class).isComponentThread())
					{
						IFuture<Void> fut = agent.getFeature(IExecutionFeature.class).scheduleStep(() ->
						{
							publishToolBeliefEvent();
							Event ev = new Event(changeevent, new ChangeInfo<Object>(event.getNewValue(), event.getOldValue(), null));
							getRuleSystem().addEvent(ev);
							return null;	// done.
						});
						fut.addResultListener(new DelegationResultListener<Void>(ret)
						{
							public void exceptionOccurred(Exception exception)
							{
								if(exception instanceof ComponentTerminatedException)
								{
//									System.out.println("Ex in observe: "+exception.getMessage());
									Object val = event.getSource();
									getRuleSystem().unobserveObject(val, self);
									ret.setResult(null);
								}
								else
								{
									super.exceptionOccurred(exception);
								}
							}
						});
					}
					else
					{
						publishToolBeliefEvent();
						Event ev = new Event(changeevent, new ChangeInfo<Object>(event.getNewValue(), event.getOldValue(), null));
						getRuleSystem().addEvent(ev);
					}
				}
				catch(Exception e)
				{
					if(!(e instanceof ComponentTerminatedException))
						System.out.println("Ex in observe: "+e.getMessage());
					Object val = event.getSource();
					getRuleSystem().unobserveObject(val, self);
					ret.setResult(null);
				}
				return ret;
			}
		};
	}
	
//	/**
//	 *  Get the interpreter.
//	 *  @return The interpreter.
//	 */
//	public BDIAgentInterpreter getInterpreter()
//	{
//		return interpreter;
//	}
	
	/**
	 *  Get the rule system.
	 *  @return The rule system.
	 */
	public RuleSystem getRuleSystem()
	{
		return IInternalBDIAgentFeature.get().getRuleSystem();
	}

	/**
	 * 
	 */
	public void observeValue(final Object val)
	{
		if(val!=null)
			getRuleSystem().observeObject(val, true, false, eventadder);
	}

	/**
	 * 
	 */
	public void unobserveValue(Object val)
	{
		getRuleSystem().unobserveObject(val, eventadder);
	}
	
	/**
	 * 
	 */
	public void publishToolBeliefEvent()//String evtype)
	{
		if(melement instanceof MBelief)
			BDIAgentFeature.publishToolBeliefEvent((MBelief)melement);//, evtype);
	}

	/**
	 *  Get the addevent.
	 *  @return The addevent
	 */
	protected EventType getAddEvent()
	{
		return addevent;
	}

	/**
	 *  Get the remevent.
	 *  @return The remevent
	 */
	protected EventType getRemEvent()
	{
		return remevent;
	}

	/**
	 *  Get the changeevent.
	 *  @return The changeevent
	 */
	protected EventType getChangeEvent()
	{
		return changeevent;
	}
	
	/**
	 *  An entry was added to the collection.
	 */
	public void entryAdded(Object value, int index)
	{
//		unobserveValue(ret);
		observeValue(value);
		getRuleSystem().addEvent(new Event(getAddEvent(), new ChangeInfo<Object>(value, null, index>-1? Integer.valueOf(index): null)));
		publishToolBeliefEvent();
	}
	
	/**
	 *  An entry was removed from the collection.
	 */
	public void entryRemoved(Object value, int index)
	{
		unobserveValue(value);
//		observeValue(value);
		getRuleSystem().addEvent(new Event(getRemEvent(), new ChangeInfo<Object>(value, null, index>-1? Integer.valueOf(index): null)));
		publishToolBeliefEvent();
	}
	
	/**
	 *  An entry was changed in the collection.
	 */
	public void entryChanged(Object oldvalue, Object newvalue, int index)
	{
		if(oldvalue!=newvalue)
		{
			unobserveValue(oldvalue);
			observeValue(newvalue);
		}
		getRuleSystem().addEvent(new Event(getChangeEvent(), new ChangeInfo<Object>(newvalue, oldvalue,  index>-1? Integer.valueOf(index): null)));
		publishToolBeliefEvent();
	}
	
	/**
	 *  An entry was added to the map.
	 */
	public void	entryAdded(Object key, Object value)
	{
		observeValue(value);
		getRuleSystem().addEvent(new Event(getAddEvent(), new ChangeInfo<Object>(value, null, key)));
		publishToolBeliefEvent();
	}
	
	/**
	 *  An entry was removed from the map.
	 */
	public void	entryRemoved(Object key, Object value)
	{
		unobserveValue(value);
		getRuleSystem().addEvent(new Event(getRemEvent(), new ChangeInfo<Object>(value, null, key)));
		publishToolBeliefEvent();
	}
	
	/**
	 *  An entry was changed in the map.
	 */
	public void	entryChanged(Object key, Object oldvalue, Object newvalue)
	{
		unobserveValue(oldvalue);
		observeValue(newvalue);
		getRuleSystem().addEvent(new Event(getChangeEvent(), new ChangeInfo<Object>(newvalue, oldvalue, key)));
		publishToolBeliefEvent();
	}
}
