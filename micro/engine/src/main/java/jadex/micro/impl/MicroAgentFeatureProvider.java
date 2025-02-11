package jadex.micro.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jadex.common.SReflect;
import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentManager;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.execution.IExecutionFeature;
import jadex.execution.impl.IInternalExecutionFeature;
import jadex.micro.MicroAgent;
import jadex.micro.MicroClassReader;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentResult;
import jadex.micro.annotation.Result;

public class MicroAgentFeatureProvider extends ComponentFeatureProvider<MicroAgentFeature> implements IComponentLifecycleManager
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
	
	public static <T extends Annotation> T findAnnotation(Class<?> clazz, Class<T> anclazz, ClassLoader cl)
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
	public IComponentHandle create(Object pojo, ComponentIdentifier cid, Application app)
	{
		return MicroAgent.create(pojo, cid, app);
	}
	
	/*@Override
	public boolean isTerminator(IComponent component) 
	{
		return component.getClass().equals(MjMicroAgent.class);
	}*/
	
	@Override
	public void terminate(IComponent component) 
	{
		((IInternalExecutionFeature)component.getFeature(IExecutionFeature.class)).terminate();
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
	
	public Map<String, Object> getResults(Object pojo)
	{
		Map<String, Object> ret = new HashMap<String, Object>();
		if(pojo!=null)
		{
			Class<?> pcl = pojo.getClass();
			Field[] fls = SReflect.getAllFields(pcl);
			
			for(int i=0; i<fls.length; i++)
			{
				if(MicroClassReader.isAnnotationPresent(fls[i], AgentResult.class, ComponentManager.get().getClassLoader()))
				{
					try
					{
						AgentResult r = MicroClassReader.getAnnotation(fls[i], AgentResult.class, ComponentManager.get().getClassLoader());
						fls[i].setAccessible(true);
						Object val = fls[i].get(pojo);
						ret.put(fls[i].getName(), val);
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
				}
			}
		}
		return ret;
	}
}
