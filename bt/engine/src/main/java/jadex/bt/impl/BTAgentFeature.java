package jadex.bt.impl;

import jadex.bt.BTAgent;
import jadex.bt.Event;
import jadex.bt.IBTProvider;
import jadex.bt.Node;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.execution.impl.ILifecycle;
import jadex.micro.impl.MicroAgentFeature;

public class BTAgentFeature	extends MicroAgentFeature implements ILifecycle
{
	public static BTAgentFeature get()
	{
		return IExecutionFeature.get().getComponent().getFeature(BTAgentFeature.class);
	}

	protected BTAgentFeature(BTAgent self)
	{
		super(self);
	}
	
	@Override
	public void	onStart()
	{
		super.onStart();
		
		IBTProvider prov = (IBTProvider)getSelf().getPojo();
		Node<IComponent> bt = prov.createBehaviorTree();
		bt.setContext(self);
		bt.execute(new Event("start", null));
	}
	
	@Override
	public void	onEnd()
	{
		super.onEnd();
	}

}
