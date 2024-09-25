package jadex.bt.impl;

import jadex.rules.eca.ChangeInfo;
import jadex.rules.eca.IEvent;

public record Event(String type, Object value) 
{	
	public Event(IEvent ecaevent)
	{
		this(ecaevent.getType().toString(), 
			ecaevent.getContent() instanceof ChangeInfo? ((ChangeInfo<?>)ecaevent.getContent()).getValue(): ecaevent.getContent());
	}
}
