package jadex.bdi.impl.wrappers;

import java.util.List;

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
		EventType addevent, EventType remevent, EventType changeevent)
	{
		super(delegate);
		this.publisher = new EventPublisher(agent, addevent, remevent, changeevent);
			
		int	i=0;
		for(T entry: delegate)
		{
			publisher.entryAdded(entry, i++);
		}
	}
}