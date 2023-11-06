package jadex.benchmark;

import jadex.bdi.runtime.IBDIAgentFeature;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;

@Agent
public class SimpleBDI
{
	@OnStart
	public void	start(IComponent agent)
	{
		@SuppressWarnings("unchecked")
		Future<ComponentIdentifier>	cid	= (Future<ComponentIdentifier>)agent.getFeature(IBDIAgentFeature.class).getArgument("cid");
		cid.setResult(agent.getId());
	}
}
