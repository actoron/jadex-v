package jadex.execution.impl;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import jadex.common.IFilter;
import jadex.common.NameValue;
import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.common.transformation.traverser.FilterProcessor;
import jadex.common.transformation.traverser.ITraverseProcessor;
import jadex.common.transformation.traverser.SCloner;
import jadex.common.transformation.traverser.Traverser;
import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentFeature;
import jadex.core.IComponentHandle;
import jadex.core.IResultProvider;
import jadex.core.IThrowingConsumer;
import jadex.core.IThrowingFunction;
import jadex.core.ResultProvider;
import jadex.core.annotation.NoCopy;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.core.impl.IBootstrapping;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.core.impl.SComponentFeatureProvider;
import jadex.execution.IExecutionFeature;
import jadex.execution.LambdaAgent;
import jadex.execution.StepAborted;
import jadex.future.Future;
import jadex.future.ISubscriptionIntermediateFuture;

public class ExecutionFeatureProvider extends ComponentFeatureProvider<IExecutionFeature>	implements IBootstrapping, IComponentLifecycleManager
{
	static
	{		
		// Init the component with schedule step functionality (hack?!)
		Component.setExternalAccessFactory(comp ->
		{
			return new ExecutableComponentHandle(comp);
		}, true);
	}

	@Override
	public Class<IExecutionFeature> getFeatureType()
	{
		return IExecutionFeature.class;
	}

	@Override
	public IExecutionFeature createFeatureInstance(Component self)
	{
		ExecutionFeature	ret	= ExecutionFeature.LOCAL.get();

		// Component created without bootstrapping
		// TODO: disallow plain component creation?
		if(ret==null || ret.self!=null)
		{
			ret = doCreateFeatureInstance();				
		}
		// else inside bootstrap -> reuse bootstrap feature
		
		assert ret.self==null;
		ret.self	= self;
		return ret;
	}

	/**
	 *  Template method allowing subclasses to provide a subclass of the feature implementation.
	 */
	protected ExecutionFeature doCreateFeatureInstance()
	{
		return new ExecutionFeature();
	}
	
	@Override
	public <T extends Component> T	bootstrap(Class<T> type, Supplier<T> creator)
	{
		Map<Class<IComponentFeature>, ComponentFeatureProvider<IComponentFeature>>	providers	= SComponentFeatureProvider.getProvidersForComponent(type);
		Object	exeprovider	= providers.get(IExecutionFeature.class);	// Hack!!! cannot cast wtf???
		IExecutionFeature	exe	= ((ExecutionFeatureProvider)exeprovider).doCreateFeatureInstance();
		Future<T>	ret	= new Future<>();

		// Fast Lambda Agent -> optimized lifecycle
		if(SReflect.isSupertype(FastLambda.class, type))
		{
			exe.scheduleStep(() -> 
			{
				T self	= null;
				try
				{
					self	= creator.get();
					@SuppressWarnings("unchecked")
					FastLambda<Object> fself	= (FastLambda<Object>)self;
					
					// Make component available as soon as possible
					ret.setResult(self);
					
					startFeatures(self);
					
					// run body and termination in same step as init
					try
					{
						/*ILifecycle lfeature = (ILifecycle)feature;
						System.out.println("starting: "+lfeature);
						lfeature.onStart();*/
						
						Object	result	= fself.getPojo().apply(self);
						if(fself.result!=null)
							fself.result.setResult(result);
					}
					catch(Exception e)
					{
						self.handleException(e);
					}
					if(fself.terminate)
					{
						exe.scheduleStep((Runnable)() -> fself.terminate());
					}
				}
				catch(Exception e)
				{
					ret.setExceptionIfUndone(e);
					if(self!=null)
					{
						self.terminate();
					}
				}
				catch(StepAborted e)
				{
					ret.setExceptionIfUndone(self!=null && self.getException()!=null ? self.getException() : new RuntimeException(e));
					throw e;
				}
			});
		}
		
		// Normal component
		else
		{
			exe.scheduleStep(() -> 
			{
				T self	= null;
				try
				{
					self = creator.get();
					
					// Make component available as soon as possible
					ret.setResult(self);
					
					startFeatures(self);
				}
				catch(Exception e)
				{
					if(ret.setExceptionIfUndone(e))
					{
						if(self!=null)
						{
							self.terminate();
						}
					}
					else
					{
						SUtil.throwUnchecked(e);
					}
				}
				catch(StepAborted e)
				{
					ret.setExceptionIfUndone(self!=null && self.getException()!=null ? self.getException() : new RuntimeException(e));
					throw e;
				}
			});
		}
		
		return ret.get();			
	}
	
	/**
	 *  Start all features, i.e. thos that implement ILifecycle.
	 */
	protected <T extends Component> void startFeatures(T self)
	{
		for(Object feature:	self.getFeatures())
		{
			if(feature instanceof ILifecycle)
			{
				ILifecycle lfeature = (ILifecycle)feature;
				//System.out.println("starting: "+lfeature);
				lfeature.onStart();
			}
		}
	}
	
	
	@Override
	public int	isCreator(Class<?> pojoclazz)
	{
		return Runnable.class.isAssignableFrom(pojoclazz)
			|| Callable.class.isAssignableFrom(pojoclazz)
			|| IThrowingFunction.class.isAssignableFrom(pojoclazz)
			|| IThrowingConsumer.class.isAssignableFrom(pojoclazz)
			? 1 : -1;
	}
	
	@Override
	public IComponentHandle create(Object pojo, ComponentIdentifier cid, Application app)
	{
		IComponentHandle ret;
		
		if(pojo instanceof Runnable)
		{
			ret = LambdaAgent.create((Runnable)pojo, cid, app);
		}
		else if(pojo instanceof Callable)
		{
			ret = LambdaAgent.create((Callable<?>)pojo, cid, app).component();
		}
		else if(pojo instanceof IThrowingFunction)
		{
			@SuppressWarnings("unchecked")
			IThrowingFunction<IComponent, ?>	itf	= (IThrowingFunction<IComponent, ?>)pojo;
			ret = LambdaAgent.create(itf, cid, app).component();
		}
		else if(pojo instanceof IThrowingConsumer)
		{
			@SuppressWarnings("unchecked")
			IThrowingConsumer<IComponent>	itc	= (IThrowingConsumer<IComponent>)pojo;
			ret = LambdaAgent.create(itc, cid, app);
		}
		else
		{
			throw new RuntimeException("Cannot create lambda agent from: "+pojo);
		}
	
		return ret;
	}
	
	@Override
	public void terminate(IComponent component) 
	{
		((IInternalExecutionFeature)component.getFeature(IExecutionFeature.class)).terminate();
	}
	
	protected static Map<ComponentIdentifier, IResultProvider>	results	= new WeakHashMap<>();
	
	@Override
	public Map<String, Object> getResults(IComponent comp)
	{
		IResultProvider	rp;
		synchronized(results)
		{
			rp = results.get(comp.getId());
		}
		return rp==null ? null : rp.getResultMap();
	}
	
	@Override
	public ISubscriptionIntermediateFuture<NameValue> subscribeToResults(IComponent comp)
	{
		IResultProvider	rp;
		synchronized(results)
		{
			rp = results.get(comp.getId());
			if(rp==null)
			{
				rp	= new ResultProvider();
				results.put(comp.getId(), rp);
			}
		}
		return rp.subscribeToResults();
	}

	public static void	addResult(ComponentIdentifier id, String name, Object value)
	{
		IResultProvider	rp;
		synchronized(results)
		{
			rp = results.get(id);
			if(rp==null)
			{
				rp	= new ResultProvider();
				results.put(id, rp);
			}
		}
		rp.addResult(name, value);
	}

	public static void addResultHandler(ComponentIdentifier id, IResultProvider provider)
	{
		synchronized(results)
		{
			if(results.containsKey(id))
			{
				throw new IllegalStateException("Result provider already added: "+results.get(id)+", "+provider);
			}
			results.put(id, provider);
		}
	}

	/**
	 *  Helper to skip NoCopy objects while cloning. 
	 */
	protected static List<ITraverseProcessor> procs = new ArrayList<>(Traverser.getDefaultProcessors());
	{
		procs.add(procs.size()-1, new FilterProcessor(new IFilter<Object>()
		{
			public boolean filter(Object object)
			{
				return isNoCopy(object);
			}
		}));
	}
	
	/**
	 *  Helper method to check if a value doesn't need copying in component methods
	 */
	public static Object copyVal(Object val, Annotation... annos)
	{
		return isNoCopy(val, annos) ? val : SCloner.clone(val, procs);
	}
	
	/**
	 *  Helper method to check if a value doesn't need copying in component methods
	 */
	public static boolean isNoCopy(Object val, Annotation... annos)
	{
		if(val==null || val.getClass().isAnnotationPresent(NoCopy.class))
		{
			return true;
		}
		else
		{
			for(Annotation anno: annos)
			{
				if(anno instanceof NoCopy)
				{
					return true;
				}
			}
		}
		return false;
	}
}
