package jadex.bpmn.runtime.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import jadex.bpmn.runtime.BpmnProcess;
import jadex.bpmn.runtime.IBpmnComponentFeature;
import jadex.bpmn.runtime.RBpmnProcess;
import jadex.common.SReflect;
import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.future.IFuture;

public class BpmnProcessFeatureProvider extends ComponentFeatureProvider<IBpmnComponentFeature> implements IComponentLifecycleManager
{
	@Override
	public IBpmnComponentFeature createFeatureInstance(Component self)
	{
		return new BpmnProcessFeature((BpmnProcess)self);
	}
	
	@Override
	public Class<IBpmnComponentFeature> getFeatureType()
	{
		return IBpmnComponentFeature.class;
	}
	
	@Override
	public Class<? extends Component> getRequiredComponentType()
	{
		return BpmnProcess.class;
	}
	
	@Override
	public int	isCreator(Class<?> pojoclazz)
	{
		return SReflect.isSupertype(RBpmnProcess.class, pojoclazz) ? 1 : -1;
	}
	
	@Override
	public IFuture<IComponentHandle> create(Object pojo, ComponentIdentifier cid, Application app)
	{
		return BpmnProcess.create(pojo, cid, app);
	}
	
	/**
	 *  Get the predecessors, i.e. features that should be inited first.
	 *  @return The predecessors.
	 */
	public Set<Class<?>> getPredecessors(Set<Class<?>> all)
	{
		all.remove(getFeatureType());
		
		// Hack!!! remove injection feature to avoid dependency cycle
		// Injection is not used for BPMN but required for BPMN provided service.
		Iterator<Class<?>>	it	= all.iterator();
		while(it.hasNext())
		{
			if(it.next().getName().indexOf("Injection")!=-1)
			{
				it.remove();
			}
		}
		
		return all;
	}
	
	@Override
	public Map<String, Object> getResults(IComponent comp)
	{
		Map<String, Object> ret = Collections.emptyMap();
		if(comp.getPojo() instanceof RBpmnProcess)
		{
			RBpmnProcess p = (RBpmnProcess)comp.getPojo();
			ret = p.getResults();
		}
		return ret;
	}
}
