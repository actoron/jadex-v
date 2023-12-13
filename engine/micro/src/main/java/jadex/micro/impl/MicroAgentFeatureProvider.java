package jadex.micro.impl;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IExternalAccess;
import jadex.core.impl.Component;
import jadex.core.impl.FeatureProvider;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.execution.IExecutionFeature;
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
		Agent val = findAnnotation(obj.getClass(), Agent.class, getClass().getClassLoader());
		if(val!=null)
			ret = "micro".equals(val.type());
		return ret;
	}
	
	protected static <T extends Annotation> T findAnnotation(Class<?> clazz, Class<T> anclazz, ClassLoader cl)
	{
		T ret = null;
		
		List<Class<?>> todo = new ArrayList<Class<?>>();
		todo.add(clazz);
		while(!todo.isEmpty())
		{
			clazz = todo.remove(0);
			todo.addAll(Arrays.asList(clazz.getInterfaces()));
			if(clazz.getSuperclass()!=null && !Object.class.equals(clazz.getSuperclass()))
				todo.add(clazz.getSuperclass());
			ret = MicroClassReader.getAnnotation(clazz, anclazz, cl);
			if(ret!=null)
				break;
		}
		
		return ret;
	}
	
	@Override
	public IExternalAccess create(Object pojo, ComponentIdentifier cid)
	{
		return MicroAgent.create(pojo, cid);
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
	
	/**
	 *  Get the predecessors, i.e. features that should be inited first.
	 *  @return The predecessors.
	 */
	public Set<Class<?>> getPredecessors(Set<Class<?>> all)
	{
		all.remove(getFeatureType());
		return all;
	}
}
