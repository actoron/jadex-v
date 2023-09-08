package jadex.mj.core.impl;

import jadex.common.IFilter;
import jadex.mj.core.MjComponent;

public interface IComponentCreator extends IFilter<Object>
{
	//public Class<? extends MjComponent> getType();
	
	public void create(Object pojo);
}