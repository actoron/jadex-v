package jadex.bdiv3.runtime.wrappers;

import java.beans.PropertyChangeEvent;

import jadex.bdiv3.features.impl.BDIAgentFeature;
import jadex.bdiv3.features.impl.IInternalBDIAgentFeature;
import jadex.bdiv3.model.MElement;
import jadex.common.IResultCommand;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.mj.feature.execution.ComponentTerminatedException;
import jadex.rules.eca.ChangeInfo;
import jadex.rules.eca.Event;
import jadex.rules.eca.EventType;
import jadex.rules.eca.RuleSystem;

/**
 * 
 */
public class InitEventPublisher implements IEventPublisher
{
	protected Object obj;
	
	/** The add event name. */
	protected EventType addevent;
	
	/** The remove event name. */
	protected EventType remevent;
	
	/** The change event name. */
	protected EventType changeevent;
	
	/** The melement. */
	protected MElement melement;
	
	/** The event adder. */
	protected IResultCommand<IFuture<Void>, PropertyChangeEvent> eventadder;
	
	/**
	 * 
	 */
	public InitEventPublisher(Object obj, EventType addevent, EventType remevent, final EventType changeevent, MElement melem)
	{
		this.obj = obj;
		this.addevent = addevent;
		this.remevent = remevent;
		this.changeevent = changeevent;
		this.melement = melem;
		
		eventadder = new IResultCommand<IFuture<Void>, PropertyChangeEvent>()
		{
			final IResultCommand<IFuture<Void>, PropertyChangeEvent> self = this;
			public IFuture<Void> execute(final PropertyChangeEvent event)
			{
				final Future<Void> ret = new Future<Void>();
				
				BDIAgentFeature.addInitWrite(InitEventPublisher.this.obj, () ->
				{
						try
						{
//								publishToolBeliefEvent();
							Event ev = new Event(changeevent, new ChangeInfo<Object>(event.getNewValue(), event.getOldValue(), null));
							getRuleSystem().addEvent(ev);
						}
						catch(Exception e)
						{
							if(!(e instanceof ComponentTerminatedException))
								System.out.println("Ex in observe: "+e.getMessage());
							Object val = event.getSource();
							getRuleSystem().unobserveObject(val, self);
							ret.setResult(null);
						}
				});
				
				return ret;
			}
		};
	}
	
	/**
	 *  An entry was added to the collection.
	 */
	public void entryAdded(final Object value, final int index)
	{
		BDIAgentFeature.addInitWrite(obj, () ->
		{
				observeValue(value);
				getRuleSystem().addEvent(new Event(getAddEvent(), new ChangeInfo<Object>(value, null, index>-1? Integer.valueOf(index): null)));
//				publishToolBeliefEvent();
		});
	}
	
	/**
	 *  An entry was removed from the collection.
	 */
	public void entryRemoved(final Object value, final int index)
	{
		BDIAgentFeature.addInitWrite(obj, () ->
		{
				unobserveValue(value);
//				observeValue(value);
				getRuleSystem().addEvent(new Event(getRemEvent(), new ChangeInfo<Object>(value, null, index>-1? Integer.valueOf(index): null)));
//				publishToolBeliefEvent();
		});
	}
	
	/**
	 *  An entry was changed in the collection.
	 */
	public void entryChanged(final Object oldvalue, final Object newvalue, final int index)
	{
		BDIAgentFeature.addInitWrite(obj, () ->
		{
				if(oldvalue!=newvalue)
				{
					unobserveValue(oldvalue);
					observeValue(newvalue);
				}
				getRuleSystem().addEvent(new Event(getChangeEvent(), new ChangeInfo<Object>(newvalue, oldvalue,  index>-1? Integer.valueOf(index): null)));
//				publishToolBeliefEvent();
		});
	}
	
	/**
	 *  An entry was added to the map.
	 */
	public void	entryAdded(final Object key, final Object value)
	{
		BDIAgentFeature.addInitWrite(obj, () ->
		{
				observeValue(value);
				getRuleSystem().addEvent(new Event(getAddEvent(), new ChangeInfo<Object>(value, null, key)));
//				publishToolBeliefEvent();
		});
	}
	
	/**
	 *  An entry was removed from the map.
	 */
	public void	entryRemoved(final Object key, final Object value)
	{
		BDIAgentFeature.addInitWrite(obj, () ->
		{
				unobserveValue(value);
				getRuleSystem().addEvent(new Event(getRemEvent(), new ChangeInfo<Object>(null, value, key)));
//				publishToolBeliefEvent();
		});
	}
	
	/**
	 *  An entry was changed in the map.
	 */
	public void	entryChanged(final Object key, final Object oldvalue, final Object newvalue)
	{
		BDIAgentFeature.addInitWrite(obj, () ->
		{
				unobserveValue(oldvalue);
				observeValue(newvalue);
				getRuleSystem().addEvent(new Event(getChangeEvent(), new ChangeInfo<Object>(newvalue, oldvalue, key)));
//				publishToolBeliefEvent();
		});
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
	 *  Get the rule system.
	 *  @return The rule system.
	 */
	public RuleSystem getRuleSystem()
	{
		return IInternalBDIAgentFeature.get().getRuleSystem();
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
}
