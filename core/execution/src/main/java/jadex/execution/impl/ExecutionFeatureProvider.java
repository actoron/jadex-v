package jadex.execution.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

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
import jadex.core.IComponentHandle;
import jadex.core.IResultProvider;
import jadex.core.IThrowingConsumer;
import jadex.core.IThrowingFunction;
import jadex.core.ResultProvider;
import jadex.core.annotation.NoCopy;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.core.impl.ComponentManager;
import jadex.core.impl.IBootstrapping;
import jadex.core.impl.IComponentLifecycleManager;
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

		// TODO: disallow plain component creation?
		if(ret==null || ret.self!=self)
		{
			ret = doCreateFeatureInstance(self);				
		}
		// else inside bootstrap -> reuse bootstrap feature
		
		return ret;
	}

	/**
	 *  Template method allowing subclasses to provide a subclass of the feature implementation.
	 */
	protected ExecutionFeature doCreateFeatureInstance(Component self)
	{
		return new ExecutionFeature(self);
	}
	
	@Override
	public <T extends Component> IFuture<IComponentHandle>	bootstrap(T component)
	{
		IExecutionFeature	exe	= doCreateFeatureInstance(component);
		
		// Fast Lambda Agent -> optimized lifecycle
		if(component instanceof FastLambda)
		{
			ComponentManager.get().increaseCreating(component.getApplication());
//			System.out.println("Creating fast lambda agent: "+type);
			exe.scheduleStep(() -> 
			{
				@SuppressWarnings("unchecked")
				FastLambda<Object> fself	= (FastLambda<Object>)component;
				try
				{
					// Extra init so component doesn't get added when just created as object
					fself.init();
					
					// Extra feature init, so subcomponent can override init() before features are initialized
					fself.initFeatures();
					
					// run body and termination in same step as init
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
						result	= itf.apply(fself);							
					}
					else if(pojo instanceof Runnable)
					{
						((Runnable)pojo).run();							
					}
					else //if(pojo instanceof IThrowingConsumer)
					{
						@SuppressWarnings("unchecked")
						IThrowingConsumer<IComponent>	itc	= (IThrowingConsumer<IComponent>)pojo;
						itc.accept(fself);							
					}
					
					// TODO: unify with LambdaAgent result handle?
					fself.result.setResult(copyVal(result, getAnnos(pojo.getClass())));

					if(!FastLambda.KEEPALIVE)
					{
						exe.scheduleStep((Runnable)() -> fself.terminate());
					}
				}
				catch(Exception e)
				{
					fself.result.setException(e);
					if(!FastLambda.KEEPALIVE)
					{
						exe.scheduleStep((Runnable)() -> fself.terminate());
					}
				}
				catch(StepAborted e)
				{
					fself.result.setException(fself.getException()!=null ? fself.getException() : new RuntimeException(e));
					throw e;
				}
				finally
				{
					ComponentManager.get().decreaseCreating(fself.getId(), fself.getApplication());
				}
			});
			
			// No handle needed, because the user only waits for the run() result
			return null;
		}
		
		// Normal component
		else
		{
			ComponentManager.get().increaseCreating(component.getApplication());
			Future<IComponentHandle>	ret	= new Future<>();
			exe.scheduleStep(() -> 
			{
				try
				{
					// Extra init so component doesn't get added when just created as object
					component.init();
					
					// Extra feature init, so subcomponent can override init() before features are initialized
					component.initFeatures();
					
					// Make component available after init is complete
					ret.setResult(component.getComponentHandle());
				}
				catch(Exception e)
				{
					if(ret.setExceptionIfUndone(e))
					{
						component.terminate();
					}
					else
					{
						SUtil.throwUnchecked(e);
					}
				}
				catch(StepAborted e)
				{
					ret.setExceptionIfUndone(component.getException()!=null ? component.getException() : new RuntimeException(e));
					throw e;
				}
				finally
				{
					ComponentManager.get().decreaseCreating(component.getId(), component.getApplication());
				}
			});
			
			return ret;
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
		if(pojo instanceof Callable
		|| pojo instanceof IThrowingFunction
		|| pojo instanceof Runnable
		|| pojo instanceof IThrowingConsumer)
		{
			Future<T>	ret	= new Future<>();
			Component.createComponent(new FastLambda<>(pojo, cid, app, ret));
			return ret;
		}
		else
		{
			return new Future<>(new RuntimeException("Cannot run lambda agent from: "+pojo));
		}
	}
}
