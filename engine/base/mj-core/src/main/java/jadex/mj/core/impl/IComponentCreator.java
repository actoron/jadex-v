package jadex.mj.core.impl;

import jadex.common.IFilter;
import jadex.mj.core.ComponentIdentifier;

public interface IComponentCreator extends IFilter<Object>
{
	public void create(Object pojo, ComponentIdentifier cid);
}