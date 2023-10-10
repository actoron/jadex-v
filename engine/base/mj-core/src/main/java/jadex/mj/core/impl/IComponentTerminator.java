package jadex.mj.core.impl;

import jadex.common.IFilter;
import jadex.mj.core.IComponent;
import jadex.mj.core.MjComponent;

public interface IComponentTerminator extends IFilter<MjComponent>
{
	public void terminate(IComponent component);
}
