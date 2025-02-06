package jadex.bpmn.runtime.impl;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import jadex.bpmn.runtime.BpmnProcess;
import jadex.bpmn.runtime.RBpmnProcess;
import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.execution.IExecutionFeature;
import jadex.execution.impl.IInternalExecutionFeature;

public class BpmnProcessLifecycleFeatureProvider extends ComponentFeatureProvider<BpmnProcessLifecycleFeature>  implements IComponentLifecycleManager
{
	@Override
	public Class<BpmnProcessLifecycleFeature> getFeatureType()
	{
		return BpmnProcessLifecycleFeature.class;
	}
	
	@Override
	public BpmnProcessLifecycleFeature createFeatureInstance(Component self)
	{
		return new BpmnProcessLifecycleFeature((BpmnProcess)self);
	}
	
	@Override
	public Class<? extends Component> getRequiredComponentType()
	{
		return BpmnProcess.class;
	}
	
	@Override
	public boolean isCreator(Object obj)
	{
		boolean ret = false;
		if(obj instanceof String)
		{
			ret	= ((String)obj).startsWith("bpmn:");
		}
		else if(obj instanceof RBpmnProcess)
		{
			ret	= true;
		}
		return ret;
	}
	
	@Override
	public IComponentHandle create(Object pojo, ComponentIdentifier cid, Application app)
	{
		return BpmnProcess.create(pojo, cid, app);
	}

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
		Map<String, Object> ret = Collections.EMPTY_MAP;
		if(pojo!=null)
		{
			if(pojo instanceof RBpmnProcess)
			{
				RBpmnProcess p = (RBpmnProcess)pojo;
				ret = p.getResults();
			}
		}
		return ret;
	}
}
