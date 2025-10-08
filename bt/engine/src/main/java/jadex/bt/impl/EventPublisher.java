package jadex.bt.impl;

import java.beans.PropertyChangeEvent;
import java.lang.System.Logger.Level;

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
public class EventPublisher
{
	/** The agent interpreter. */
	protected IComponent agent;
	
	/** The add event name. */
	protected EventType addevent;
	
	/** The remove event name. */
	protected EventType remevent;
	
	/** The change event name. */
	protected EventType changeevent;
	
	/** The event adder. */
	protected IResultCommand<IFuture<Void>, PropertyChangeEvent> eventadder;

	/**
	 *  Create a new publisher.
	 */
	public EventPublisher(IComponent agent, EventType changeevent)
	{
		this(agent, null, null, changeevent);
	}
	
	/**
	 *  Create a new publisher.
	 */
	public EventPublisher(IComponent agent, 
		EventType addevent, EventType remevent, EventType changeevent)
	{
		this.agent = agent;
		this.addevent = addevent;
		this.remevent = remevent;
		this.changeevent = changeevent;
		
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
							//publishToolBeliefEvent();
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
						//publishToolBeliefEvent();
						Event ev = new Event(changeevent, new ChangeInfo<Object>(event.getNewValue(), event.getOldValue(), null));
						getRuleSystem().addEvent(ev);
					}
				}
				catch(Exception e)
				{
					if(!(e instanceof ComponentTerminatedException))
					{
						//System.out.println("Ex in observe: "+e.getMessage());
						System.getLogger(this.getClass().getName()).log(Level.ERROR, "Ex in observe: "+e.getMessage());
					}
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
		return BTAgentFeature.get().getRuleSystem();
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
	 * /
	public void publishToolBeliefEvent()//String evtype)
	{
		if(melement instanceof MBelief)
			BDIAgentFeature.publishToolBeliefEvent((MBelief)melement);//, evtype);
	}*/

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
	public void entryAdded(Object value, Object info)
	{
//		unobserveValue(ret);
		observeValue(value);
		getRuleSystem().addEvent(new Event(getAddEvent(), new ChangeInfo<Object>(value, null, info)));
		//publishToolBeliefEvent();
	}
	
	/**
	 *  An entry was removed from the collection.
	 */
	public void entryRemoved(Object value, Object info)
	{
		unobserveValue(value);
//		observeValue(value);
		getRuleSystem().addEvent(new Event(getRemEvent(), new ChangeInfo<Object>(value, null, info)));
		//publishToolBeliefEvent();
	}
	
	/**
	 *  An entry was changed in the collection.
	 */
	public void entryChanged(Object oldvalue, Object newvalue, Object info)
	{
		if(oldvalue!=newvalue)
		{
			unobserveValue(oldvalue);
			observeValue(newvalue);
		}
		getRuleSystem().addEvent(new Event(getChangeEvent(), new ChangeInfo<Object>(newvalue, oldvalue,  info)));
		//publishToolBeliefEvent();
	}
}
