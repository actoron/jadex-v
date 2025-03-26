package jadex.execution.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import jadex.common.ICommand;
import jadex.common.IFilter;
import jadex.common.IParameterGuesser;
import jadex.common.NameValue;
import jadex.common.SAccess;
import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.common.SimpleParameterGuesser;
import jadex.common.transformation.traverser.FilterProcessor;
import jadex.common.transformation.traverser.ITraverseProcessor;
import jadex.common.transformation.traverser.SCloner;
import jadex.common.transformation.traverser.Traverser;
import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.ICallable;
import jadex.core.IComponent;
import jadex.core.IComponentFeature;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.IResultProvider;
import jadex.core.IThrowingConsumer;
import jadex.core.IThrowingFunction;
import jadex.core.InvalidComponentAccessException;
import jadex.core.ResultProvider;
import jadex.core.annotation.NoCopy;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.core.impl.IBootstrapping;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.core.impl.SComponentFeatureProvider;
import jadex.execution.ComponentMethod;
import jadex.execution.IExecutionFeature;
import jadex.execution.LambdaAgent;
import jadex.execution.future.FutureFunctionality;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;

public class ExecutionFeatureProvider extends ComponentFeatureProvider<IExecutionFeature>	implements IBootstrapping, IComponentLifecycleManager
{
	@NoCopy
	public static class ExecutableComponentHandle implements IComponentHandle
	{
		private final Component comp;
		public static Set<String> ALLOWED_METHODS = new HashSet<>();
		static
		{
			ALLOWED_METHODS.add("toString");
			ALLOWED_METHODS.add("hashCode");
			ALLOWED_METHODS.add("equals");
		}
		protected Object pojo;

		public ExecutableComponentHandle(Component comp)
		{
			this.comp = comp;
		}

		@Override
		public ComponentIdentifier getId()
		{
			return comp.getId();
		}

		@Override
		public String getAppId()
		{
			return comp.getAppId();
		}

		@Override
		public <T> T getPojoHandle(Class<T> type)
		{
			if(pojo==null)
			{
				synchronized(this)
				{
					if(pojo==null)
					{
						pojo = createPojo();
					}
				}
			}
			return (T)pojo;
		}

		protected Object createPojo()
		{
			try
			{
				Object pojo = comp.getPojo();
				
				Object proxy = new ByteBuddy()
					.subclass(pojo.getClass())
					.method(ElementMatchers.not(ElementMatchers.isAnnotatedWith(ComponentMethod.class)))
		            .intercept(InvocationHandlerAdapter.of(new InvocationHandler() 
		            {
		            	@Override
		            	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable 
		                {
		            		if(IComponentManager.get().getCurrentComponent()!=null && IComponentManager.get().getCurrentComponent().getId().equals(getId()))
		            		{   
		            			return invokeMethod(comp, pojo, SUtil.arrayToList(args), method);
		            		}
		            		else if(ALLOWED_METHODS.contains(method.getName()))
		            		{
		            			return invokeMethod(comp, pojo, SUtil.arrayToList(args), method);
		            		}
		            		else
		            		{
		            			throw new InvalidComponentAccessException(comp.getId());
		            		}
		                }
		            }))
					.method(ElementMatchers.isAnnotatedWith(ComponentMethod.class)) 
					.intercept(InvocationHandlerAdapter.of((Object target, Method method, Object[] args)->
					{
						IComponent caller = IComponentManager.get().getCurrentComponent();
						
						List<Object> myargs = new ArrayList<Object>(); 
						Class<?>[] ptypes = method.getParameterTypes();
						java.lang.annotation.Annotation[][] pannos = method.getParameterAnnotations();

						for(int p = 0; p < ptypes.length; p++)
						{
							if(isNoCopy(args[p]) || hasAnnotation(pannos[p], NoCopy.class))
							{
								myargs.add(args[p]);
							}
							else
							{
								myargs.add(SCloner.clone(args[p], procs));
							}
						}
						
						FutureFunctionality func = new FutureFunctionality()
						{
							@Override
							public <T> void scheduleForward(final ICommand<T> com, final T args)
							{
								// Don't reschedule if already on correct thread.
								if(caller==null || caller.getFeature(IExecutionFeature.class).isComponentThread())
								{
									com.execute(args);
								}
								else
								{
									//System.out.println("todo: scheduleDecoupledStep");
									caller.getFeature(IExecutionFeature.class).scheduleStep(agent ->
									{
										com.execute(args);
									});
								}
							}
							
							public Object handleResult(Object val) throws Exception
							{
								if(!isNoCopy(val) && !hasAnnotation(method.getAnnotatedReturnType().getAnnotations(), NoCopy.class))
				            	{
									Object val2 = SCloner.clone(val, procs);
									//System.out.println("cloned val: "+val+" "+val2+" "+(val==val2));
									val = val2;
				            	}
								
								return val;
							}
							
							public Object handleIntermediateResult(Object val) throws Exception
							{
								if(!isNoCopy(val) && !hasAnnotation(method.getAnnotatedReturnType().getAnnotations(), NoCopy.class))
				            	{
									val = SCloner.clone(val, procs);
									//System.out.println("cloned val: "+val);
				            	}
								return val;
							}
						};
						
						Future<Object> ret = FutureFunctionality.createReturnFuture(method, args, target.getClass().getClassLoader(), func);
						
						//final Object[] myargs = copyargs.toArray();
						
				        if(IComponentManager.get().getCurrentComponent()!=null && IComponentManager.get().getCurrentComponent().getId().equals(getId()))
				        {
				        	//System.out.println("already on agent: "+getId());
				        	if(ret instanceof IFuture)
				        	{
				        		IFuture fut = (IFuture)invokeMethod(comp, pojo, myargs, method);
				        		fut.delegateTo(ret);
				        	}
				        	else if(method.getReturnType().equals(void.class))
				        	{
				        		invokeMethod(comp, pojo, myargs, method);
				        	}
				        	else
				        	{
				        		//System.out.println("Agent methods must be async: "+method.getName()+" "+pojo);
				        		throw new InvalidComponentAccessException(comp.getId(), "Agent methods must be async: "+method.getName());
				        	}
				        }
				        else
				        {
				        	if(ret instanceof IFuture)
				        	{
					        	IFuture fut = scheduleAsyncStep(new ICallable<IFuture<Object>>() 
				        		{
				        			public Class<? extends IFuture<?>> getFutureReturnType() 
				        			{
				        				return (Class<? extends IFuture<?>>)ret.getClass();
				        			}
				        			
				        			public IFuture<Object> call() throws Exception
				        			{
				        				return (IFuture<Object>)invokeMethod(comp, pojo, myargs, method);
				        			}
								});
					        	fut.delegateTo(ret);
				        	}
				        	//System.out.println("scheduled on agent: "+getId());
				        	else if(method.getReturnType().equals(void.class))
				        	{
				        		scheduleStep(() -> invokeMethod(comp, pojo, myargs, method));
				        	}
				        	else 
				        	{
				        		//System.out.println("Agent methods must be async: "+method.getName()+" "+pojo);
				          		throw new InvalidComponentAccessException(comp.getId(), "Agent methods must be async: "+method.getName());
				        	}
				        }
				        
				        return ret;
					}))
					.make()
					.load(pojo.getClass().getClassLoader())
		            .getLoaded()
		            .getConstructor()
		            .newInstance();
				
				//System.out.println("proxy hashCode: "+proxy.hashCode());
				
				return proxy;
			}
			catch(Exception e)
			{
				SUtil.rethrowAsUnchecked(e);
			}
			return null;
		}

		protected Object invokeMethod(Component component, Object pojo, List<Object> args, Method method)
		{
			// Try to guess parameters from given args or component internals.
			IParameterGuesser guesser = new SimpleParameterGuesser(component.getValueProvider().getParameterGuesser(), args);
			Object[] iargs = new Object[method.getParameterTypes().length];
			for(int i=0; i<method.getParameterTypes().length; i++)
			{
				iargs[i] = guesser.guessParameter(method.getParameterTypes()[i], false);
			}
			
			try
			{
				// It is now allowed to use protected/private component methods
				SAccess.setAccessible(method, true);
				return method.invoke(pojo, iargs);
			}
			catch(Exception e)
			{
				SUtil.rethrowAsUnchecked(e);
			}
			return null;
		}

		protected boolean hasAnnotation(Annotation[] pannos, Class<? extends Annotation> anntype) 
		{
		    for (Annotation anno : pannos) 
		    {
		        if (anno.annotationType().equals(anntype)) 
		        {
		            return true;
		        }
		    }
		    return false;
		}

		@Override
		public <T> IFuture<T> scheduleStep(Callable<T> step) 
		{
			return comp.getFeature(IExecutionFeature.class).scheduleStep(step);
		}

		@Override
		public void scheduleStep(Runnable step) 
		{
			comp.getFeature(IExecutionFeature.class).scheduleStep(step);
		}

		@Override
		public <T> IFuture<T> scheduleStep(IThrowingFunction<IComponent, T> step)
		{
			return comp.getFeature(IExecutionFeature.class).scheduleStep(step);
		}

		@Override
		public void scheduleStep(IThrowingConsumer<IComponent> step)
		{
			comp.getFeature(IExecutionFeature.class).scheduleStep(step);
		}

		@Override
		public <T> IFuture<T> scheduleAsyncStep(Callable<IFuture<T>> step)
		{
			return comp.getFeature(IExecutionFeature.class).scheduleAsyncStep(step);
		}

		@Override
		public <T> IFuture<T> scheduleAsyncStep(IThrowingFunction<IComponent, IFuture<T>> step)
		{
			return comp.getFeature(IExecutionFeature.class).scheduleAsyncStep(step);
		}
	}

	static
	{		
		// Init the component with schedule step functionality (hack?!)
		Component.setExternalAccessFactory(comp ->
		{
			return new ExecutableComponentHandle(comp);
		}, true);
	}
	
	/**
	 *  Helper to skip NoCopy objects while cloning. 
	 */
	protected static IFilter<Object>	filter = new IFilter<Object>()
	{
		public boolean filter(Object object)
		{
			return isNoCopy(object);
		}
	};
	
	/**
	 *  Helper to skip NoCopy objects while cloning. 
	 */
	protected static List<ITraverseProcessor> procs = new ArrayList<>(Traverser.getDefaultProcessors());
	{
		procs.add(procs.size()-1, new FilterProcessor(filter));
	}
	
	/**
	 *  Helper method to check if a value doesn't need copying in component methods
	 */
	protected static boolean isNoCopy(Object val)
	{
		return val==null || val.getClass().isAnnotationPresent(NoCopy.class);
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
				try
				{
					@SuppressWarnings("unchecked")
					FastLambda<Object> self = (FastLambda<Object>)creator.get();
					startFeatures(self);
					
					// run body and termination in same step as init
					try
					{
						/*ILifecycle lfeature = (ILifecycle)feature;
						System.out.println("starting: "+lfeature);
						lfeature.onStart();*/
						
						Object	result	= self.getPojo().apply(self);
						if(self.result!=null)
							self.result.setResult(result);
					}
					catch(Exception e)
					{
						self.handleException(e);
					}
					if(self.terminate)
					{
						exe.scheduleStep((Runnable)() -> self.terminate());
					}
					
					@SuppressWarnings("unchecked")
					T t	= (T)self;
					ret.setResult(t);
				}
				catch(Exception e)
				{
					ret.setException(e);
				}
			});
		}
		
		// Normal component
		else
		{
			exe.scheduleStep(() -> 
			{
				try
				{
					T self = creator.get();
					startFeatures(self);
					ret.setResult(self);
				}
				catch(Exception e)
				{
					ret.setException(e);
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
			throw new RuntimeException("Cannot create lambda agent from: "+cid);
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
}
