package jadex.execution.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import jadex.collection.WeakKeyValueMap;
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
import jadex.future.IFuture;
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
	public <T extends Component> IFuture<IComponentHandle>	bootstrap(Class<T> type, Supplier<T> creator)
	{
		Map<Class<IComponentFeature>, ComponentFeatureProvider<IComponentFeature>>	providers	= SComponentFeatureProvider.getProvidersForComponent(type);
		Object	exeprovider	= providers.get(IExecutionFeature.class);	// Hack!!! cannot cast wtf???
		IExecutionFeature	exe	= ((ExecutionFeatureProvider)exeprovider).doCreateFeatureInstance();
		
		// Fast Lambda Agent -> optimized lifecycle
		if(FastLambda.class.isAssignableFrom(type))
		{
//			System.out.println("Creating fast lambda agent: "+type);
			exe.scheduleStep(() -> 
			{
				T self	= creator.get();
				@SuppressWarnings("unchecked")
				FastLambda<Object> fself	= (FastLambda<Object>)self;
				
				try
				{
					startFeatures(self);
					
					// run body and termination in same step as init
					try
					{
						/*ILifecycle lfeature = (ILifecycle)feature;
						System.out.println("starting: "+lfeature);
						lfeature.onStart();*/

						Object	result	= null;
						Object	pojo	= fself.getPojo();
						if(pojo instanceof Callable)
						{
							result	= ((Callable<?>)pojo).call();							
						}
						else if(pojo instanceof IThrowingFunction)
						{
							@SuppressWarnings("unchecked")
							IThrowingFunction<IComponent, T>	itf	= (IThrowingFunction<IComponent, T>)pojo;
							result	= itf.apply(self);							
						}
						else if(pojo instanceof Runnable)
						{
							((Runnable)pojo).run();							
						}
						else //if(pojo instanceof IThrowingConsumer)
						{
							@SuppressWarnings("unchecked")
							IThrowingConsumer<IComponent>	itc	= (IThrowingConsumer<IComponent>)pojo;
							itc.accept(self);							
						}
						
						if(fself.result!=null)
						{
							// TODO: unify with LambdaAgent result handle?
							fself.result.setResult(copyVal(result, getAnnos(pojo.getClass())));
						}
					}
					catch(Exception e)
					{
						System.err.println("Error in fast lambda agent 1: "+e);
						if(fself.result!=null)
						{
							fself.result.setException(e);
						}
						else
						{
							self.handleException(e);
						}
					}
					if(fself.terminate)
					{
						exe.scheduleStep((Runnable)() -> fself.terminate());
					}
				}
				catch(Exception e)
				{
					System.err.println("Error in fast lambda agent: "+e);
					if(fself.result!=null)
					{
						fself.result.setException(e);
					}
					else
					{
						self.handleException(e);
					}
					if(fself.terminate)
					{
						exe.scheduleStep((Runnable)() -> fself.terminate());
					}
				}
				catch(StepAborted e)
				{
					if(fself.result!=null)
					{
						fself.result.setException(self!=null && self.getException()!=null ? self.getException() : new RuntimeException(e));
					}
					throw e;
				}
			});
			
			// No handle needed, because the user only wait for the run() result
			return null;
		}
		
		// Normal component
		else
		{
			Future<IComponentHandle>	ret	= new Future<>();
			exe.scheduleStep(() -> 
			{
				T self	= null;
				try
				{
					self = creator.get();
					
					// Make component available as soon as possible
					ret.setResult(self.getComponentHandle());
					
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
			
			return ret;
		}
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
	public IFuture<IComponentHandle>	create(Object pojo, ComponentIdentifier cid, Application app)
	{
		IFuture<IComponentHandle>	ret;
		
		if(pojo instanceof Runnable)
		{
			ret = LambdaAgent.create((Runnable)pojo, cid, app);
		}
		else if(pojo instanceof Callable)
		{
			ret = LambdaAgent.create((Callable<?>)pojo, cid, app);
		}
		else if(pojo instanceof IThrowingFunction)
		{
			@SuppressWarnings("unchecked")
			IThrowingFunction<IComponent, ?>	itf	= (IThrowingFunction<IComponent, ?>)pojo;
			ret = LambdaAgent.create(itf, cid, app);
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
	
	protected static Map<ComponentIdentifier, IResultProvider>	results	= new WeakKeyValueMap<>();
	
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
	
	protected static Map<Class<?>, Annotation[]>	ANNOS	= new LinkedHashMap<>();
	
	/**
	 *  Get annotations from method return type to check for NoCopy.
	 */
	public static Annotation[]	getAnnos(Class<?> pojoclazz)
	{
		synchronized (ANNOS)
		{
			if(!ANNOS.containsKey(pojoclazz))
			{
				Method	m	= null;
				
				if(SReflect.isSupertype(IThrowingFunction.class, pojoclazz))
				{
					// Can be also explicitly declared with component type or just implicit (lambda) as object type
					try
					{
						m	= pojoclazz.getMethod("apply", IComponent.class);
					}
					catch(Exception e)
					{
						try
						{
							m	= pojoclazz.getMethod("apply", Object.class);
						}
						catch(Exception e2)
						{
						}
					}
				}
				else	// Callable
				{
					try
					{
						m	= pojoclazz.getMethod("call");
					}
					catch(Exception e)
					{
					}
				}
				
				ANNOS.put(pojoclazz, m!=null ? m.getAnnotatedReturnType().getAnnotations() : null);
			}
			return ANNOS.get(pojoclazz);
		}
	}
	
	@Override
	public <T> IFuture<T> run(Object pojo, ComponentIdentifier cid, Application app)
	{
		if(pojo instanceof Callable)
		{
			@SuppressWarnings("unchecked")
			Callable<T>	callable	= (Callable<T>)pojo;
			return LambdaAgent.run(callable);
		}
		else if(pojo instanceof IThrowingFunction)
		{
			@SuppressWarnings("unchecked")
			IThrowingFunction<IComponent, T>	itf	= (IThrowingFunction<IComponent, T>)pojo;
			return LambdaAgent.run(itf);
		}
		else if(pojo instanceof Runnable)
		{
			@SuppressWarnings("unchecked")
			IFuture<T>	ret	= (IFuture<T>) LambdaAgent.run((Runnable)pojo);
			return ret;
		}
		else if(pojo instanceof IThrowingConsumer)
		{
			@SuppressWarnings("unchecked")
			IThrowingConsumer<IComponent>	itc	= (IThrowingConsumer<IComponent>)pojo;
			@SuppressWarnings("unchecked")
			IFuture<T>	ret	= (IFuture<T>) LambdaAgent.run(itc);
			return ret;
		}
		else
		{
			return new Future<>(new RuntimeException("Cannot run lambda agent from: "+pojo));
		}
	}
}
