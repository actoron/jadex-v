package jadex.injection.impl;

import java.lang.System.Logger.Level;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jadex.common.SUtil;
import jadex.core.ChangeEvent;
import jadex.core.IChangeListener;
import jadex.core.IComponent;
import jadex.core.ResultProvider;
import jadex.core.impl.ILifecycle;
import jadex.execution.IExecutionFeature;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.injection.Dyn;
import jadex.injection.IInjectionFeature;

/**
 *  Component feature allowing e.g. @OnStart methods and field injections.
 */
public class InjectionFeature implements IInjectionFeature, ILifecycle
{
	/** The component. */
	protected IComponent	self;
	
	/** The model caching all invocation stuff. */
	protected InjectionModel	model;
	
	/** Managed extra objects (e.g. services implemented as separate class). List of pojo hierarchy -> context. */
	protected Map<List<Object>, Object>	extras;
	
	/** Listeners for dynamic value changes. */
	protected Map<String, Set<IChangeListener>>	listeners;
	
	protected ResultProvider	rp;
	
	/**
	 *  Create the injection feature.
	 */
	public InjectionFeature(IComponent self)
	{
		this.self	= self;
		this.model	= InjectionModel.getStatic(Collections.singletonList(self.getPojo()!=null ? self.getPojo().getClass() : Object.class), null, null);
	}
	
	//-------- lifecycle methods --------
	
	@Override
	public void init()
	{
		startPojo(model, Collections.singletonList(self.getPojo()), null);		
	}
	
	/**
	 *  Perform init and start operations for the given object.
	 */
	protected void startPojo(InjectionModel model, List<Object> pojos, Object context)
	{
		for(IInjectionHandle handle: model.getPreInject())
		{
			handle.apply(self, pojos, context, null);
		}

		for(IInjectionHandle handle: model.getFieldInjections())
		{
			handle.apply(self, pojos, context, null);
		}
		
		for(IInjectionHandle handle: model.getPostInject())
		{
			handle.apply(self, pojos, context, null);
		}

		// Hack!!! Start async when component pojo to allow agents with never ending main loops (e.g. quiz, simple cleaner, ...)
		if(pojos.size()==1)
		{
			if(!model.getOnStart().isEmpty())
			{
				// TODO: wait for future return values?
				self.getFeature(IExecutionFeature.class).scheduleStep((Runnable)()->
				{
					for(IInjectionHandle handle: model.getOnStart())
					{
						handle.apply(self, pojos, context, null);
					}
				});
			}

			if(!model.getMethodInjections().isEmpty())
			{
				self.getFeature(IExecutionFeature.class).scheduleStep((Runnable)()->
				{
					for(IInjectionHandle handle: model.getMethodInjections())
					{
						handle.apply(self, pojos, context, null);
					}
				});
			}
		}
		
		// Start sync when extra pojo to simplify, e.g. bdi plans
		else
		{
			// Schedule method injections first, in case OnStart blocks
			if(!model.getMethodInjections().isEmpty())
			{
				self.getFeature(IExecutionFeature.class).scheduleStep((Runnable)()->
				{
					for(IInjectionHandle handle: model.getMethodInjections())
					{
						handle.apply(self, pojos, context, null);
					}
				});
			}
			
			// TODO: wait for future return values?
			for(IInjectionHandle handle: model.getOnStart())
			{
				handle.apply(self, pojos, context, null);
			}
		}
	}

	@Override
	public void cleanup()
	{
		if(extras!=null)
		{
			for(List<Object> pojos: extras.keySet())
			{
				InjectionModel	model	= InjectionModel.get(pojos);
				endPojo(model, pojos, extras.get(pojos));
			}
		}
		
		endPojo(model, Collections.singletonList(self.getPojo()), null);
		
		// Notify on end -> conflict with terminate() in IComponentFactory.run(Object)
//		// Inform result subscribers, if any
//		if(rp!=null)
//		{
//			Map<String, Object>	results	= getResults();
//			if(results!=null)
//			{
//				for(String name: results.keySet())
//				{
//					rp.addResult(name, results.get(name));
//				}
//			}
//		}
	}
	
	//-------- IInjectionFeature interface --------
	
	@Override
	public void setResult(String name, Object value)
	{
		if(rp==null)
		{
			rp	= new ResultProvider();
		}
		rp.setResult(name, value);
	}
	
	@Override
	public void addListener(String name, IChangeListener listener)
	{
		if(listeners==null)
		{
			listeners	= new LinkedHashMap<>();
		}
		Set<IChangeListener>	set	= listeners.get(name);
		if(set==null)
		{
			set	= new LinkedHashSet<>();
			listeners.put(name, set);
		}
		set.add(listener);
	}
	
	@Override
	public void removeListener(String name, IChangeListener listener)
	{
		if(listeners!=null)
		{
			Set<IChangeListener>	set	= listeners.get(name);
			if(set!=null)
			{
				set.remove(listener);
				
				// Do some cleanup to speedup valueChanged()
				if(set.isEmpty())
				{
					listeners.remove(name);
					if(listeners.isEmpty())
					{
						listeners	= null;
					}
				}
			}
		}
	}
	
	//-------- internal methods (to be used by other features) --------
	
	/**
	 *  Get the underlying model.
	 */
	public InjectionModel	getModel()
	{
		return model;
	}
	
	/**
	 *  Notify about a result, i.e. a change in a dynamic result field.
	 */
	public void notifyResult(ChangeEvent event)
	{
		if(rp!=null)
		{
			rp.notifyResult(event);
		}
	}
	
	/**
	 *  Notify about a change in a dynamic field.
	 */
	public void valueChanged(ChangeEvent event)
	{
		// Generic handlers for e.g. sending result events.
		Set<Class<? extends Annotation>> kinds	= model.getDynamicValue(event.name()).kinds();
		for(Class<? extends Annotation> kind: kinds)
		{
			List<IChangeHandler>	handlers	= model.getChangeHandlers(kind);
			if(handlers!=null)
			{
				for(IChangeHandler handler: handlers)
				{
					try
					{
						handler.handleChange(self, event);
					}
					catch(Exception ex)
					{
						self.getLogger().log(Level.WARNING, "Exception in "+event.name()+" handler: "+handler+", "+ex);
					}
				}
			}
		}
		
		// Value specific listeners.
		if(listeners!=null)
		{
			Set<IChangeListener>	set	= listeners.get(event.name());
			if(set!=null)
			{
				for(IChangeListener l: set)
				{
					try
					{
						l.valueChanged(event);
					}
					catch(Exception ex)
					{
						self.getLogger().log(Level.WARNING, "Exception in "+event.name()+" listener: "+l+", "+ex);
					}
				}
			}
		}
	}
	
	/**
	 *  Add dependencies between dynamic values.
	 */
	public void	addDependencies(Dyn<?> dyn, String name, Set<String> dependencies)
	{
		IChangeListener	listener	= new IChangeListener()
		{
			Object	oldvalue	= dyn.get();

			@Override
			public void valueChanged(ChangeEvent event)
			{
				Object	newvalue	= dyn.get();
				if(!SUtil.equals(oldvalue, newvalue))
				{
					InjectionFeature.this.valueChanged(new ChangeEvent(ChangeEvent.Type.CHANGED, name, newvalue, oldvalue, null));
				}
				oldvalue	= newvalue;
			}
		};
		for(String dep: dependencies)
		{
			addListener(dep, listener);
		}
	}
	
	/**
	 *  Get the results as annotated with ProvideResult.
	 */
	public Map<String, Object>	getResults()
	{
		Map<String, Object> ret	= null;
		if(extras!=null)
		{
			for(List<Object> pojos: extras.keySet())
			{
				InjectionModel	model	= InjectionModel.get(pojos);
				ret = addResults(model, pojos, ret);
			}
		}
		ret = addResults(model, Collections.singletonList(self.getPojo()), ret);
		
		// Add manually added results last -> manual overwrites annotation, if any.
		if(rp!=null)
		{
			Map<String, Object>	results	= rp.getResultMap();
			if(ret==null)
			{
				ret	= results;
			}
			else
			{
				ret.putAll(results);
			}			
		}
		
		return ret;
	}

	/**
	 *  Add results from the given model and pojo list to the given map.
	 *  @return	The updated map.
	 */
	protected Map<String, Object> addResults(InjectionModel model, List<Object> pojos, Map<String, Object> ret)
	{
		if(model.getResultsFetcher()!=null)
		{
			@SuppressWarnings("unchecked")
			Map<String, Object>	results	= (Map<String, Object>) model.getResultsFetcher().apply(self, pojos, null, null);
			if(ret==null)
			{
				ret	= results;
			}
			else
			{
				ret.putAll(results);
			}
		}
		return ret;
	}
	
	/**
	 *  Subscribe to results of the component.
	 */
	public ISubscriptionIntermediateFuture<ChangeEvent> subscribeToResults()
	{
		if(rp==null)
		{
			rp	= new ResultProvider();
		}
		return rp.subscribeToResults();
	}
	
	/**
	 *  Add an extra pojo to be managed.
	 *  E.g. inject fields in service impl, call OnStart/End methods.
	 *  
	 *  @param pojos	The actual pojo objects as a hierarchy of component pojo plus subobjects.
	 *  				The injection is for the last pojo in the list.
	 *  
	 *  @param context	Optional local context of the pojo (e.g. rplan for a plan pojo).
	 */
	public void	addExtraObject(List<Object> pojos, Object context)
	{
		InjectionModel	model	= InjectionModel.get(pojos);
		
		if(extras==null)
		{
			extras	= new LinkedHashMap<>();
		}
		extras.put(pojos, context);
		
		startPojo(model, pojos, context);
	}

	
	/**
	 *  Remove an extra pojo.
	 *  E.g. inject fields in service impl, call OnStart/End methods.
	 *  
	 *  @param pojos	The actual pojo objects as a hierachy of component pojo plus subobjects.
	 *  				The injection is for the last pojo in the list.
	 *  
	 *  @param context	Optional local context of the pojo (e.g. rplan for a plan pojo).
	 */
	public void	removeExtraObject(List<Object> pojos, Object context)
	{
		// Might be called before added for removePlan when plan fails due to model not loadable.
		if(extras!=null && extras.containsKey(pojos))
		{
			extras.remove(pojos);
			InjectionModel	model	= InjectionModel.get(pojos);
			endPojo(model, pojos, context);
		}
	}
	
	/**
	 *  Perform end operations on object.
	 */
	protected void endPojo(InjectionModel model, List<Object> pojos, Object context)
	{
		// TODO: wait for future return values?
		if(model.getOnEnd()!=null)	// May be null when exception in model init()
		{
			for(IInjectionHandle handle: model.getOnEnd())
			{
	//			self.getFeature(IExecutionFeature.class).scheduleStep((Runnable)()->
					handle.apply(self, pojos, context, null);
			}
		}
	}
}
