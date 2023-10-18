package jadex.mj.core.impl;

import jadex.mj.core.ComponentIdentifier;
import jadex.mj.core.IComponent;

public interface IComponentLifecycleManager
{
	public void create(Object pojo, ComponentIdentifier cid);
	
	public boolean isCreator(Object pojo);
	
	public void terminate(IComponent component);
	
	//public boolean isTerminator(IComponent component);
}