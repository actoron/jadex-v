package jadex.bpmn.runtime;

import java.util.Set;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.impl.Component;
import jadex.core.impl.FeatureProvider;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.execution.IExecutionFeature;
import jadex.micro.impl.MicroAgentFeature;
import jadex.micro.impl.MicroAgentFeatureProvider;

public class BpmnProcessLifecycleFeatureProvider extends FeatureProvider<MicroAgentFeature>  implements IComponentLifecycleManager
{
	@Override
	public Class<MicroAgentFeature> getFeatureType()
	{
		return MicroAgentFeature.class;
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
	public boolean replacesFeatureProvider(FeatureProvider<MicroAgentFeature> provider)
	{
		return provider instanceof MicroAgentFeatureProvider;
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
	public void create(Object pojo, ComponentIdentifier cid)
	{
		BpmnProcess.create(pojo, cid);
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
