package jadex.micro;

import jadex.core.IComponent;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnEnd;
import jadex.model.annotation.OnStart;

/**
 *  Interface for inline pojo creation, i.e.
 *  IComponentManager.get().create(new IMicroAgent(){...})
 */
@Agent
public interface IMicroAgent 
{
	@OnStart
	public default void onStart(IComponent agent)
	{
	}
	
	@OnEnd
	public default void onEnd(IComponent agent)
	{
	}
}
