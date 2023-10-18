package jadex.mj.micro.impl;

import jadex.mj.core.ComponentIdentifier;
import jadex.mj.core.IComponent;
import jadex.mj.core.MjComponent;
import jadex.mj.core.impl.IComponentLifecycleManager;
import jadex.mj.core.impl.MjFeatureProvider;
import jadex.mj.feature.execution.IMjExecutionFeature;
import jadex.mj.micro.MicroClassReader;
import jadex.mj.micro.MjMicroAgent;
import jadex.mj.micro.annotation.Agent;

public class MjMicroAgentFeatureProvider extends MjFeatureProvider<MjMicroAgentFeature> implements IComponentLifecycleManager
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
	public Class< ? extends MjComponent> getRequiredComponentType()
	{
		return MjMicroAgent.class;
	}
	
	@Override
	public Class<MjMicroAgentFeature> getFeatureType()
	{
		return MjMicroAgentFeature.class;
	}

	@Override
	public MjMicroAgentFeature createFeatureInstance(MjComponent self)
	{
		return new MjMicroAgentFeature((MjMicroAgent)self);
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
		MjMicroAgent.create(pojo, cid);
	}
	
	/*@Override
	public boolean isTerminator(IComponent component) 
	{
		return component.getClass().equals(MjMicroAgent.class);
	}*/
	
	@Override
	public void terminate(IComponent component) 
	{
		component.getFeature(IMjExecutionFeature.class).terminate();
	}
}
