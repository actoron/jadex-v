package jadex.bpmn.runtime.impl;

import java.util.Set;

import jadex.bpmn.runtime.BpmnProcess;
import jadex.bpmn.runtime.RBpmnProcess;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IExternalAccess;
import jadex.core.impl.Component;
import jadex.core.impl.FeatureProvider;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.execution.IExecutionFeature;

public class BpmnProcessLifecycleFeatureProvider extends FeatureProvider<BpmnProcessLifecycleFeature>  implements IComponentLifecycleManager
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
	public IExternalAccess create(Object pojo, ComponentIdentifier cid)
	{
		return BpmnProcess.create(pojo, cid);
	}

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
