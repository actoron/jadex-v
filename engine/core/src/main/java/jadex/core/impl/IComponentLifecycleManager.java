package jadex.core.impl;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;

public interface IComponentLifecycleManager
{
	public void create(Object pojo, ComponentIdentifier cid);
	
	public boolean isCreator(Object pojo);
	
	public void terminate(IComponent component);
	
	//public boolean isTerminator(IComponent component);
}