package jadex.core;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import jadex.core.impl.Component;
import jadex.core.impl.ComponentManager;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.core.impl.SComponentFeatureProvider;
import jadex.future.Future;
import jadex.future.IFuture;

public interface IComponentFactory
{
	/**
	 *  Create a component based on a pojo.
	 *  @param pojo The pojo.
	 *  @return The external access of the running component.
	 */
	public default IFuture<IComponentHandle> create(Object pojo)
	{
		return create(pojo, null);
	}
	
	/**
	 *  Create a component based on a pojo.
	 *  @param pojo The pojo.
	 *  @param cid The component id or null for auto-generationm.
	 *  @return The external access of the running component.
	 */
	public default IFuture<IComponentHandle> create(Object pojo, ComponentIdentifier cid)
	{		
		return create(pojo, null, null);
	}
	
	/**
	 *  Create a component based on a pojo.
	 *  @param pojo The pojo.
	 *  @param cid The component id or null for auto-generationm.
	 *  @param app The application context.
	 *  @return The external access of the running component.
	 */
	public default IFuture<IComponentHandle> create(Object pojo, ComponentIdentifier cid, Application app)
	{		
		if(pojo==null)
		{
			// Plain component for null pojo
			return Component.createComponent(Component.class, () -> new Component(pojo,cid,app));
		}
		else
		{
			IComponentLifecycleManager	creator	= SComponentFeatureProvider.getCreator(pojo.getClass());
			if(creator!=null)
			{
				return creator.create(pojo, cid, app);
			}
			else
			{
				return new Future<>(new RuntimeException("Could not create component: "+pojo));
			}
		}
	}
	
	/**
	 *  Usage of components as functions that terminate after execution.
	 *  Create a component based on a function.
	 *  @param pojo The function.
	 *  @return The execution result.
	 */
	public default <T> IFuture<T> run(IThrowingFunction<IComponent, T> pojo)
	{
		return run((Object)pojo);
	}
	
	/**
	 *  Usage of components as functions that terminate after execution.
	 *  Create a component based on a function.
	 *  @param pojo The callable.
	 *  @return The execution result.
	 */
	public default <T> IFuture<T> run(Callable<T> pojo)
	{
		return run((Object)pojo);
	}

	/**
	 *  Usage of components as functions that terminate after execution.
	 *  Create a component based on a function.
	 *  @param pojo The pojo.
	 *  @return The execution result.
	 */
	public default <T> IFuture<T> run(Object pojo)
	{
		return run(pojo, null, null);
	}
	
	/**
	 *  Usage of components as functions that terminate after execution.
	 *  Create a component based on a function.
	 *  @param pojo The pojo.
	 *  @param cid The component id or null for auto-generationm.
	 *  @return The execution result.
	 */
	public default <T> IFuture<T> run(Object pojo, ComponentIdentifier cid)
	{
		return run(pojo, cid, null);
	}
	
	/**
	 *  Usage of components as functions that terminate after execution.
	 *  Create a component based on a function.
	 *  @param pojo The pojo.
	 *  @param cid The component id or null for auto-generationm.
	 *  @return The execution result.
	 */
	public default <T> IFuture<T> run(Object pojo, ComponentIdentifier cid, Application app)
	{
		if(pojo==null)
		{
			return new Future<>(new UnsupportedOperationException("No null pojo allowed for run()."));
		}
		else
		{
			IComponentLifecycleManager	creator	= SComponentFeatureProvider.getCreator(pojo.getClass());
			if(creator!=null)
			{
				return creator.run(pojo, cid, app);
			}
			else
			{
				return new Future<>(new RuntimeException("Could not create component: "+pojo));
			}
		}
	}
	
	/**
	 *  Terminate a component with given id.
	 *  @param cid The component id.
	 */
	// todo: return pojo as result (has results)
	public default IFuture<Void> terminate(ComponentIdentifier cid)
	{
		IFuture<Void> ret;
		
		//System.out.println("terminate: "+cid+" comps: "+ComponentManager.get().getNumberOfComponents());
		
		try
		{
			IComponent comp = ComponentManager.get().getComponent(cid);
			if(comp!=null)
			{
				IComponentHandle	exta = comp.getComponentHandle();
				//ComponentManager.get().removeComponent(cid); // done in Component
				if(Component.isExecutable())
				{
					// Don't use async step, because icomp.terminate() is sync anyways (when no cid is given).
					ret	= exta.scheduleStep(icomp ->
					{
						icomp.terminate();
						return (Void)null;
					});
				}
				else
				{
					// Hack!!! Concurrency issue?
					ret	= comp.terminate();
				}
			}
			else
			{
				ret	= new Future<>(new ComponentNotFoundException(cid));
			}
		}
		catch(Exception e)
		{
			ret	= new Future<>(e);
		}
		
		return ret;
	}
	
	/**
	 *  Wait for the last component being terminated.
	 *  This call keeps the calling thread waiting till termination.
	 */
	public void waitForLastComponentTerminated();
	
	/**
	 *  Wait for termination of a component.
	 *  @param cid The component id;
	 *  @return True on termination; false on component not found.
	 */
	public default IFuture<Boolean> waitForTermination(ComponentIdentifier cid)
	{
		Future<Boolean> ret = new Future<>();
		boolean[] found = new boolean[1];
		//synchronized(ComponentManager.get().components)
		ComponentManager.get().runWithComponentsLock(() ->
		{
			if(ComponentManager.get().getComponent(cid)!=null)
			{
				found[0] = true;
				IComponentManager.get().addComponentListener(new IComponentListener() 
				{
					@Override
					public void componentRemoved(ComponentIdentifier ccid) 
					{
						if(cid.equals(ccid))
						{
							IComponentManager.get().removeComponentListener(this, IComponentManager.COMPONENT_REMOVED);
							ret.setResult(true);
						}
					}
				}, IComponentManager.COMPONENT_REMOVED);
			}
		});
		if(!found[0])
			ret.setResult(false);
		return ret;
	}

}
