package jadex.bdi.runtime.wrappers;

import java.util.Set;

import jadex.bdi.model.MElement;
import jadex.collection.IEventPublisher;
import jadex.core.IComponent;
import jadex.rules.eca.EventType;

/**
 * 
 */
public class SetWrapper<T> extends jadex.collection.SetWrapper<T>
{
	/**
	 *  Create a new set wrapper.
	 */
	public SetWrapper(Set<T> delegate, IComponent agent, 
		EventType addevent, EventType remevent, EventType changeevent, MElement mbel)
	{
		super(delegate);
		if(agent!=null)
			this.publisher = new EventPublisher(agent, addevent, remevent, changeevent, mbel);
		else
			this.publisher = new InitEventPublisher(delegate, addevent, remevent, changeevent, mbel);

		
		int	i=0;
		for(T entry: delegate)
		{
			publisher.entryAdded(entry, i++);
		}
	}
	
	/**
	 * 
	 */
	public void setAgent(IComponent agent)
	{
		if(publisher instanceof InitEventPublisher)
		{
			InitEventPublisher pub = (InitEventPublisher)publisher;
			this.publisher = new EventPublisher(agent, pub.addevent, pub.remevent, pub.changeevent, pub.melement);
		}
	}
	
	/**
	 * 
	 */
	public boolean isInitWrite()
	{
		return publisher instanceof InitEventPublisher;
	}

}
