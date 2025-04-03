package jadex.bdi.impl.wrappers;

import java.util.Map;

import jadex.core.IComponent;
import jadex.rules.eca.EventType;

/**
 * 
 */
public class MapWrapper<T, E> extends jadex.collection.MapWrapper<T, E>
{
	/**
	 *  Create a new set wrapper.
	 */
	public MapWrapper(Map<T, E> delegate, IComponent agent, 
		EventType addevent, EventType remevent, EventType changeevent)
	{
		super(delegate);
		this.publisher = new EventPublisher(agent, addevent, remevent, changeevent);
		
		for(Map.Entry<T,E> entry: delegate.entrySet())
		{
			publisher.entryAdded(entry.getKey(), entry.getValue());
		}
	}
}
