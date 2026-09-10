package jadex.bding.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jadex.bding.IBDINGAgentFeature;
import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponentHandle;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.future.IFuture;

public class BDINGAgentFeatureProvider extends ComponentFeatureProvider<IBDINGAgentFeature> implements IComponentLifecycleManager
{

	public BDINGAgentFeatureProvider()
	{
	}

	@Override
	public Class< ? extends Component> getRequiredComponentType()
	{
		return BDINGAgent.class;
	}
	
	@Override
	public Class<IBDINGAgentFeature> getFeatureType()
	{
		return IBDINGAgentFeature.class;
	}

	@Override
	public IBDINGAgentFeature createFeatureInstance(Component self)
	{
		return new BDINGAgentFeature((BDINGAgent)self);
	}
	
	@Override
	public int isCreator(Class<?> pojoclazz)
	{
		boolean found	= false;
		Class<?>	test	= pojoclazz;
		while(!found && test!=null)
		{
			found	= test.isAnnotationPresent(jadex.bding.annotation.BDINGAgent.class);
			List<Class<?>>	interfaces	= new ArrayList<>(Arrays.asList(test.getInterfaces()));
			while(!found && !interfaces.isEmpty())
			{
				Class<?> interfaze	= interfaces.removeLast();
				found	= interfaze.isAnnotationPresent(jadex.bding.annotation.BDINGAgent.class);
				interfaces.addAll(new ArrayList<>(Arrays.asList(interfaze.getInterfaces())));
			}
			test	= test.getSuperclass();
		}
		return found?1:-1;
	}
	
	@Override
	public IFuture<IComponentHandle> create(Object pojo, ComponentIdentifier cid, Application app)
	{
		return Component.createComponent(new BDINGAgent(pojo, cid, app));
	}

	@Override
	public void init()
	{
		System.out.println("BDINGAgentFeatureProvider.init() called");	
	}	
}
