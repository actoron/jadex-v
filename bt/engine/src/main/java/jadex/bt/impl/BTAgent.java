package jadex.bt.impl;

import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponentHandle;
import jadex.core.impl.Component;
import jadex.future.IFuture;

public class BTAgent extends Component
{
	public static IFuture<IComponentHandle> create(Object pojo)
	{
		return create(pojo, null, null);
	}
	
	public static IFuture<IComponentHandle> create(Object pojo, ComponentIdentifier cid, Application app)
	{
		return Component.createComponent(BTAgent.class, () -> new BTAgent(pojo, cid, app));
	}
	
	public BTAgent(Object pojo, ComponentIdentifier cid, Application app)
	{
		super(pojo, cid, app);
	}
}
