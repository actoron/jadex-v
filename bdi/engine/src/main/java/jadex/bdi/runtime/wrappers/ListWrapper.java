package jadex.bdi.runtime.wrappers;

import java.util.List;

import jadex.bdi.model.MElement;
import jadex.core.IComponent;
import jadex.rules.eca.EventType;

/**
 * 
 */
public class ListWrapper<T> extends jadex.collection.ListWrapper<T> 
{
	/**
	 *  Create a new list wrapper.
	 */
	public ListWrapper(List<T> delegate, IComponent agent, 
		EventType addevent, EventType remevent, EventType changeevent, MElement melem)
	{
		super(delegate);
		if(agent!=null)
			this.publisher = new EventPublisher(agent, addevent, remevent, changeevent, melem);
		else
			this.publisher = new InitEventPublisher(delegate, addevent, remevent, changeevent, melem);
			
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