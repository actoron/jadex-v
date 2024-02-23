package jadex.core.impl;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IExternalAccess;

public interface IComponentLifecycleManager
{
	public IExternalAccess create(Object pojo, ComponentIdentifier cid);
	
	public boolean isCreator(Object pojo);
	
	public void terminate(IComponent component);
	
	//public boolean isTerminator(IComponent component);
}