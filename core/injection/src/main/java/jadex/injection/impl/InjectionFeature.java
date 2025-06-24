package jadex.injection.impl;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jadex.common.NameValue;
import jadex.core.IComponent;
import jadex.core.ResultProvider;
import jadex.execution.IExecutionFeature;
import jadex.execution.impl.ILifecycle;
import jadex.future.ISubscriptionIntermediateFuture;
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
	
	/** Managed extra objects (e.g. services implemented as separate class). */
	protected List<List<Object>>	extras;
	
	protected ResultProvider	rp;
	
	/**
	 *  Create the injection feature.
	 */
	public InjectionFeature(IComponent self)
	{
		this.self	= self;
	}
	
	//-------- lifecycle methods --------
	
	@Override
	public void onStart()
	{
		List<Object> pojos	= Collections.singletonList(self.getPojo());
		this.model	= InjectionModel.get(pojos, null, null);
		startPojo(model, pojos, null);		
	}
	
	/**
	 *  Perform init and start operations for the given object.
	 */
	protected void startPojo(InjectionModel model, List<Object> pojos, Object context)
	{
		if(model.getPreInject()!=null)
		{
			model.getPreInject().apply(self, pojos, context, null);
		}

		if(model.getFieldInjections()!=null)
		{
			model.getFieldInjections().apply(self, pojos, context, null);
		}
		
		if(model.getPostInject()!=null)
		{
			model.getPostInject().apply(self, pojos, context, null);
		}

		// Hack!!! Start async when component pojo to allow agents with never ending main loops (e.g. quiz, simple cleaner, ...)
		if(pojos.size()==1)
		{
			if(model.getOnStart()!=null)
			{
				// TODO: wait for future return value?
				self.getFeature(IExecutionFeature.class).scheduleStep((Runnable)()->
					model.getOnStart().apply(self, pojos, context, null));
			}

			if(model.getMethodInjections()!=null)
			{
				// TODO: wait for future return value?
				self.getFeature(IExecutionFeature.class).scheduleStep((Runnable)()->
					model.getMethodInjections().apply(self, pojos, context, null));
			}
		}
		
		// Start sync when extra pojo to simplify, e.g. bdi plans
		else
		{
			// Schedule method injections first, in case OnStart blocks
			if(model.getMethodInjections()!=null)
			{
				// TODO: wait for future return value?
				self.getFeature(IExecutionFeature.class).scheduleStep((Runnable)()->
					model.getMethodInjections().apply(self, pojos, context, null));
			}
			
			if(model.getOnStart()!=null)
			{
				// TODO: wait for future return value?
				model.getOnStart().apply(self, pojos, context, null);
			}
		}
	}

	@Override
	public void onEnd()
	{
		if(extras!=null)
		{
			for(List<Object> pojos: extras)
			{
				InjectionModel	model	= InjectionModel.get(pojos, null, null);
				if(model.getOnEnd()!=null)
				{	
					model.getOnEnd().apply(self, pojos, null, null);
				}
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
	public void addResult(String name, Object value)
	{
		if(rp==null)
		{
			rp	= new ResultProvider();
		}
		rp.addResult(name, value);
	}
	
	//-------- internal methods (to be used by other features) --------
	
	/**
	 *  Get the results as annotated with Provide or ProvideResult.
	 */
	public Map<String, Object>	getResults()
	{
		Map<String, Object> ret	= null;
		if(extras!=null)
		{
			// Go backwards through list so outer objects overwrite conflicting results of inner objects.
			for(List<Object> pojos: extras.reversed())
			{
				InjectionModel	model	= InjectionModel.get(pojos, null, null);
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
			}
		}
		
		if(model.getResultsFetcher()!=null)
		{
			@SuppressWarnings("unchecked")
			Map<String, Object>	results	= (Map<String, Object>) model.getResultsFetcher().apply(self, Collections.singletonList(self.getPojo()), null, null);
			if(ret==null)
			{
				ret	= results;
			}
			else
			{
				ret.putAll(results);
			}
		}
		
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
	 *  Subscribe to results of the component.
	 */
	public ISubscriptionIntermediateFuture<NameValue> subscribeToResults()
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
	 *  @param pojos	The actual pojo objects as a hierachy of component pojo plus subobjects.
	 *  				The injection is for the last pojo in the list.
	 *  
	 *  @param context	Optional path information, e.g. name of subcapability(s).
	 *  
	 *  @param context	Optional local context of the pojo (e.g. rplan for a plan pojo).
	 *  
	 *  @param contextfetchers	Local fetchers, if any.
	 */
	public void	addExtraObject(List<Object> pojos, List<String> path, Object context, Map<Class<? extends Annotation>,List<IValueFetcherCreator>> contextfetchers)
	{
		if(extras==null)
		{
			extras	= new ArrayList<List<Object>>(4);
		}
		extras.add(pojos);
		
		InjectionModel	model	= InjectionModel.get(pojos, path, contextfetchers);
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
		InjectionModel	model	= InjectionModel.get(pojos, null, null);
		if(extras!=null)
		{
			extras.remove(pojos);
		}
		
		endPojo(model, pojos, context);
	}
	
	/**
	 *  Perform end operations on object.
	 */
	protected void endPojo(InjectionModel model, List<Object> pojos, Object context)
	{
		if(model.getOnEnd()!=null)
		{
			// TODO: wait for future return value?
//			self.getFeature(IExecutionFeature.class).scheduleStep((Runnable)()->
				model.getOnEnd().apply(self, pojos, context, null);
		}
	}
}
