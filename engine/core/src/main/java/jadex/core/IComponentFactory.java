package jadex.core;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.Callable;

import jadex.core.impl.Component;
import jadex.core.impl.ComponentManager;
import jadex.core.impl.StepAborted;
import jadex.future.Future;
import jadex.future.FutureBarrier;
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
	 *  @param localname The component id or null for auto-generationm.
	 *  @return The external access of the running component.
	 */
	public default IFuture<IComponentHandle> create(Object pojo, String localname)
	{		
		return ComponentManager.get().create(pojo, localname, null);
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
	public default <E, T extends IFuture<E>> T runAsync(Callable<T> pojo)
	{
		@SuppressWarnings("unchecked")
		T	ret	= (T) ComponentManager.get().run(pojo, null, null, true);
		return ret;
	}

	/**
	 *  Usage of components as functions that terminate after execution.
	 *  Create a component based on a function.
	 *  @param pojo The function.
	 *  @return The execution result.
	 */
	public default <E, T extends IFuture<E>> T runAsync(IThrowingFunction<IComponent, T> pojo)
	{
		@SuppressWarnings("unchecked")
		T	ret	= (T) ComponentManager.get().run(pojo, null, null, true);
		return ret;
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
		return run(pojo, null);
	}
	
	/**
	 *  Usage of components as functions that terminate after execution.
	 *  Create a component based on a function.
	 *  @param pojo The pojo.
	 *  @param localname The component id or null for auto-generationm.
	 *  @return The execution result.
	 */
	public default <T> IFuture<T> run(Object pojo, String localname)
	{
		return ComponentManager.get().run(pojo, localname, null, false);
	}

	/**
	 *  Get all components.
	 *  @return The component ids.
	 */
	public Set<ComponentIdentifier> getAllComponents();
	
	/**
	 *  Terminate components
	 *  @param cid The component ids or none for all components.
	 */
	// todo: return pojo as result (has results)
	public default IFuture<Void> terminate(ComponentIdentifier... cids)
	{
		Iterable<ComponentIdentifier>	iter;
		
		if(cids==null || cids.length==0)
		{
			iter	= getAllComponents();
		}
		else if(cids.length==1)
		{
			return doTerminate(cids[0]);
		}
		else
		{
			iter	= Arrays.asList(cids);
		}
		
		FutureBarrier<Void> bar = new FutureBarrier<Void>();
		for(ComponentIdentifier cid: iter)
		{
			IFuture<Void>	fut	= doTerminate(cid);
			bar.add(fut);
		}
		return bar.waitFor();
	}

	private static IFuture<Void>	doTerminate(ComponentIdentifier cid)
	{
		try
		{
			IComponent comp = ComponentManager.get().getComponent(cid);
			if(comp==null)
			{
				throw new IllegalArgumentException("Component with id '"+cid+"' does not exist.");
			}
			IComponentHandle	exta = comp.getComponentHandle();
			//ComponentManager.get().removeComponent(cid); // done in Component
			if(Component.isExecutable())
			{
				// Don't use async step, because icomp.terminate() is sync anyways (when no cid is given).
				return exta.scheduleStep(icomp ->
				{
					try
					{
						((Component)icomp).doTerminate();
					}
					catch(StepAborted e)
					{
						// Skip abortion of user code when called from outside.
					}
					return (Void)null;
				});
			}
			else
			{
				// Hack!!! Concurrency issue?
				((Component)comp).doTerminate();
				return IFuture.DONE;
			}
		}
		catch(Exception e)
		{
			return new Future<>(e);
		}
	}
	
	/**
	 *  Get the component handle.
	 *  @param cid The id of the component.
	 *  @return The handle.
	 *  @throws IllegalArgumentException when the component does not exist.
	 */
	public default IComponentHandle getComponentHandle(ComponentIdentifier cid)
	{
		IComponent comp = ComponentManager.get().getComponent(cid);
		if(comp==null)
		{
			throw new IllegalArgumentException("Component with id '"+cid+"' does not exist.");
		}
		return comp.getComponentHandle();
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
