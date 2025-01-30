package jadex.requiredservice.impl;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jadex.bytecode.ProxyFactory;
import jadex.common.IParameterGuesser;
import jadex.common.SAccess;
import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.common.Tuple2;
import jadex.core.impl.Component;
import jadex.execution.IExecutionFeature;
import jadex.future.CounterResultListener;
import jadex.future.DelegationResultListener;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.IResultListener;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.IntermediateEmptyResultListener;
import jadex.javaparser.SJavaParser;
import jadex.micro.MicroAgent;
import jadex.model.IModelFeature;
import jadex.model.impl.AbstractModelLoader;
import jadex.model.modelinfo.ModelInfo;
import jadex.providedservice.IService;
import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.ServiceScope;
import jadex.providedservice.impl.search.ServiceEvent;
import jadex.providedservice.impl.search.ServiceNotFoundException;
import jadex.providedservice.impl.search.ServiceQuery;
import jadex.requiredservice.IRequiredServiceFeature;
import jadex.requiredservice.RequiredServiceInfo;

public class MicroRequiredServiceFeature extends RequiredServiceFeature
{
	protected MicroRequiredServiceFeature(Component self)
	{
		super(self);
	}
	
	@Override
	public void	onStart()
	{
		super.onStart();

		ModelInfo model = self.hasFeature(IModelFeature.class)? (ModelInfo)self.getFeature(IModelFeature.class).getModel(): null;
		RequiredServiceModel mymodel = model!=null? (RequiredServiceModel)model.getFeatureModel(IRequiredServiceFeature.class): null;

		if(mymodel!=null)
		{
			final RequiredServiceModel fmymodel = mymodel;
			String[] sernames = mymodel.getServiceInjectionNames();
			
			Stream<Tuple2<String, ServiceInjectionInfo[]>> s = Arrays.stream(sernames).map(sername -> new Tuple2<String, ServiceInjectionInfo[]>(sername, fmymodel.getServiceInjections(sername)));
			
			Map<String, ServiceInjectionInfo[]> serinfos = s.collect(Collectors.toMap(t -> t.getFirstEntity(), t -> t.getSecondEntity())); 
			
			Object pojo = ((MicroAgent)self).getPojo(); // hack
			
			injectServices(getComponent(), pojo, sernames, serinfos, mymodel).get();
		}
	}
	
	public RequiredServiceModel loadModel()
	{
		ModelInfo model = (ModelInfo)self.getFeature(IModelFeature.class).getModel();

		RequiredServiceModel mymodel = (RequiredServiceModel)model.getFeatureModel(IRequiredServiceFeature.class);
		if(mymodel==null)
		{
			mymodel = (RequiredServiceModel)MicroRequiredServiceLoader.readFeatureModel(((MicroAgent)self).getPojo().getClass(), this.getClass().getClassLoader());
			final RequiredServiceModel fmymodel = mymodel;
			AbstractModelLoader loader = AbstractModelLoader.getLoader((Class< ? extends Component>)self.getClass());
			loader.updateCachedModel(() ->
			{
				model.putFeatureModel(IRequiredServiceFeature.class, fmymodel);
			});
		}
		
		return mymodel;
	}
	
	/**
	 *  Inject the services and initialize queries.
	 */
	public static IFuture<Void> injectServices(Component component, Object target, String[] sernames, Map<String, ServiceInjectionInfo[]> serinfos, RequiredServiceModel rsm)
	{
		final Future<Void> ret = new Future<Void>();
		
		// Inject required services
		// Fetch all injection names - field and method injections
		//String[] sernames = model.getServiceInjectionNames();
		
		if(sernames.length>0)
		{
			CounterResultListener<Void> lis = new CounterResultListener<Void>(sernames.length, 
				new DelegationResultListener<Void>(ret));
	
			for(int i=0; i<sernames.length; i++)
			{
				final ServiceInjectionInfo[] infos = serinfos.get(sernames[i]); //model.getServiceInjections(sernames[i]);
				final CounterResultListener<Void> lis2 = new CounterResultListener<Void>(infos.length, lis);

				String sername = (String)SJavaParser.evaluateExpressionPotentially(sernames[i], 
					component.getFeature(IModelFeature.class).getModel().getAllImports(), 
					//component.getFeature(IModelFeature.class).getFetcher(), 
					component.getValueProvider().getFetcher(), 
					component.getClassLoader());
						
				//if(sername!=null && sername.indexOf("calc")!=-1)
				//	System.out.println("calc");
				
				for(int j=0; j<infos.length; j++)
				{
					// Uses required service info to search service
					
					RequiredServiceInfo	info = infos[j].getRequiredServiceInfo()!=null? infos[j].getRequiredServiceInfo(): rsm.getService(sername);
					
					ServiceQuery<Object> query = createServiceQuery(component, info);
											
					// if query
					if(infos[j].getQuery()!=null && infos[j].getQuery().booleanValue())
					{							
						//ServiceQuery<Object> query = new ServiceQuery<>((Class<Object>)info.getType().getType(component.getClassLoader()), info.getDefaultBinding().getScope());
						//query = info.getTags()==null || info.getTags().size()==0? query: query.setServiceTags(info.getTags().toArray(new String[info.getTags().size()]), component.getExternalAccess()); 
						
						// Set event mode to get also removed events
						query.setEventMode();
						
						long to = infos[j].getActive();
						ISubscriptionIntermediateFuture<Object> sfut = to>0?
							component.getFeature(IRequiredServiceFeature.class).addQuery(query, to):
							component.getFeature(IRequiredServiceFeature.class).addQuery(query);
						
						// Directly continue with init when service is not required
						if(infos[j].getRequired()==null || !infos[j].getRequired().booleanValue())
							lis2.resultAvailable(null);
						final int fj = j;
						
						// Invokes methods for each intermediate result
						sfut.addResultListener(new IntermediateEmptyResultListener<Object>()
						{
							boolean first = true;
							public void intermediateResultAvailable(final Object result)
							{
								//System.out.println("agent received service event: "+result);
								
								/*if(result==null)
								{
									System.out.println("received null as service: "+infos[fj]);
									return;
								}*/
								// todo: multiple parameters and using parameter annotations?!
								// todo: multiple parameters and wait until all are filled?!
																
								if(infos[fj].getMethodInfo()!=null)
								{
									Method m = SReflect.getAnyMethod(target.getClass(), infos[fj].getMethodInfo().getName(), infos[fj].getMethodInfo().getParameterTypes(component.getClassLoader()));
									
									invokeMethod(m, target, result, component);
								}
								else if(infos[fj].getFieldInfo()!=null)
								{
									final Field	f = infos[fj].getFieldInfo().getField(component.getClassLoader());
										
									setDirectFieldValue(f, target, result, component);
								}
								
								// Continue with agent init when first service is found 
								if(first)
								{
									first = false;
									if(infos[fj].getRequired()==null || infos[fj].getRequired().booleanValue())
										lis2.resultAvailable(null);
								}
							}
							
							public void resultAvailable(Collection<Object> result)
							{
								finished();
							}
							
							public void exceptionOccurred(Exception e)
							{
								// todo:
								
//									if(!(e instanceof ServiceNotFoundException)
//										|| m.getAnnotation(AgentServiceSearch.class).required())
//									{
//										component.getLogger().warning("Method injection failed: "+e);
//									}
//									else
								{
									// Call self with empty list as result.
									finished();
								}
							}
						});
					}
					// if is search
					else
					{
						if(infos[j].getFieldInfo()!=null)
						{
							final Field	f = infos[j].getFieldInfo().getField(component.getClassLoader());
							Class<?> ft = f.getDeclaringClass();
							boolean multiple = ft.isArray() || SReflect.isSupertype(Collection.class, ft) || info.getMax()>2;
							
							final IFuture<Object> sfut = callgetService(sername, info, component, multiple);

							
							// todo: what about multi case?
							// why not add values to a collection as they come?!
							// currently waits until the search has finished before injecting
							
							// Is annotation is at field and field is of type future directly set it
							if(SReflect.isSupertype(IFuture.class, f.getType()))
							{
								try
								{
									SAccess.setAccessible(f, true);
									f.set(target, sfut);
									lis2.resultAvailable(null);
								}
								catch(Exception e)
								{
									System.out.println("Field injection failed: "+e);
									lis2.exceptionOccurred(e);
								}	
							}
							else
							{
								// if future is already done 
								if(sfut.isDone() && sfut.getException() == null)
								{
									try
									{
										setDirectFieldValue(f, target, sfut.get(), component);
										lis2.resultAvailable(null);
									}
									catch(Exception e)
									{
										lis2.exceptionOccurred(e);
									}
								}
								else if(infos[j].getLazy()!=null && infos[j].getLazy().booleanValue() && !multiple)
								{
									//RequiredServiceInfo rsi = ((IInternalRequiredServicesFeature)component.getFeature(IRequiredServicesFeature.class)).getServiceInfo(sername);
									Class<?> clz = info.getType().getType(component.getClassLoader(), component.getFeature(IModelFeature.class).getModel().getAllImports());
									//ServiceQuery<Object> query = RequiredServicesComponentFeature.getServiceQuery(component, info);
									
									UnresolvedServiceInvocationHandler h = new UnresolvedServiceInvocationHandler(component, query);
									Object proxy = ProxyFactory.newProxyInstance(component.getClassLoader(), new Class[]{IService.class, clz}, h);
								
									try
									{
										SAccess.setAccessible(f, true);
										f.set(target, proxy);
										lis2.resultAvailable(null);
									}
									catch(Exception e)
									{
										System.out.println("Field injection failed: "+e);
										lis2.exceptionOccurred(e);
									}
								}
								else
								{
									// todo: remove!
									// todo: disallow multiple field injections!
									// This is problematic because search can defer the agent startup esp. when remote search

									// Wait for result and block init until available
									// Dangerous because agent blocks
									final int fj = j;
									sfut.addResultListener(new IResultListener<Object>()
									{
										public void resultAvailable(Object result)
										{
											try
											{
												setDirectFieldValue(f, target, result, component);
												lis2.resultAvailable(null);
											}
											catch(Exception e)
											{
												lis2.exceptionOccurred(e);
											}
										}
										
										public void exceptionOccurred(Exception e)
										{
											if(!(e instanceof ServiceNotFoundException)
												|| (infos[fj].getRequired()!=null && infos[fj].getRequired().booleanValue()))
											{
												System.out.println("Field injection failed: "+e);
												lis2.exceptionOccurred(e);
											}
											else
											{
												// Set empty list, set on exception 
												if(SReflect.isSupertype(f.getType(), List.class))
												{
													// Call self with empty list as result.
													resultAvailable(new ArrayList<Object>());
												}
												else if(SReflect.isSupertype(f.getType(), Set.class))
												{
													// Call self with empty list as result.
													resultAvailable(new HashSet<Object>());
												}
												else
												{
													// Don't set any value.
													lis2.resultAvailable(null);
												}
											}
										}
									});
								}
							}
						}
						else if(infos[j].getMethodInfo()!=null)
						{
							// injection of future as parameter not considered meanigful case
							
							// injection of lazy proxy not considered as meaningful case

							final Method m = SReflect.getAnyMethod(target.getClass(), infos[j].getMethodInfo().getName(), infos[j].getMethodInfo().getParameterTypes(component.getClassLoader()));

							boolean multiple = info.getMax()>2;

							final IFuture<Object> sfut = callgetService(sername, info, component, multiple);
							
							// if future is already done 
							if(sfut.isDone() && sfut.getException() == null)
							{
								try
								{
									invokeMethod(m, target, sfut.get(), component);
									lis2.resultAvailable(null);
								}
								catch(Exception e)
								{
									lis2.exceptionOccurred(e);
								}
							}
							else 
							{
								sfut.addResultListener(new IResultListener<Object>() 
								{
									@Override
									public void resultAvailable(Object result) 
									{
										try
										{
											invokeMethod(m, target, sfut.get(), component);
											lis2.resultAvailable(null);
										}
										catch(Exception e)
										{
											lis2.exceptionOccurred(e);
										}
									}
									
									@Override
									public void exceptionOccurred(Exception exception) 
									{
										lis2.exceptionOccurred(exception);
									}
								});
							}
						}
					}
				}
			}
		}
		else
		{
			ret.setResult(null);
		}
		
		return ret;
	}
	
	/**
	 * 
	 */
	protected static void setDirectFieldValue(Field f, Object target, Object result, Component component)
	{
		
		//boolean multiple = ft.isArray() || SReflect.isSupertype(Collection.class, ft) || info.getMax()>2;

		//System.out.println("setDirectFieldValue: "+result+" "+component.getId());
		
		ServiceEvent event = result instanceof ServiceEvent? (ServiceEvent)result: null;
		
		if(event!=null)
		{
			IServiceIdentifier sid = event.getService();
			IService result2 = getServiceProxy(sid, null, component);
			
			if(event.getType()==ServiceEvent.SERVICE_ADDED)
			{
				if(!addDirectFieldValue(f, target, result))
				{
					if(!addDirectFieldValue(f, target, result2))
					{
						System.out.println("could not add value: "+result);
						//throw new RuntimeException("Could not add/set service value: "+result);
					}
				}
			}
			else if(event.getType()==ServiceEvent.SERVICE_REMOVED)
			{
				if(!removeDirectFieldValue(f, target, result))
				{
					if(!removeDirectFieldValue(f, target, result2))
					{
						System.out.println("could not remove value: "+result);
						//throw new RuntimeException("Could not remove service value: "+result);
					}
				}
			}
		}
		else
		{
			// default is set value
			addDirectFieldValue(f, target, result);
		}
	}
	
	protected static boolean addDirectFieldValue(Field f, Object target, Object result)
	{
		boolean ret = false;
		Class<?> ft = f.getType();
		SAccess.setAccessible(f, true);
		
		try
		{
		if(SReflect.isSupertype(ft, result.getClass()))
		{
			try
			{
				f.set(target, result);
				ret = true;
			}
			catch(Throwable t)
			{
				throw SUtil.throwUnchecked(t);
			}
		}
		else if(ft.isArray())
		{
			// find next null value and insert new value there
			Class<?> ct = ft.getComponentType();
			if(SReflect.isSupertype(ct, result.getClass()))
			{
				try
				{
					Object ar = f.get(target);
				
					for(int i=0; i<Array.getLength(ar); i++)
					{
						if(Array.get(ar, i)==null)
						{
							try
							{
								f.set(target, result);
								ret = true;
								break;
							}
							catch(Exception e)
							{
								throw SUtil.throwUnchecked(e);
							}
						}
					}
				}
				catch(Exception e)
				{
					throw SUtil.throwUnchecked(e);
				}
			}
			/*else
			{
				throw new RuntimeException("cannot invoke method as result type does not fit field types: "+result+" "+f);
			}*/
		}
		else if(SReflect.isSupertype(List.class, ft))
		{
			try
			{
				Class<?> type = SReflect.getIterableComponentType(f.getGenericType());
				if(SReflect.isSupertype(type, result.getClass()))
				{
					List<Object> coll = (List<Object>)f.get(target);
					if(coll==null)
					{
						coll = new ArrayList<Object>();
						try
						{
							f.set(target, coll);
						}
						catch(Exception e)
						{
							throw SUtil.throwUnchecked(e);
						}
					}
					coll.add(result);
					ret = true;
				}
			}
			catch(Exception e)
			{
				//throw SUtil.throwUnchecked(e);
			}
		}
		else if(SReflect.isSupertype(Set.class, ft))
		{
			try
			{
				Class<?> type = SReflect.getIterableComponentType(f.getGenericType());
				if(SReflect.isSupertype(type, result.getClass()))
				{
					Set<Object> coll = (Set<Object>)f.get(target);
					if(coll==null)
					{
						coll = new HashSet<Object>();
						try
						{
							f.set(target, coll);
						}
						catch(Exception e)
						{
							throw SUtil.throwUnchecked(e);
						}
					}
					coll.add(result);
					ret = true;
				}
			}
			catch(Exception e)
			{
				throw SUtil.throwUnchecked(e);
			}
		}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		return ret;
	}
	
	/**
	 *  Call
	 *  @param sername
	 *  @param info
	 *  @return
	 */
	protected static IFuture<Object> callgetService(String sername, RequiredServiceInfo info, Component component, boolean multiple)
	{
		final IFuture<Object> sfut;
		
		// if info is available use it. in case of services it is not available in the agent (model)
		if(info!=null)
		{
			if(multiple)
			{
				IFuture	ifut = component.getFeature(IRequiredServiceFeature.class).searchServices(createServiceQuery(component, info));
				sfut = ifut;
			}
			else
			{
				IFuture	ifut = component.getFeature(IRequiredServiceFeature.class).searchService(createServiceQuery(component, info));
				sfut = ifut;
			}
		}
		else
		{
			if(multiple)
			{
				IFuture	ifut = component.getFeature(IRequiredServiceFeature.class).getServices(sername);
				sfut = ifut;
			}
			else
			{
				IFuture	ifut = component.getFeature(IRequiredServiceFeature.class).getService(sername);
				sfut = ifut;
			}
		}
		
		return sfut;
	}
	
	/**
	 * When searching for declared service -> map required service declaration to service query.
	 */
	public static <T> ServiceQuery<T> createServiceQuery(Component component, RequiredServiceInfo info)
	{
		// Evaluate and replace scope expression, if any.
		ServiceScope scope = info.getDefaultBinding()!=null ? info.getDefaultBinding().getScope() : null;
		/*if(ServiceScope.EXPRESSION.equals(scope))
		{
			scope = (ServiceScope)SJavaParser.getParsedValue(info.getDefaultBinding().getScopeExpression(), component.getFeature(IModelFeature.class).getModel().getAllImports(), component.getFeature(IModelFeature.class).getFetcher(), component.getClassLoader());
			info	= new RequiredServiceInfo(info.getName(), info.getType(), info.getMin(), info.getMax(),
				new RequiredServiceBinding(info.getDefaultBinding()).setScope(scope),
				//info.getNFRProperties(), 
				info.getTags());
		}*/
		return getServiceQuery(component, info);
	}
	
	/**
	 * 
	 * @param m
	 * @param target
	 * @param result
	 */
	protected static void invokeMethod(Method m, Object target, Object result, Component component)
	{
		Object[] args = new Object[m.getParameterCount()];
		
		IParameterGuesser guesser = null;
		if(component.getFeature(IModelFeature.class)!=null)
			guesser = component.getValueProvider().getParameterGuesser();
			//guesser = component.getFeature(IModelFeature.class).getParameterGuesser();
		
		boolean invoke = fillMethodParameter(m, args, result, guesser);
		
		if(!invoke && result instanceof ServiceEvent)
		{
			ServiceEvent event = (ServiceEvent)result;
			IServiceIdentifier sid = event.getService();
			result = getServiceProxy(sid, null, component);  
			if(event.getType()==ServiceEvent.SERVICE_ADDED)
			{
				invoke = fillMethodParameter(m, args, result, guesser);
			}
			else if(event.getType()==ServiceEvent.SERVICE_REMOVED)
			{
				// do not invoke @OnService with removed service?! Do we want @OnServiceRemoved or @OnService(type=removed)
			}
		}
		
		if(invoke)
		{
			// what to do with exception in user code?
			component.getFeature(IExecutionFeature.class).scheduleStep(() ->
			{
				try
				{
					SAccess.setAccessible(m, true);
					m.invoke(target, args);
				}
				catch(Exception t)
				{
					//throw SUtil.throwUnchecked(t);
					t.printStackTrace();
				}
			});
		}
		/*else
		{
			System.out.println("cannot invoke method as result type does not fit parameter types: "+result+" "+m);
		}*/
	}
	
	protected static boolean fillMethodParameter(Method m, Object[] args, Object result, IParameterGuesser guesser)
	{
		boolean ret = false;
		for(int i=0; i<m.getParameterCount(); i++)
		{
			if(SReflect.isSupertype(m.getParameterTypes()[i], result.getClass()))
			{
				args[i] = result;
				ret = true;
				break;
			}
			else
			{
				if(guesser!=null)
				{
					try
					{
						args[i] = guesser.guessParameter(m.getParameterTypes()[i], false);
					}
					catch(Exception e)
					{
					}
				}
			}
			/*else
			{
				System.out.println("arg does not fit: "+m.getParameterTypes()[i]+" "+result.getClass());
			}*/
		}
		return ret;
	}
	
	protected static boolean removeDirectFieldValue(Field f, Object target, Object result)
	{
		boolean ret = false;
		Class<?> ft = f.getType();
		SAccess.setAccessible(f, true);
		
		try
		{
		
		if(SReflect.isSupertype(ft, result.getClass()))
		{
			try
			{
				f.set(target, null);
				ret = true;
			}
			catch(Throwable t)
			{
				throw SUtil.throwUnchecked(t);
			}
		}
		else if(ft.isArray())
		{
			// find next null value and insert new value there
			Class<?> ct = ft.getComponentType();
			if(SReflect.isSupertype(ct, result.getClass()))
			{
				try
				{
					Object ar = f.get(target);
				
					for(int i=0; i<Array.getLength(ar); i++)
					{
						if(Array.get(ar, i)==result)
						{
							try
							{
								f.set(target, null);
								ret = true;
								break;
							}
							catch(Exception e)
							{
								throw SUtil.throwUnchecked(e);
							}
						}
					}
				}
				catch(Exception e)
				{
					throw SUtil.throwUnchecked(e);
				}
			}
			/*else
			{
				throw new RuntimeException("cannot invoke method as result type does not fit field types: "+result+" "+f);
			}*/
		}
		else if(SReflect.isSupertype(List.class, ft))
		{
			try
			{
				List<Object> coll = (List<Object>)f.get(target);
				if(coll!=null && coll.contains(result))
				{
					coll.remove(result);
					ret = true;
				}
			}
			catch(Exception e)
			{
				//throw SUtil.throwUnchecked(e);
			}
		}
		else if(SReflect.isSupertype(Set.class, ft))
		{
			try
			{
				Set<Object> coll = (Set<Object>)f.get(target);
				if(coll!=null && coll.contains(result))
				{
					coll.remove(result);
					ret = true;
					//System.out.println("removed: "+coll.size());
				}
			}
			catch(Exception e)
			{
				//throw SUtil.throwUnchecked(e);
			}
		}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		return ret;
	}
}