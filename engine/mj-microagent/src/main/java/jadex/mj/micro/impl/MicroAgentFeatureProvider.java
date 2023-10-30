package jadex.mj.micro.impl;

import jadex.mj.core.ComponentIdentifier;
import jadex.mj.core.IComponent;
import jadex.mj.core.impl.IComponentLifecycleManager;
import jadex.mj.core.impl.Component;
import jadex.mj.core.impl.FeatureProvider;
import jadex.mj.feature.execution.IExecutionFeature;
import jadex.mj.micro.MicroClassReader;
import jadex.mj.micro.MicroAgent;
import jadex.mj.micro.annotation.Agent;

public class MicroAgentFeatureProvider extends FeatureProvider<MicroAgentFeature> implements IComponentLifecycleManager
{
	/*static
	{
		MjComponent.addComponentCreator(new IComponentLifecycleManager() 
		{
			// todo: use our classreader?!
			@Override
			public boolean isCreator(Object obj) 
			{
				boolean ret = false;
				Agent val = MicroClassReader.getAnnotation(obj.getClass(), Agent.class, getClass().getClassLoader());
				if(val!=null)
					ret = "micro".equals(val.type());
				return ret;
			}
			
			@Override
			public void create(Object pojo, ComponentIdentifier cid)
			{
				MjMicroAgent.create(pojo, cid);
			}
			
			@Override
			public boolean isTerminator(IComponent component) 
			{
				return component.getClass().equals(MjMicroAgent.class);
			}
			
			@Override
			public void terminate(IComponent component) 
			{
				component.getFeature(IMjExecutionFeature.class).terminate();
			}
		});
	}*/
	
	@Override
	public Class< ? extends Component> getRequiredComponentType()
	{
		return MicroAgent.class;
	}
	
	@Override
	public Class<MicroAgentFeature> getFeatureType()
	{
		return MicroAgentFeature.class;
	}

	@Override
	public MicroAgentFeature createFeatureInstance(Component self)
	{
		return new MicroAgentFeature((MicroAgent)self);
	}
	
	
	@Override
	public boolean isCreator(Object obj) 
	{
		boolean ret = false;
		Agent val = MicroClassReader.getAnnotation(obj.getClass(), Agent.class, getClass().getClassLoader());
		if(val!=null)
			ret = "micro".equals(val.type());
		return ret;
	}
	
	@Override
	public void create(Object pojo, ComponentIdentifier cid)
	{
		MicroAgent.create(pojo, cid);
	}
	
	/*@Override
	public boolean isTerminator(IComponent component) 
	{
		return component.getClass().equals(MjMicroAgent.class);
	}*/
	
	@Override
	public void terminate(IComponent component) 
	{
		component.getFeature(IExecutionFeature.class).terminate();
	}
}
