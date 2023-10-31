package jadex.micro.impl;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.impl.Component;
import jadex.core.impl.FeatureProvider;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.feature.execution.IExecutionFeature;
import jadex.micro.MicroAgent;
import jadex.micro.MicroClassReader;
import jadex.micro.annotation.Agent;

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
