package jadex.bt.impl;

import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponentHandle;
import jadex.core.impl.Component;

public class BTAgent extends Component
{
	public static IComponentHandle create(Object pojo)
	{
		return create(pojo, null, null);
	}
	
	public static IComponentHandle create(Object pojo, ComponentIdentifier cid, Application app)
	{
		Component comp = Component.createComponent(BTAgent.class, () -> new BTAgent(pojo, cid, app));
		return comp.getComponentHandle();
	}
	
	public BTAgent(Object pojo, ComponentIdentifier cid, Application app)
	{
		super(pojo, cid, app);
	}
}
