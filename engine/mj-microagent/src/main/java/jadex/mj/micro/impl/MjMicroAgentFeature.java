package jadex.mj.micro.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;

import jadex.common.FieldInfo;
import jadex.common.IParameterGuesser;
import jadex.common.IValueFetcher;
import jadex.common.MethodInfo;
import jadex.common.SAccess;
import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.common.SimpleParameterGuesser;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.javaparser.SJavaParser;
import jadex.javaparser.SimpleValueFetcher;
import jadex.mj.core.IMjModelFeature;
import jadex.mj.core.IParameterGuesserProvider;
import jadex.mj.core.annotation.OnEnd;
import jadex.mj.core.annotation.OnStart;
import jadex.mj.feature.execution.IMjExecutionFeature;
import jadex.mj.feature.execution.impl.IMjLifecycle;
import jadex.mj.micro.InjectionInfoHolder;
import jadex.mj.micro.MicroModel;
import jadex.mj.micro.MjMicroAgent;

public class MjMicroAgentFeature	implements IMjLifecycle
{
	public static MjMicroAgentFeature get()
	{
		return IMjExecutionFeature.get().getComponent().getFeature(MjMicroAgentFeature.class);
	}

	protected MjMicroAgent	self;
	
	protected IParameterGuesser	guesser;
	
	protected MjMicroAgentFeature(MjMicroAgent self)
	{
		this.self	= self;
	}
	
	public MjMicroAgent getSelf()
	{
		return self;
	}

	@Override
	public IFuture<Void> onStart()
	{
		Future<Void> ret = new Future<Void>();
		//System.out.println("start: "+getSelf());
		injectStuff(getSelf(), getSelf().getPojo(), ((MicroModel)getSelf().getModel().getRawModel()).getInjectionInfoHolder())
			.then(v ->
		{
			MicroModel model = (MicroModel)getSelf().getModel().getRawModel();
			
			Class<? extends Annotation> ann = OnStart.class;
			if(model.getAgentMethod(ann)!=null)
			{
				//return invokeMethod(getInternalAccess(), OnInit.class, null);
				//if(wasAnnotationCalled(ann))
				//	return IFuture.DONE;
				//else
				invokeMethod(getSelf(), ann, null).delegateTo(ret);
			}
			else
			{
				ret.setResult(null);
				//ret.setException(new RuntimeException("no oninit found"));
				//return invokeMethod(getInternalAccess(), AgentCreated.class, null);
			}
		}).catchEx(ret);
		return ret;
	}
	
	/*@Override
	public IFuture<Void> onBody()
	{
		System.out.println("body: "+self);
		//IMjLifecycleFeature.of(self).terminate();
		
		//invokeServices();
		
		MicroModel model = (MicroModel)self.getModel().getRawModel();
		
		Class<? extends Annotation> ann = OnStart.class;
		if(model.getAgentMethod(ann)!=null)
		{
			//return invokeMethod(getInternalAccess(), OnInit.class, null);
			//if(wasAnnotationCalled(ann))
			//	return IFuture.DONE;
			//else
			return invokeMethod(self, ann, null);
		}
		else
		{
			throw new RuntimeException("no onstart found");
			//return invokeMethod(getInternalAccess(), AgentBody.class, null);
		}
	}*/
	
	@Override
	public IFuture<Void> onEnd()
	{
		//System.out.println("end: "+getSelf());
		
		MicroModel model = (MicroModel)getSelf().getModel().getRawModel();
		
		Class<? extends Annotation> ann = OnEnd.class;
		if(model.getAgentMethod(ann)!=null)
		{
			//return invokeMethod(getInternalAccess(), OnInit.class, null);
			//if(wasAnnotationCalled(ann))
			//	return IFuture.DONE;
			//else
			return invokeMethod(getSelf(), ann, null);
		}
		else
		{
			//throw new RuntimeException("no onend found");
			//return invokeMethod(getInternalAccess(), AgentBody.class, null);
			return Future.DONE;
		}
	}
	
	/**
	 *  Called just before the agent is removed from the platform.
	 *  @return The result of the component.
	 * /
	public IFuture<Void> shutdown()
	{
		boolean debug	= component instanceof IPlatformComponentAccess && ((IPlatformComponentAccess)component).getPlatformComponent().debug;
		if(debug)
		{
			component.getLogger().severe("lifecycle feature shutdown start: "+getComponent());
		}
			
		final Future<Void> ret = new Future<Void>();
		
		MicroModel model = (MicroModel)component.getModel().getRawModel();
		
		IFuture<Void> fut;
		Class<? extends Annotation> ann = OnEnd.class;
		if(model.getAgentMethod(ann)!=null)
		{
			//return invokeMethod(getInternalAccess(), OnInit.class, null);
			if(wasAnnotationCalled(ann))
			{
				fut = IFuture.DONE;
				if(debug)
				{
					component.getLogger().severe("lifecycle feature shutdown method already invoked: "+getComponent());
				}
			}
			else
			{
				fut = invokeMethod(getInternalAccess(), ann, null);
				if(debug)
				{
					component.getLogger().severe("lifecycle feature shutdown method invoked: "+getComponent()+" done="+fut.isDone());
				}
			}
		}
		else
		{
			throw new RuntimeException("no onend found");
			/*fut = invokeMethod(getInternalAccess(), AgentKilled.class, null);
			if(debug)
			{
				component.getLogger().severe("lifecycle feature shutdown agent killed invoked: "+getComponent()+" done="+fut.isDone());
			}* /
		}
		
		fut.addResultListener(new IResultListener<Void>()
		{
			public void resultAvailable(Void result)
			{
				if(debug)
				{
					component.getLogger().severe("lifecycle feature shutdown end result: "+getComponent());
				}
				proceed(null);
			}
			
			public void exceptionOccurred(Exception exception)
			{
				if(debug)
				{
					component.getLogger().severe("lifecycle feature shutdown end exception: "+getComponent()+"\n"+SUtil.getExceptionStacktrace(exception));
				}
				proceed(exception);
			}
			
			protected void proceed(Exception e)
			{
				try
				{
					MicroModel micromodel = (MicroModel)getComponent().getModel().getRawModel();
					Object agent = getPojoAgent();
					
					for(String name: micromodel.getResultInjectionNames())
					{
						Tuple3<FieldInfo, String, String> inj = micromodel.getResultInjection(name);
						Field field = inj.getFirstEntity().getField(getComponent().getClassLoader());
						String convback = inj.getThirdEntity();
						
						SAccess.setAccessible(field, true);
						Object val = field.get(agent);
						
						if(convback!=null)
						{
							SimpleValueFetcher fetcher = new SimpleValueFetcher(getComponent().getFetcher());
							fetcher.setValue("$value", val);
							val = SJavaParser.evaluateExpression(convback, getComponent().getModel().getAllImports(), fetcher, getComponent().getClassLoader());
						}
						
						getComponent().getFeature(IArgumentsResultsFeature.class).getResults().put(name, val);
					}
				}
				catch(Exception e2)
				{
					ret.setException(e2);
//					throw new RuntimeException(e2);
				}
				
				if(!ret.isDone())
				{
					if(e!=null)
					{
						ret.setException(e);
					}
					else
					{
						ret.setResult(null);
					}
				}
			}
		});
		
		return ret;
	}*/
	
	/**
	 *  Inject according to the annotations.
	 *  @param component The component.
	 *  @param target The target.
	 */
	public static IFuture<Void> injectStuff(MjMicroAgent component, Object target, InjectionInfoHolder holder)
	{
		final Future<Void> ret = new Future<>();
		
		//Map<String, Object>	args = component.getFeature(IArgumentsResultsFeature.class).getArguments();
		//Map<String, Object>	results	= component.getFeature(IArgumentsResultsFeature.class).getResults();
		//final MicroModel model = (MicroModel)component.getModel().getRawModel();

		try
		{
			// Inject agent fields.
			FieldInfo[] fields = holder.getAgentInjections();
			for(int i=0; i<fields.length; i++)
			{
				Field f = fields[i].getField(component.getClass().getClassLoader());
				SAccess.setAccessible(f, true);
				f.set(target, component);
			}
			ret.setResult(null);
	
			// Inject argument values
			/*if(args!=null)
			{
				String[] names = holder.getArgumentInjectionNames();
				if(names.length>0)
				{
					for(int i=0; i<names.length; i++)
					{
						if(args.containsKey(names[i]))
						{
							Object val = args.get(names[i]);
							
		//					if(val!=null || getModel().getArgument(names[i]).getDefaultValue()!=null)
							final Tuple2<FieldInfo, String>[] infos = holder.getArgumentInjections(names[i]);
							
							for(int j=0; j<infos.length; j++)
							{
								Field field = infos[j].getFirstEntity().getField(component.getClassLoader());
								String convert = infos[j].getSecondEntity();
//								System.out.println("setting arg: "+names[i]+" "+val);
								setFieldValue(component, target, val, field, convert);
							}
						}
					}
				}
			}*/
			
			// Inject default result values
			/*if(results!=null)
			{
				String[] names = holder.getResultInjectionNames();
				if(names.length>0)
				{
					for(int i=0; i<names.length; i++)
					{
						if(results.containsKey(names[i]))
						{
							Object val = results.get(names[i]);
							final Tuple3<FieldInfo, String, String> info = holder.getResultInjection(names[i]);
							
							Field field = info.getFirstEntity().getField(component.getClassLoader());
							String convert = info.getSecondEntity();
//							System.out.println("seting res: "+names[i]+" "+val);
							setFieldValue(component, target, val, field, convert);
						}
					}
				}
			}*/
			
			// Inject feature fields.
			/*fields = holder.getFeatureInjections();
			for(int i=0; i<fields.length; i++)
			{
				Class<?> iface = component.getClassLoader().loadClass(fields[i].getTypeName());
				Object feat = component.getFeature(iface);
				Field f = fields[i].getField(component.getClassLoader());
				SAccess.setAccessible(f, true);
				f.set(target, feat);
			}*/
			
			// Inject parent
			/*final FieldInfo[]	pis	= holder.getParentInjections();
			if(pis.length>0)
			{
//				IComponentManagementService cms = getComponent().getFeature(IRequiredServicesFeature.class).getLocalService(new ServiceQuery<>(IComponentManagementService.class));
				IExternalAccess exta = component.getExternalAccess(component.getId().getParent());
				final CounterResultListener<Void> lis = new CounterResultListener<Void>(pis.length, new DelegationResultListener<Void>(ret));
				
				for(int i=0; i<pis.length; i++)
				{
					final Field	f	= pis[i].getField(component.getClassLoader());
					if(IExternalAccess.class.equals(f.getType()))
					{
						try
						{
							SAccess.setAccessible(f, true);
							f.set(target, exta);
							lis.resultAvailable(null);
						}
						catch(Exception e)
						{
							ret.setException(e);
						}
					}
					else if(component.getDescription().isSynchronous())
					{
						exta.scheduleStep(new IComponentStep<Void>()
						{
							public IFuture<Void> execute(IInternalAccess ia)
							{
								Object pagent = ia.getFeature(IPojoComponentFeature.class).getPojoAgent();
								if(SReflect.isSupertype(f.getType(), pagent.getClass()))
								{
									try
									{
										SAccess.setAccessible(f, true);
										f.set(target, pagent);
										lis.resultAvailable(null);
									}
									catch(Exception e)
									{
										ret.setException(e);
									}
								}
								else
								{
									throw new RuntimeException("Incompatible types for parent injection: "+pagent+", "+f);													
								}
								return IFuture.DONE;
							}
						});
					}
					else
					{
						ret.setException(new RuntimeException("Non-external parent injection for non-synchronous subcomponent not allowed: "+f));
					}
				}
			}
			else
			{
				ret.setResult(null);
			}*/
		}
		catch(Exception e)
		{
			ret.setException(e);
		}
		
		return ret;
	}
	
	//-------- helper methods --------
	
	/**
	 *  Set an injected field value.
	 */
	protected static void setFieldValue(MjMicroAgent component, Object target, Object val, Field field, String convert)
	{
		if(val!=null || !SReflect.isBasicType(field.getType()))
		{
			try
			{
				//Object agent = component.getFeature(IPojoComponentFeature.class).getPojoAgent();
				if(convert!=null)
				{
					SimpleValueFetcher fetcher = new SimpleValueFetcher(component.getFeature(IMjModelFeature.class).getFetcher());
					fetcher.setValue("$value", val);
					val = SJavaParser.evaluateExpression(convert, component.getModel().getAllImports(), fetcher, component.getClass().getClassLoader());
				}
				SAccess.setAccessible(field, true);
//				if(field.getName().equals("address"))
//					System.out.println("setVal: "+getComponent().getId()+" "+val+" "+field.getName());
				//field.set(agent, val);
				field.set(target, val);
			}
			catch(Exception e)
			{
				throw SUtil.throwUnchecked(e);
			}
		}
	}
	
	
	// todo: parameter guesser
	/**
	 *  Invoke an agent method by injecting required arguments.
	 */
	public static IFuture<Void> invokeMethod(MjMicroAgent component, Class<? extends Annotation> ann, Object[] args)
	{
		IFuture<Void> ret;
		
		MicroModel	model = (MicroModel)component.getModel().getRawModel();
		MethodInfo	mi	= model.getAgentMethod(ann);
		if(mi!=null)
		{
			Method	method	= null;
			try
			{
				Object pojo = component.getPojo();
				method = mi.getMethod(pojo.getClass().getClassLoader());
				
				// Try to guess parameters from given args or component internals.
				IParameterGuesser guesser	= args!=null ? new SimpleParameterGuesser(component.getFeature(IMjModelFeature.class).getParameterGuesser(), Arrays.asList(args)) : component.getFeature(IMjModelFeature.class).getParameterGuesser();
				Object[]	iargs	= new Object[method.getParameterTypes().length];
				for(int i=0; i<method.getParameterTypes().length; i++)
				{
					iargs[i]	= guesser.guessParameter(method.getParameterTypes()[i], false);
				}
				
				try
				{
					// It is now allowed to use protected/private agent created, body, terminate methods
					SAccess.setAccessible(method, true);
					Object res = method.invoke(pojo, iargs);
					if(res instanceof IFuture)
					{
						ret	= (IFuture<Void>)res;
					}
					else
					{
						ret	= IFuture.DONE;
					}
				}
				catch(Exception e)
				{
					if(e instanceof InvocationTargetException)
					{
						if(((InvocationTargetException)e).getTargetException() instanceof Exception)
						{
							e	= (Exception)((InvocationTargetException)e).getTargetException();
						}
						else if(((InvocationTargetException)e).getTargetException() instanceof Error)
						{
							// re-throw errors, e.g. StepAborted
							throw (Error)((InvocationTargetException)e).getTargetException();
						}
					}
					ret	= new Future<Void>(e);
				}

			}
			catch(Exception e)
			{
				// Error in method search or parameter guesser
				if(method==null)
				{
					ret	= new Future<Void>(new RuntimeException("Cannot find method: "+mi, e));
				}
				else
				{
					ret	= new Future<Void>(new RuntimeException("Cannot inject values for method: "+method, e));
				}
			}
		}
		else
		{
			ret	= IFuture.DONE;
		}
		
		return ret;
	}
	
	/**
	 *  Check if a method using an annotation was already invoked.
	 *  @param ann The annotation.
	 *  @return True, if it was already called.
	 * /
	public boolean wasAnnotationCalled(Class<? extends Annotation> ann)
	{
		Object pojo = self.getPojo();
		Map<Object, Set<String>> invocs = (Map<Object, Set<String>>)Starter.getPlatformValue(component.getId(), Starter.DATA_INVOKEDMETHODS);
		Set<String> invans = invocs.get(pojo);
		if(invans!=null && invans.contains(SReflect.getUnqualifiedClassName(ann)))
		{
			return true;
		}
		else
		{
			if(invans==null)
			{
				invans = new HashSet<>();
				invocs.put(pojo, invans);
			}
			invans.add(SReflect.getUnqualifiedClassName(ann));
			return false;
		}
	}*/	
}
