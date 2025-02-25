package jadex.requiredservice.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import jadex.bytecode.ProxyFactory;
import jadex.common.ClassInfo;
import jadex.common.MethodInfo;
import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.common.UnparsedExpression;
import jadex.core.ComponentTerminatedException;
import jadex.core.impl.Component;
import jadex.execution.IExecutionFeature;
import jadex.execution.future.ComponentFutureFunctionality;
import jadex.execution.future.FutureFunctionality;
import jadex.execution.impl.ILifecycle;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.ITerminableFuture;
import jadex.future.ITerminableIntermediateFuture;
import jadex.future.IntermediateEmptyResultListener;
import jadex.future.IntermediateFuture;
import jadex.future.SubscriptionIntermediateFuture;
import jadex.future.TerminableFuture;
import jadex.future.TerminableIntermediateFuture;
import jadex.future.TerminationCommand;
import jadex.javaparser.SJavaParser;
import jadex.model.IModelFeature;
import jadex.model.modelinfo.ModelInfo;
import jadex.providedservice.IService;
import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.ServiceScope;
import jadex.providedservice.impl.search.IServiceRegistry;
import jadex.providedservice.impl.search.MultiplicityException;
import jadex.providedservice.impl.search.ServiceNotFoundException;
import jadex.providedservice.impl.search.ServiceQuery;
import jadex.providedservice.impl.search.ServiceQuery.Multiplicity;
import jadex.providedservice.impl.search.ServiceRegistry;
import jadex.providedservice.impl.service.AbstractServiceInvocationHandler;
import jadex.providedservice.impl.service.BasicService;
import jadex.providedservice.impl.service.IInternalService;
import jadex.providedservice.impl.service.IServiceInvocationInterceptor;
import jadex.providedservice.impl.service.ServiceIdentifier;
import jadex.providedservice.impl.service.ServiceInvocationHandler;
import jadex.providedservice.impl.service.interceptors.DecouplingInterceptor;
import jadex.providedservice.impl.service.interceptors.DecouplingReturnInterceptor;
import jadex.providedservice.impl.service.interceptors.MethodInvocationInterceptor;
import jadex.requiredservice.IRequiredServiceFeature;
import jadex.requiredservice.RequiredServiceBinding;
import jadex.requiredservice.RequiredServiceInfo;
import jadex.requiredservice.ServiceCallEvent;

public class RequiredServiceFeature	implements ILifecycle, IRequiredServiceFeature//, IParameterGuesser
{
	/** Marker for duplicate declarations of same type. */
	private static final RequiredServiceInfo DUPLICATE_SERVICE_TYPE_MARKER = new RequiredServiceInfo();
	
	protected Component self;
	
	/** The required service infos. */
	protected Map<String, RequiredServiceInfo> requiredserviceinfos;
	
	//-------- monitoring attributes --------
	
	/** The current subscriptions. */
	protected Set<SubscriptionIntermediateFuture<ServiceCallEvent>> subscriptions;
	
	//protected ISearchQueryManagerService sqms;
	
	//protected List<Tuple2<ServiceQuery<?>, SubscriptionIntermediateDelegationFuture<?>>> delayedremotequeries;

	
	protected RequiredServiceFeature(Component self)
	{
		this.self	= self;
	}
	
	public RequiredServiceModel loadModel()
	{
		return null;
	}
	
	@Override
	public void	onStart()
	{
		ModelInfo model = self.hasFeature(IModelFeature.class)? (ModelInfo)self.getFeature(IModelFeature.class).getModel(): null;
		RequiredServiceModel mymodel = model!=null? (RequiredServiceModel)model.getFeatureModel(IRequiredServiceFeature.class): null;
		if(mymodel==null)
			mymodel = loadModel();
		
		if(mymodel!=null)
		{
			// Required services. (Todo: prefix for capabilities)
			Map<String, RequiredServiceInfo> rservices = mymodel.getRequiredServices();
			
			//String config = model.getConfiguration();
			
			Map<String, RequiredServiceInfo> sermap = new LinkedHashMap<String, RequiredServiceInfo>(rservices!=null? rservices: Collections.EMPTY_MAP);
			/*for(int i=0; i<ms.length; i++)
			{
				ms[i] = new RequiredServiceInfo(/*getServicePrefix()+* /ms[i].getName(), ms[i].getType().getType(cl, model.getAllImports()), ms[i].getMin(), ms[i].getMax(), 
					ms[i].getDefaultBinding(), 
					//ms[i].getNFRProperties(), 
					ms[i].getTags());
				sermap.put(ms[i].getName(), ms[i]);
			}*/
	
			/*if(config!=null && model.getConfiguration(config)!=null)
			{
				ConfigurationInfo cinfo = model.getConfiguration(config);
				RequiredServiceInfo[] cs = cinfo.getServices();
				for(int i=0; i<cs.length; i++)
				{
					RequiredServiceInfo rsi = sermap.get(/*getServicePrefix()+* /cs[i].getName());
					RequiredServiceInfo newrsi = new RequiredServiceInfo(rsi.getName(), rsi.getType().getType(cl, model.getAllImports()), ms[i].getMin(), ms[i].getMax(), 
						new RequiredServiceBinding(cs[i].getDefaultBinding()), ms[i].getNFRProperties(), ms[i].getTags());
					sermap.put(rsi.getName(), newrsi);
				}
			}*/
			
			// Todo: Bindings from outside
			/*RequiredServiceBinding[] bindings = cinfo.getRequiredServiceBindings();
			if(bindings!=null)
			{
				for(int i=0; i<bindings.length; i++)
				{
					RequiredServiceInfo rsi = sermap.get(bindings[i].getName());
					RequiredServiceInfo newrsi = new RequiredServiceInfo(rsi.getName(), rsi.getType().getType(cl, model.getAllImports()), ms[i].getMin(), ms[i].getMax(), 
						new RequiredServiceBinding(bindings[i]), ms[i].getNFRProperties(), ms[i].getTags());
					sermap.put(rsi.getName(), newrsi);
				}
			}
			
			RequiredServiceInfo[] rservices = sermap.values().toArray(new RequiredServiceInfo[sermap.size()]);*/
			
			addRequiredServiceInfos(sermap.values().toArray(new RequiredServiceInfo[sermap.size()]));
			
			/*sqms = getLocalService(new ServiceQuery<>(query).setMultiplicity(0));
			if(sqms == null)
			{
				delayedremotequeries = new ArrayList<>();
				
				ISubscriptionIntermediateFuture<ISearchQueryManagerService> sqmsfut = addQuery(query);
				sqmsfut.addResultListener(new IntermediateEmptyResultListener<ISearchQueryManagerService>()
				{
					public void intermediateResultAvailable(ISearchQueryManagerService result)
					{
						//System.out.println("ISearchQueryManagerService "+result);
						if(sqms == null)
						{
							sqms = result;
							sqmsfut.terminate();
							for (Tuple2<ServiceQuery<?>, SubscriptionIntermediateDelegationFuture<?>> sqi : delayedremotequeries)
							{
								@SuppressWarnings({"unchecked"})
								ISubscriptionIntermediateFuture<Object> source = (ISubscriptionIntermediateFuture<Object>)addQuery(sqi.getFirstEntity());
								@SuppressWarnings("unchecked")
								SubscriptionIntermediateDelegationFuture<Object>	target	= (SubscriptionIntermediateDelegationFuture<Object>)sqi.getSecondEntity();
								
								source.delegateTo(target);
							}
							delayedremotequeries = null;
						}
					}
				});
			}*/
			/*else
			{
				System.out.println("directly found ISearchQueryManagerService");
			}*/
			
			
			/*final RequiredServiceModel fmymodel = mymodel;
			String[] sernames = mymodel.getServiceInjectionNames();
			
			Stream<Tuple2<String, ServiceInjectionInfo[]>> s = Arrays.stream(sernames).map(sername -> new Tuple2<String, ServiceInjectionInfo[]>(sername, fmymodel.getServiceInjections(sername)));
			
			Map<String, ServiceInjectionInfo[]> serinfos = s.collect(Collectors.toMap(t -> t.getFirstEntity(), t -> t.getSecondEntity())); 
			
			Object pojo = ((MicroAgent)self).getPojo(); // hack
			
			injectServices(getComponent(), pojo, sernames, serinfos, mymodel)
				.then(q ->
			{
				ret.setResult(null);
			}).catchEx(ret);*/
		}
	}
	
	@Override
	public void	onEnd()
	{
	}
	
	/**
	 *  Resolve a declared required service of a given name.
	 *  Asynchronous method for locally as well as remotely available services.
	 *  @param name The service name.
	 *  @return The service.
	 */
	public <T> IFuture<T> getService(String name)
	{
		return resolveService(getServiceQuery(getServiceInfo(name)), getServiceInfo(name));
	}
	
	/**
	 *  Resolve a required service of a given type.
	 *  Asynchronous method for locally as well as remotely available services.
	 *  @param type The service type.
	 *  @return The service.
	 */
	public <T> IFuture<T> getService(Class<T> type)
	{
		RequiredServiceInfo info = getServiceInfo(type);
		if(info==null)
		{
			// Convenience case: switch to search when type not declared
			return searchService(new ServiceQuery<>(type));
		}
		else
		{
			return resolveService(getServiceQuery(info), info);
			
		}
	}
	
	/**
	 *  Resolve a required services of a given name.
	 *  Asynchronous method for locally as well as remotely available services.
	 *  @param name The services name.
	 *  @return Each service as an intermediate result and a collection of services as final result.
	 */
	public <T> ITerminableIntermediateFuture<T> getServices(String name)
	{
		return resolveServices(getServiceQuery(getServiceInfo(name)), getServiceInfo(name));
	}
	
	/**
	 *  Resolve a required services of a given type.
	 *  Asynchronous method for locally as well as remotely available services.
	 *  @param type The services type.
	 *  @return Each service as an intermediate result and a collection of services as final result.
	 */
	public <T> ITerminableIntermediateFuture<T> getServices(Class<T> type)
	{
		RequiredServiceInfo info = getServiceInfo(type);
		if(info==null)
		{
			// Convenience case: switch to search when type not declared
			return searchServices(new ServiceQuery<>(type));
		}
		else
		{
			return resolveServices(getServiceQuery(info), info);
			
		}
	}
	
	/**
	 *  Resolve a declared required service of a given name.
	 *  Synchronous method only for locally available services.
	 *  @param name The service name.
	 *  @return The service.
	 */
	public <T> T getLocalService(String name)
	{
		return resolveLocalService(getServiceQuery(getServiceInfo(name)), getServiceInfo(name));
	}
	
	/**
	 *  Resolve a required service of a given type.
	 *  Synchronous method only for locally available services.
	 *  @param type The service type.
	 *  @return The service.
	 */
	public <T> T getLocalService(Class<T> type)
	{
		RequiredServiceInfo info = getServiceInfo(type);
		if(info==null)
		{
			// Convenience case: switch to search when type not declared
			return getLocalService(new ServiceQuery<>(type));
		}
		else
		{
			return resolveLocalService(getServiceQuery(info), info);
			
		}
	}
	
	/**
	 *  Resolve a required service of a given type.
	 *  Synchronous method only for locally available services.
	 *  @param type The service type.
	 *  @return The service.
	 */
	public <T> T getLocalService0(Class<T> type)
	{
		RequiredServiceInfo info = getServiceInfo(type);
		if(info==null)
		{
			// Convenience case: switch to search when type not declared
			return getLocalService(new ServiceQuery<>(type).setMultiplicity(Multiplicity.ZERO_ONE));
		}
		else
		{
			@SuppressWarnings("unchecked")
			ServiceQuery<T> sq = (ServiceQuery<T>)getServiceQuery(info).setMultiplicity(Multiplicity.ZERO_ONE);
			return resolveLocalService(sq, info);
			
		}
	}
	
	/**
	 *  Resolve a required services of a given name.
	 *  Synchronous method only for locally available services.
	 *  @param name The services name.
	 *  @return Each service as an intermediate result and a collection of services as final result.
	 */
	public <T> Collection<T> getLocalServices(String name)
	{
		return resolveLocalServices(getServiceQuery(getServiceInfo(name)), getServiceInfo(name)); 	
	}
	
	/**
	 *  Resolve a required services of a given type.
	 *  Synchronous method only for locally available services.
	 *  @param type The services type.
	 *  @return Each service as an intermediate result and a collection of services as final result.
	 */
	public <T> Collection<T> getLocalServices(Class<T> type)
	{
		RequiredServiceInfo info = getServiceInfo(type);
		if(info==null)
		{
			// Convenience case: switch to search when type not declared
			return getLocalServices(new ServiceQuery<>(type));
		}
		else
		{
			return resolveLocalServices(getServiceQuery(info), info);
			
		}
	}
	
	//-------- methods for searching --------
	
	/**
	 *  Search for matching services and provide first result.
	 *  @param query The search query.
	 *  @return Future providing the corresponding service or ServiceNotFoundException when not found.
	 */
	public <T> IFuture<T> searchService(ServiceQuery<T> query)
	{
		return resolveService(query, createServiceInfo(query));
	}
	
	/**
	 *  Search for matching services and provide first result.
	 *  Synchronous method only for locally available services.
	 *  @param query The search query.
	 *  @return Future providing the corresponding service or ServiceNotFoundException when not found.
	 */
	public <T> T getLocalService(ServiceQuery<T> query)
	{
		return resolveLocalService(query, createServiceInfo(query));
	}
	
	/**
	 *  Search for all matching services.
	 *  @param query The search query.
	 *  @return Future providing the corresponding services or ServiceNotFoundException when not found.
	 */
	public <T>  ITerminableIntermediateFuture<T> searchServices(ServiceQuery<T> query)
	{
		return resolveServices(query, createServiceInfo(query));
	}
	
	/**
	 *  Search for all matching services.
	 *  Synchronous method only for locally available services.
	 *  @param query The search query.
	 *  @return Future providing the corresponding services or ServiceNotFoundException when not found.
	 */
	public <T> Collection<T> getLocalServices(ServiceQuery<T> query)
	{
		return resolveLocalServices(query, createServiceInfo(query));
	}
	
	/**
	 *  Performs a sustained search for a service. Attempts to find a service
	 *  for a maximum duration until timeout occurs.
	 *  
	 *  @param query The search query.
	 *  @param timeout Maximum time period to search, -1 for no wait.
	 *  @return Service matching the query, exception if service is not found.
	 */
	public <T> IFuture<T> searchService(ServiceQuery<T> query, long timeout)
	{
		Future<T> ret = new Future<T>();
		//todo: hack!!!
		timeout = timeout != 0 ? timeout : 30000;// Starter.getDefaultTimeout(component.getId());
		
		ISubscriptionIntermediateFuture<T> queryfut = addQuery(query);
		
		queryfut.addResultListener(new IntermediateEmptyResultListener<T>()
		{
			public void exceptionOccurred(Exception exception)
			{
				ret.setExceptionIfUndone(exception);
			}

			public void intermediateResultAvailable(T result)
			{
				ret.setResultIfUndone(result);
				queryfut.terminate();
			}
		});
		
		long to = timeout;
		//isRemote(query)	// TODO: only realtime for remote queries?
		
		if(to>0)
		{
			// , Starter.isRealtimeTimeout(getComponent().getId(), true)
			getComponent().getFeature(IExecutionFeature.class).waitForDelay(timeout).then(done -> 
			{
				Multiplicity m = query.getMultiplicity();
				if(m.getFrom()>0)
				{
					queryfut.terminate(new ServiceNotFoundException("Service " + query + " not found in search period " + to));
				}
				else
				{
					queryfut.terminate();
				}
			});
		}
		
		return ret;
	}
	
	//-------- query methods --------

	/**
	 *  Add a query for a declared required service.
	 *  Continuously searches for matching services.
	 *  @param name The name of the required service declaration.
	 *  @return Future providing the corresponding services as intermediate results.
	 */
	public <T> ISubscriptionIntermediateFuture<T> addQuery(ServiceQuery<T> query, long timeout)
	{
		return addQuery(query);
		
		//SubscriptionIntermediateDelegationFuture<T> ret = new SubscriptionIntermediateDelegationFuture<>();
		
		//timeout = timeout != 0 ? timeout : Starter.getDefaultTimeout(component.getId());
		
		//ISubscriptionIntermediateFuture<T> queryfut = addQuery(query);
		
		//queryfut.delegateTo(ret);
		
		/*final int[] resultcnt = new int[1];
		queryfut.addResultListener(new IIntermediateResultListener<T>()
		{
			public void resultAvailable(Collection<T> result)
			{
				for(T r: result)
					intermediateResultAvailable(r);
				finished();
			}

			public void exceptionOccurred(Exception exception)
			{
				ret.setExceptionIfUndone(exception);
			}

			public void intermediateResultAvailable(T result)
			{
				resultcnt[0]++;
				ret.addIntermediateResultIfUndone(result);
			}

			public void finished()
			{
				ret.setFinishedIfUndone();
			}
			
			public void maxResultCountAvailable(int max) 
			{
				ret.setMaxResultCount(max);
			}
		});*/
		
		//long to = timeout;
		//isRemote(query)
		
		/*if(to>0)
		{
			component.waitForDelay(timeout, Starter.isRealtimeTimeout(component.getId(), true)).then(done -> 
			{
				Exception e;
				Multiplicity m = query.getMultiplicity();
				if(m.getFrom()>0 && resultcnt[0]<m.getFrom()
					|| m.getTo()>0 && resultcnt[0]>m.getTo())
				{
					e = new MultiplicityException("["+m.getFrom()+"-"+m.getTo()+"]"+", resultcnt="+resultcnt[0]);
				}
				else
				{
					e = new TimeoutException(""+to);
					//new ServiceNotFoundException("Service " + query + " not found in search period " + to)
				}
				queryfut.terminate(e);
			});
		}*/
		
		//return ret;
	}
	
	/**
	 *  Add a query for a declared required service.
	 *  Continuously searches for matching services.
	 *  @param name The name of the required service declaration.
	 *  @return Future providing the corresponding services as intermediate results.
	 */
	public <T> ISubscriptionIntermediateFuture<T> addQuery(String name)
	{
		return resolveQuery(getServiceQuery(getServiceInfo(name)), getServiceInfo(name));
	}

	/**
	 *  Add a query for a declared required service.
	 *  Continuously searches for matching services.
	 *  @param type The type of the required service declaration.
	 *  @return Future providing the corresponding services as intermediate results.
	 */
	public <T> ISubscriptionIntermediateFuture<T> addQuery(Class<T> type)
	{
		return resolveQuery(getServiceQuery(getServiceInfo(type)), getServiceInfo(type));
	}

	/**
	 *  Add a service query.
	 *  Continuously searches for matching services.
	 *  @param query The search query.
	 *  @return Future providing the corresponding service or ServiceNotFoundException when not found.
	 */
	public <T> ISubscriptionIntermediateFuture<T> addQuery(ServiceQuery<T> query)
	{
		return resolveQuery(query, createServiceInfo(query));
	}
	
	//-------- event interface --------
	
	/**
	 *  Get the required services.
	 *  @return The required services.
	 */
	public RequiredServiceInfo[] getServiceInfos()
	{
//		if(shutdowned)
//			throw new ComponentTerminatedException(id);
		
		// Convert to set to remove duplicate entries (name+type) and exclude marker.
		Set<RequiredServiceInfo>	ret	= new LinkedHashSet<>();
		if(requiredserviceinfos!=null)
		{
			for(RequiredServiceInfo info: requiredserviceinfos.values())
			{
				if(!DUPLICATE_SERVICE_TYPE_MARKER.equals(info))
				{
					ret.add(info);
				}
			}
		}
		return ret.toArray(new RequiredServiceInfo[ret.size()]);
	}
	
	/**
	 *  Listen to service call events (call, result and commands).
	 */
	// Todo: only match specific calls?
	// Todo: Commands
	public ISubscriptionIntermediateFuture<ServiceCallEvent> getServiceEvents()
	{
		if(subscriptions==null)
			subscriptions = new LinkedHashSet<SubscriptionIntermediateFuture<ServiceCallEvent>>();
		@SuppressWarnings("unchecked")
		final SubscriptionIntermediateFuture<ServiceCallEvent> ret = new SubscriptionIntermediateFuture<ServiceCallEvent>();
			//(SubscriptionIntermediateFuture<ServiceCallEvent>)SFuture.getNoTimeoutFuture(SubscriptionIntermediateFuture.class, getInternalAccess());
		ret.setTerminationCommand(new TerminationCommand()
		{
			@Override
			public void terminated(Exception reason)
			{
				subscriptions.remove(ret);
				if(subscriptions.isEmpty())
				{
					subscriptions = null;
				}
			}
		});
		subscriptions.add(ret);
		return ret;
	}
	
	/**
	 *  Post a service call event.
	 */
	public void postServiceEvent(ServiceCallEvent event)
	{
		if(subscriptions!=null)
		{
			for(SubscriptionIntermediateFuture<ServiceCallEvent> sub: subscriptions)
			{
				sub.addIntermediateResult(event);
			}
		} 
	}

	/**
	 *  Check if there is someone monitoring.
	 *  To avoid posting when nobody is listening.
	 */
	public boolean isMonitoring()
	{
		return subscriptions!=null;
	}
	
	//-------- convenience methods --------
	
	/**
	 *  Get a service raw (i.e. w/o required proxy).
	 *  @return null when not found.
	 */
	public <T> T getRawService(Class<T> type)
	{
		try
		{
			ServiceQuery<T> query = new ServiceQuery<>(type).setMultiplicity(Multiplicity.ZERO_ONE);
			query.setRequiredProxyType(ServiceQuery.PROXYTYPE_RAW);
			return resolveLocalService(query, createServiceInfo(query));
		}
		catch(ServiceNotFoundException snfe)
		{
			return null;
		}
	}

	/**
	 *  Get a service raw (i.e. w/o required proxy).
	 */
	public <T> Collection<T> getRawServices(Class<T> type)
	{
		ServiceQuery<T> query = new ServiceQuery<>(type);
		query.setRequiredProxyType(ServiceQuery.PROXYTYPE_RAW);
		return resolveLocalServices(query, createServiceInfo(query));
	}

	
	//-------- impl/raw methods --------
	
	/**
	 * 
	 * @param result
	 * @param info
	 * @return
	 */
	protected Object processResult(Object result, RequiredServiceInfo info)
	{
//		if(result instanceof ServiceEvent<?>)
//			return processServiceEvent((ServiceEvent<?>)result, info);
		/*else*/ if(result instanceof IServiceIdentifier)
			return getServiceProxy((IServiceIdentifier)result, info);
		else if(result instanceof IService)
			return addRequiredServiceProxy((IService)result, info, getComponent());
		else
			return result;
	}
	
	/**
	 *  Search for matching services and provide first result.
	 *  @param query The search query.
	 *  @param info Used for required service proxy configuration -> null for no proxy.
	 *  @return Future providing the corresponding service or ServiceNotFoundException when not found.
	 */
	public <T> ITerminableFuture<T> resolveService(ServiceQuery<T> query, RequiredServiceInfo info)
	{
		enhanceQuery(query, false);
		Future<T> ret = null;
		
		// Try to find locally
		IServiceIdentifier sid = ServiceRegistry.getRegistry().searchService(query);
		if(sid!=null)
		{
			ret = new TerminableFuture<>();
			@SuppressWarnings("unchecked")
			T t = (T)getServiceProxy(sid, info);
			ret.setResult(t);
		}
		
		// If not found -> try to find remotely
		/*else if(isRemote(query) && sqms != null)
		{
//			ISearchQueryManagerService sqms = getLocalService(new ServiceQuery<>(ISearchQueryManagerService.class).setMultiplicity(Multiplicity.ZERO_ONE));
//			if(sqms!=null)
//			{
			
			@SuppressWarnings("rawtypes")
			ITerminableFuture fut = sqms.searchService(query);
			@SuppressWarnings("unchecked")
			ITerminableFuture<T> castedfut = (ITerminableFuture<T>) fut;
			ret = FutureFunctionality.getDelegationFuture(castedfut, new FutureFunctionality(getComponent().getLogger())
			{
				@Override
				public Object handleResult(Object result) throws Exception
				{
					// todo: remove after superpeer fix
					result = processResult(result, info);
					if(result==null)
					{
						if(query.getMultiplicity().getFrom()!=0)
						{
							throw new ServiceNotFoundException(query);
						}
					}
					return result;
					
					//return processResult(result, info);
				}
			});
			
//			}
		}*/
		
		// Not found locally and query not remote or no remote search manager available
		if(ret==null)
		{
			ret = new TerminableFuture<>();
			if(query.getMultiplicity().getFrom()==0)
			{
				ret.setResult(null);
			}
			else
			{
				ret.setException(new ServiceNotFoundException(query));
			}
		}
		
		@SuppressWarnings("unchecked")
		ITerminableFuture<T> iret = (ITerminableFuture<T>)ret;
		return iret;
	}
	
	/**
	 *  Search for matching services and provide first result.
	 *  Synchronous method only for locally available services.
	 *  @param query The search query.
	 *  @param info Used for required service proxy configuration -> null for no proxy.
	 *  @return Future providing the corresponding service or ServiceNotFoundException when not found.
	 */
	public <T> T resolveLocalService(ServiceQuery<T> query, RequiredServiceInfo info)
	{
		enhanceQuery(query, false);
		
		IServiceIdentifier sid = ServiceRegistry.getRegistry().searchService(query);
		
		if(sid==null && query.getMultiplicity().getFrom()>0)
			throw new ServiceNotFoundException(query);
				
		// Fetches service and wraps result in proxy, if required. 
		@SuppressWarnings("unchecked")
		T ret = sid!=null ? (T)getServiceProxy(sid, info) : null;
		return ret;
	}
	
	/**
	 *  Search for all matching services.
	 *  @param query The search query.
	 *  @param info Used for required service proxy configuration -> null for no proxy.
	 *  @return Future providing the corresponding services or ServiceNotFoundException when not found.
	 */
	public <T>  ITerminableIntermediateFuture<T> resolveServices(ServiceQuery<T> query, RequiredServiceInfo info)
	{
		ITerminableIntermediateFuture<T> ret;
		
//		if(query.getServiceType().toString().indexOf("ITransportInfoService")!=-1)
//			System.out.println("here");
		
		// Check if remote
//		ISearchQueryManagerService sqms = isRemote(query) ? getLocalService(new ServiceQuery<>(ISearchQueryManagerService.class).setMultiplicity(Multiplicity.ZERO_ONE)) : null;
//		if(isRemote(query) && sqms==null)
//		{
//			getComponent().getLogger().warning("No ISearchQueryManagerService found for remote search: "+query);
////			return new TerminableIntermediateFuture<>(new IllegalStateException("No ISearchQueryManagerService found for remote search: "+query));
//		}
		
		//final int min = query.getMultiplicity()!=null? query.getMultiplicity().getFrom(): -1;
		final int max = query.getMultiplicity()!=null? query.getMultiplicity().getTo(): -1;
		final int[] resultcnt = new int[1];
		
		// Local only -> create future, fill results, and set to finished.
		if(!isRemote(query))// || sqms == null)
		{
			TerminableIntermediateFuture<T>	fut	= new TerminableIntermediateFuture<>();
			ret	= fut;
			
			// Find local matches (also enhances query, hack!?)
			Collection<T> locals = resolveLocalServices(query, info);
			for(T result: locals)
			{
				if(max<0 || ++resultcnt[0]<=max)
				{
					fut.addIntermediateResult(result);
					// if next result is not allowed any more
					if(max>0 && resultcnt[0]+1>max)
					{
						// Finish the user side and terminate the source side
						fut.setFinishedIfUndone();
						Exception reason = new MultiplicityException("Max number of values received: "+max);
						fut.terminate(reason);
					}
				}
				else
				{
					break;
				}
			}
			fut.setFinishedIfUndone();
		}
		else
		{
			ret = new TerminableIntermediateFuture(new UnsupportedOperationException());
		}

		// Find remote matches, if needed
		/*else
		{
			enhanceQuery(query, true);
			SlidingCuckooFilter scf = new SlidingCuckooFilter();
			
			// Search remotely and connect to delegation future.
			ITerminableIntermediateFuture<IServiceIdentifier> remotes = sqms.searchServices(query);

			@SuppressWarnings("unchecked")
			final IntermediateFuture<IServiceIdentifier>[] futs = new IntermediateFuture[1];
			
			// Combined delegation future for local and remote results.
			futs[0] = (IntermediateFuture<IServiceIdentifier>)FutureFunctionality
				.getDelegationFuture(ITerminableIntermediateFuture.class, new ComponentFutureFunctionality(getInternalAccess())
			{
				@Override
				public Object handleIntermediateResult(Object result) throws Exception
				{
					// Drop result when already in cuckoo filter
					if(scf.contains(result.toString()))
					{
						return DROP_INTERMEDIATE_RESULT;
					}
					else
					{
						if(max<0 || ++resultcnt[0]<=max)
						{
							scf.insert(result.toString());
							return processResult(result, info);
						}
						else
						{
							//System.out.println("fut drop: "+hashCode());
							return DROP_INTERMEDIATE_RESULT;
						}
					}
				}
				
				@Override
				public void handleAfterIntermediateResult(Object result) throws Exception
				{
					if(DROP_INTERMEDIATE_RESULT.equals(result))
						return;
					
					// if next result is not allowed any more
					if(max>0 && resultcnt[0]+1>max)
					{
						// Finish the user side and terminate the source side
						futs[0].setFinishedIfUndone();
						Exception reason = new MultiplicityException("Max number of values received: "+max);
						//System.out.println("fut terminate: "+hashCode());
						remotes.terminate(reason);
					}
				}
				
				@Override
				public void handleTerminated(Exception reason)
				{
					//System.out.println("fut terminated: "+hashCode());
					super.handleTerminated(reason);
				}
				
				@Override
				public void handleFinished(Collection<Object> results) throws Exception
				{
					//System.out.println("fut fin: "+hashCode());
					super.handleFinished(results);
				}
			});
			
			// Manually add local results to delegation future
			IServiceRegistry registry = ServiceRegistry.getRegistry(getInternalAccess());
			Collection<IServiceIdentifier> results =  registry.searchServices(query);
			for(IServiceIdentifier result: results)
			{
				if(max<0 || ++resultcnt[0]<=max)
				{
					futs[0].addIntermediateResult(result);
					// if next result is not allowed any more
					if(max>0 && resultcnt[0]+1>max)
					{
						// Finish the user side and terminate the source side
						futs[0].setFinishedIfUndone();
						Exception reason = new MultiplicityException("Max number of values received: "+max);
						((ITerminableIntermediateFuture<IServiceIdentifier>)futs[0]).terminate(reason);
					}
				}
				else
				{
					break;
				}
			}

//			System.out.println("Search: "+query);
//			remotes.addResultListener(res -> System.out.println("Search finished: "+query));
			remotes.delegateTo(futs[0]);
			
			@SuppressWarnings("unchecked")
			IIntermediateFuture<T>	casted	= (IIntermediateFuture<T>)futs[0];
			ret	= (ITerminableIntermediateFuture<T>)casted;
//			ret.addResultListener(res -> System.out.println("ret finished: "+query));
		}
		
		// print outs for debugging
		/*ret.addResultListener(new IIntermediateResultListener<T>()
		{
			@Override
			public void intermediateResultAvailable(T result)
			{
			}
			
			@Override
			public void exceptionOccurred(Exception exception)
			{
				System.out.println("ex: "+exception+" "+hashCode());
			}
			
			@Override
			public void finished()
			{
				System.out.println("fini: "+hashCode());
			}
			
			@Override
			public void resultAvailable(Collection<T> result)
			{
				System.out.println("resa: "+hashCode());
			}
		});*/
		
		return ret;
	}
	
	/**
	 *  Search for all matching services.
	 *  Synchronous method only for locally available services.
	 *  @param query The search query.
	 *  @param info Used for required service proxy configuration -> null for no proxy.
	 *  @return Future providing the corresponding services or ServiceNotFoundException when not found.
	 */
	public <T> Collection<T> resolveLocalServices(ServiceQuery<T> query, RequiredServiceInfo info)
	{
		enhanceQuery(query, true);
		
		IServiceRegistry registry = ServiceRegistry.getRegistry();
		Collection<IServiceIdentifier> results =  registry.searchServices(query);
		
		// Wraps result in proxy, if required. 
		Collection<T> ret = new ArrayList<>();
		for(IServiceIdentifier result: results)
		{
			@SuppressWarnings("unchecked")
			T service = (T)getServiceProxy(result, info);
			ret.add(service);
		}
		
		return ret;
	}
	
	/**
	 *  Query for all matching services.
	 *  @param query The search query.
	 *  @param info Used for required service proxy configuration -> null for no proxy.
	 *  @return Future providing the corresponding services or ServiceNotFoundException when not found.
	 */
	public <T>  ISubscriptionIntermediateFuture<T> resolveQuery(ServiceQuery<T> query, RequiredServiceInfo info)
	{
		enhanceQuery(query, true);
		SlidingCuckooFilter scf = new SlidingCuckooFilter();
		
		//System.out.println("query: "+query);
		
		// Query remote
//		ISearchQueryManagerService sqms = getLocalService(new ServiceQuery<>(ISearchQueryManagerService.class).setMultiplicity(Multiplicity.ZERO_ONE));
//		if(isRemote(query) && sqms==null)
//		{
//			return new SubscriptionIntermediateFuture<>(new IllegalStateException("No ISearchQueryManagerService found for remote query: "+query));
//		}
		ISubscriptionIntermediateFuture<T> tmpremotes = null;
		
		/*if(isRemote(query))
		{
			//System.out.println("remote query: "+query+" "+sqms);
			if(sqms != null)
			{
				tmpremotes = sqms.addQuery(query);
			}
			else
			{
				tmpremotes = new SubscriptionIntermediateDelegationFuture<>();
				Tuple2<ServiceQuery<?>, SubscriptionIntermediateDelegationFuture<?>> sqi = new Tuple2<ServiceQuery<?>, SubscriptionIntermediateDelegationFuture<?>>(query, (SubscriptionIntermediateDelegationFuture<T>) tmpremotes);
				delayedremotequeries.add(sqi);
				return tmpremotes;
			}
		}*/
		ISubscriptionIntermediateFuture<T> remotes = tmpremotes;
		
		// Query local registry
		IServiceRegistry registry = ServiceRegistry.getRegistry();
		ISubscriptionIntermediateFuture<?> localresults = (ISubscriptionIntermediateFuture<?>)registry.addQuery(query);
		
		final int[] resultcnt = new int[1];
		final ISubscriptionIntermediateFuture<T>[] ret = new ISubscriptionIntermediateFuture[1];
		ret[0] = (ISubscriptionIntermediateFuture)FutureFunctionality
			// Component functionality as local registry pushes results on arbitrary thread.
			.getDelegationFuture(localresults, new ComponentFutureFunctionality(getComponent())
		{
			@Override
			public Object handleIntermediateResult(Object result) throws Exception
			{
				//System.out.println("local: "+result);
				
				// Drop result when already in cuckoo filter
				if(scf.contains(result.toString()))
				{
					return DROP_INTERMEDIATE_RESULT;
				}
				else
				{
					// check multiplicity constraints
					resultcnt[0]++;
					int max = query.getMultiplicity().getTo();
					
					if(max<0 || resultcnt[0]<=max)
					{
						scf.insert(result.toString());
						return processResult(result, info);
					}
					else
					{
						return DROP_INTERMEDIATE_RESULT;
					}
				}
			}
			
			@Override
			public void handleAfterIntermediateResult(Object result) throws Exception
			{
				if(DROP_INTERMEDIATE_RESULT.equals(result))
					return;
				
				int max = query.getMultiplicity().getTo();
				// if next result is not allowed any more
				if(max>0 && resultcnt[0]+1>max)
				{
					((IntermediateFuture)ret[0]).setFinishedIfUndone();
					Exception reason = new MultiplicityException("Max number of values received: "+max);
					if(remotes!=null)
						remotes.terminate(reason);
					localresults.terminate(reason);
				}
			}
			
			@Override
			public void handleTerminated(Exception reason)
			{
				//System.out.println("terminated called: "+reason);
				
				// TODO: multi delegation future with multiple sources but one target?
				if(remotes!=null)
					remotes.terminate(reason);
				
				super.handleTerminated(reason);
			}
		});
		
		// print outs for debugging
		/*ret.addResultListener(new IIntermediateResultListener<T>()
		{
			@Override
			public void intermediateResultAvailable(T result)
			{
			}
			
			@Override
			public void exceptionOccurred(Exception exception)
			{
				System.out.println("ex: "+exception+" "+hashCode());
			}
			
			@Override
			public void finished()
			{
				System.out.println("fini: "+hashCode());
			}
			
			@Override
			public void resultAvailable(Collection<T> result)
			{
			}
		});*/
		
		// Add remote results to future (functionality handles wrapping)
		if(remotes!=null)
		{
			remotes.next(
				result->
				{
					@SuppressWarnings("unchecked")
					IntermediateFuture<T> fut = (IntermediateFuture<T>)ret[0];
					fut.addIntermediateResultIfUndone(result);
				})
			.catchEx(exception -> {}); // Ignore exception (printed when no listener supplied)
		}
		
		return ret[0];
	}
	
	//-------- helper methods --------
	
	/**
	 * When searching for declared service -> map required service declaration to service query.
	 */
	public <T> ServiceQuery<T> getServiceQuery(RequiredServiceInfo info)
	{
		// Evaluate and replace scope expression, if any.
		ServiceScope scope = info.getDefaultBinding()!=null ? info.getDefaultBinding().getScope() : null;
		/*if(ServiceScope.EXPRESSION.equals(scope))
		{
			scope = (ServiceScope)SJavaParser.getParsedValue(info.getDefaultBinding().getScopeExpression(), 
				getComponent().getFeature(IModelFeature.class).getModel().getAllImports(), getComponent().getFeature(IModelFeature.class).getFetcher(), getComponent().getClassLoader());
			info = new RequiredServiceInfo(info.getName(), info.getType(), info.getMin(), info.getMax(),
				new RequiredServiceBinding(info.getDefaultBinding()).setScope(scope),
				//info.getNFRProperties(), 
				info.getTags());
		}*/
		return getServiceQuery(getComponent(), info);
	}
	
	
	
	/**
	 *  Get the required service info for a name.
	 *  @param name The required service name.
	 */
	// Hack!!! used by multi invoker?
	public RequiredServiceInfo getServiceInfo(String name)
	{
		RequiredServiceInfo info = requiredserviceinfos==null ? null : requiredserviceinfos.get(name);
		if(info==null)
			throw new IllegalArgumentException("No such required service: "+name);
		return info;
	}
	
	/**
	 *  Get the required service info for a type.
	 *  @param type The required service type.
	 */
	protected RequiredServiceInfo getServiceInfo(Class<?> type)
	{
		RequiredServiceInfo info = requiredserviceinfos==null ? null : requiredserviceinfos.get(SReflect.getClassName(type));
		if(info==DUPLICATE_SERVICE_TYPE_MARKER)
			throw new IllegalArgumentException("Multiple required service declarations found for type: "+type);
		return info;
	}
	
//	/**
//	 *  Create the user-facing object from the received search or query result.
//	 *  Result may be service object, service identifier (local or remote), or event.
//	 *  User object is either event or service (with or without required proxy).
//	 */
//	@SuppressWarnings({ "rawtypes", "unchecked" })
//	protected ServiceEvent processServiceEvent(ServiceEvent event, RequiredServiceInfo info)
//	{
//		if(event.getService() instanceof IService)
//		{
//			IService service = addRequiredServiceProxy((IService)event.getService(), info);
//			event.setService(service);
//		}
//		else if(event.getService() instanceof IServiceIdentifier
//			&& event.getType()!=ServiceEvent.SERVICE_REMOVED)
//		{
//			IService service = getServiceProxy((IServiceIdentifier)event.getService(), info);
//			// can null when service is not available any more
//			if(service!=null)
//				event.setService(service);
//		}
//		
//		return event;
//	}
	
	/**
	 *  Create the user-facing object from the received search or query result.
	 *  Result may be service object, service identifier (local or remote), or event.
	 *  User object is either event or service (with or without required proxy).
	 */
	public IService getServiceProxy(IServiceIdentifier sid, RequiredServiceInfo info)
	{
		return getServiceProxy(sid, info, getComponent());
	}
	
	/**
	 *  Create the user-facing object from the received search or query result.
	 *  Result may be service object, service identifier (local or remote), or event.
	 *  User object is either event or service (with or without required proxy).
	 */
	public static IService getServiceProxy(IServiceIdentifier sid, RequiredServiceInfo info, Component component)
	{
		IService ret = null;
		
		// If service identifier -> find/create service object or proxy
			
		// Local component -> fetch local service object.
		//if(sid.getProviderId().getRoot().equals(getComponent().getId().getRoot()))
		//{
			ret = ServiceRegistry.getRegistry().getLocalService(sid); 			
		//}
			
		// Must support the case that 
		if(ret==null)
		{
			ComponentTerminatedException ex = new ComponentTerminatedException(sid.getProviderId());
			
			ret = new IInternalService() 
			{
				@Override
				public IFuture<Object> invokeMethod(String methodname, ClassInfo[] argtypes, Object[] args, ClassInfo returntype) 
				{
					return new Future<Object>(ex);
				}
				
				@Override
				public IServiceIdentifier getServiceId() 
				{
					return sid;
				}
				
				@Override
				public IFuture<MethodInfo[]> getMethodInfos() 
				{
					return BasicService.getMethodInfos(sid, component.getClassLoader());
				}
				
				@Override
				public IFuture<Void> startService() 
				{
					return new Future<Void>(ex);
				}
				
				@Override
				public IFuture<Void> shutdownService() 
				{
					return new Future<Void>(ex);
				}
				
				@Override
				public void setServiceIdentifier(IServiceIdentifier sid) 
				{
				}
				
				@Override
				public IFuture<Void> setComponentAccess(Component access) 
				{
					return new Future<Void>(ex);
				}
			};
		}
		
		// Remote component -> create remote proxy
		/*else
		{
			ret = RemoteMethodInvocationHandler.createRemoteServiceProxy(getInternalAccess(), sid);
		}*/
		
		// else service event -> just return event, as desired by user (specified in query return type)
		
		if(ret!=null)
			ret = addRequiredServiceProxy(ret, info, component);
		
		return ret;
	}

	/**
	 * 
	 * @param service
	 * @param info
	 */
	protected static IService addRequiredServiceProxy(IService service, RequiredServiceInfo info, Component component)
	{
		IService ret = service;
		
		// Add required service proxy if specified.
		//if(info!=null)
		//{
			ret = createRequiredServiceProxy(component, 
				(IService)ret, null, 
				//info, 
				info==null? null: info.getDefaultBinding() 
				//Starter.isRealtimeTimeout(getComponent().getId(), true) 
				//true
				);
			
			// Check if no property provider has been created before and then create and init properties
			/*if(!getComponent().getFeature(INFPropertyComponentFeature.class).hasRequiredServicePropertyProvider(ret.getServiceId()))
			{
				INFMixedPropertyProvider nfpp = getComponent().getFeature(INFPropertyComponentFeature.class)
					.getRequiredServicePropertyProvider(((IService)ret).getServiceId());
				
				List<NFRPropertyInfo> nfprops = info.getNFRProperties();
				if(nfprops!=null && nfprops.size()>0)
				{
					for(NFRPropertyInfo nfprop: nfprops)
					{
						MethodInfo mi = nfprop.getMethodInfo();
						Class<?> clazz = nfprop.getClazz().getType(getComponent().getClassLoader(), getComponent().getModel().getAllImports());
						INFProperty<?, ?> nfp = AbstractNFProperty.createProperty(clazz, getInternalAccess(), (IService)ret, nfprop.getMethodInfo(), nfprop.getParameters());
						if(mi==null)
						{
							nfpp.addNFProperty(nfp);
						}
						else
						{
							nfpp.addMethodNFProperty(mi, nfp);
						}
					}
				}
			}*/
		//}
		
		return ret;
	}
	

	/**
	 *  Enhance a query before processing.
	 *  Does some necessary preprocessing and needs to be called at least once before processing the query.
	 *  @param query The query to be enhanced.
	 */
	protected <T> void enhanceQuery(ServiceQuery<T> query, boolean multi)
	{
//		if(shutdowned)
//			return new Future<T>(new ComponentTerminatedException(id));

		// Set owner if not set
		if(query.getOwner()==null)
			query.setOwner(self.getId());
		
		// Set scope if not set
		/*if(ServiceScope.DEFAULT.equals(query.getScope()))
		{
			// Default to application if service type not set or not system service
			query.setScope(query.getServiceType()!=null && ServiceIdentifier.isSystemService(query.getServiceType().getType(self.getClassLoader()))
				? ServiceScope.PLATFORM : ServiceScope.APPLICATION);
		}*/
		
		if(query.getMultiplicity()==null)
		{
			// Fix multiple flag according to single/multi method 
			query.setMultiplicity(multi ? Multiplicity.ZERO_MANY : Multiplicity.ONE);
		}
		
		//if(query.getMultiplicity()!=null && query.getMultiplicity().getTo()==0)
		//	throw new MultiplicityException();
		
		// Network names not set by user?
		if(Arrays.equals(query.getNetworkNames(), ServiceQuery.NETWORKS_NOT_SET))
		{
			// Local or unrestricted?
			if(!isRemote(query) || Boolean.TRUE.equals(query.isUnrestricted())
				|| query.getServiceType()!=null && ServiceIdentifier.isUnrestricted(self, query.getServiceType().getType(self.getClassLoader()))) 
			{
				// Unrestricted -> Don't check networks.
				query.setNetworkNames((String[])null);
			}
			else
			{
				System.out.println("todo: enhance sid with network names");
				// Not unrestricted -> only find services from my local networks
				//@SuppressWarnings("unchecked")
				//Set<String> nnames = (Set<String>)Starter.getPlatformValue(getComponent().getId(), Starter.DATA_NETWORKNAMESCACHE);
				//query.setNetworkNames(nnames!=null? nnames.toArray(new String[nnames.size()]): SUtil.EMPTY_STRING_ARRAY);
			}
		}
	}
	
	/**
	 *  Check if a query is potentially remote.
	 *  @return True, if scope is set to a remote scope (e.g. global or network).
	 */
	public boolean isRemote(ServiceQuery<?> query)
	{
		//return query.getSearchStart()!=null && query.getSearchStart().getRoot()!=getComponent().getId().getRoot() || 
		return !query.getScope().isLocal();
	}
	
	/**
	 *  Get a service query for a required service info (as defined in the agent under that name).
	 *  @param name The name.
	 *  @return The service query.
	 */
	public ServiceQuery<?> getServiceQuery(String name)
	{
		return getServiceQuery(getServiceInfo(name));
	}
	
	/**
	 *  Add required services for a given prefix.
	 *  @param prefix The name prefix to use.
	 *  @param required services The required services to set.
	 */
	protected void addRequiredServiceInfos(RequiredServiceInfo[] requiredservices)
	{
//		if(shutdowned)
//			throw new ComponentTerminatedException(id);

		if(requiredservices!=null && requiredservices.length>0)
		{
			if(this.requiredserviceinfos==null)
				this.requiredserviceinfos = new HashMap<String, RequiredServiceInfo>();
			for(int i=0; i<requiredservices.length; i++)
			{
				this.requiredserviceinfos.put(requiredservices[i].getName(), requiredservices[i]);
				if(requiredservices[i].getType()!=null)
				{
					if(requiredserviceinfos.containsKey(requiredservices[i].getType().getTypeName()))
					{
						this.requiredserviceinfos.put(requiredservices[i].getType().getTypeName(), DUPLICATE_SERVICE_TYPE_MARKER);
					}
					else
					{
						this.requiredserviceinfos.put(requiredservices[i].getType().getTypeName(), requiredservices[i]);
					}
				}
			}
		}
	}
	
	/**
	 *  When searching with query -> create required service info from service query.
	 */
	public static <T> RequiredServiceInfo createServiceInfo(ServiceQuery<T> query)
	{
		// TODO: multiplicity required here for info? should not be needed for proxy creation
		RequiredServiceBinding binding = new RequiredServiceBinding(SUtil.createUniqueId(), query.getScope());
		binding.setProxytype(query.getRequiredProxyType());
		Multiplicity m = query.getMultiplicity();
		return new RequiredServiceInfo(null, 
			query.getServiceType(), 
			m==null? RequiredServiceInfo.UNDEFINED: m.getFrom(), 
			m==null? RequiredServiceInfo.UNDEFINED: m.getTo(), 
			binding, 
			//null, 
			query.getServiceTags()==null ? null : Arrays.asList(query.getServiceTags()));
	}
	
	protected Component getComponent()
	{
		return self;
	}
	
	/**
	 *  Static method for creating a standard service proxy for a required service.
	 */
	public static IService createRequiredServiceProxy(Component ia, IService service, 
		IRequiredServiceFetcher fetcher, 
		//RequiredServiceInfo info, 
		RequiredServiceBinding binding 
		//boolean realtime
		)
	{
		if(isRequiredServiceProxy(service))
		{
			System.out.println("Already required service proxy: "+service);
			return service;
		}
		
//		if(service.getServiceIdentifier().getServiceType().getTypeName().indexOf("IServiceCallService")!=-1)
//			System.out.println("hijijij");

//		System.out.println("cRSP:"+service.getServiceIdentifier());
		IService ret = service;
		
		if(binding==null || !AbstractServiceInvocationHandler.PROXYTYPE_RAW.equals(binding.getProxytype()))
		{
	//		System.out.println("create: "+service.getServiceIdentifier().getServiceType());
			ServiceInvocationHandler handler = new ServiceInvocationHandler(ia, service, true); // ia.getDescription().getCause()
			handler.addFirstServiceInterceptor(new MethodInvocationInterceptor());
//			handler.addFirstServiceInterceptor(new AuthenticationInterceptor(ia, true));
			// Dropped for v4???
//			if(binding!=null && binding.isRecover())
//				handler.addFirstServiceInterceptor(new RecoveryInterceptor(ia.getExternalAccess(), info, binding, fetcher));
			if(binding==null || AbstractServiceInvocationHandler.PROXYTYPE_DECOUPLED.equals(binding.getProxytype())) // done on provided side
				handler.addFirstServiceInterceptor(new DecouplingReturnInterceptor());
			//handler.addFirstServiceInterceptor(new MethodCallListenerInterceptor(ia, service.getServiceId()));
//			handler.addFirstServiceInterceptor(new NFRequiredServicePropertyProviderInterceptor(ia, service.getId()));
			UnparsedExpression[] interceptors = binding!=null ? binding.getInterceptors() : null;
			if(interceptors!=null && interceptors.length>0)
			{
				for(int i=0; i<interceptors.length; i++)
				{
					IServiceInvocationInterceptor interceptor = (IServiceInvocationInterceptor)SJavaParser.evaluateExpression(
//						interceptors[i].getValue(), ea.getModel().getAllImports(), ia.getFetcher(), ea.getModel().getClassLoader());
						interceptors[i].getValue(), ia.getFeature(IModelFeature.class).getModel().getAllImports(), ia.getValueProvider().getFetcher(), ia.getClassLoader());
					handler.addServiceInterceptor(interceptor);
				}
			}
			// Decoupling interceptor on required chains ensures that wrong incoming calls e.g. from gui thread
			// are automatically pushed to the req component thread
			if(binding==null || AbstractServiceInvocationHandler.PROXYTYPE_DECOUPLED.equals(binding.getProxytype())) // done on provided side
				handler.addFirstServiceInterceptor(new DecouplingInterceptor(ia, false, true));
			
			// Collect service interfaces (if interfaces are not present they are omitted. 
			ClassLoader cl = ia.getClassLoader();
			IServiceIdentifier sid = service.getServiceId();
			Set<Class<?>> ifaces = new HashSet<>();
			Class<?> iface = sid.getServiceType().getType(cl);
			if(iface!=null)
				ifaces.add(iface);
			for(ClassInfo ci: sid.getServiceSuperTypes())
			{
				iface = ci.getType(cl);
				if(iface!=null)
					ifaces.add(iface);
			}
			
			ifaces.add(IService.class);
			
			ret = (IService)ProxyFactory.newProxyInstance(ia.getClassLoader(), ifaces.toArray(new Class<?>[ifaces.size()]), handler); 	
			
			// todo: think about orders of decouping interceptors
			// if we want the decoupling return interceptor to schedule back on an external caller actual order must be reversed
			// now it can only schedule back on the hosting component of the required proxy
		}
		
		return ret;
	}
	
	/**
	 *  Test if a service is a required service proxy.
	 *  @param service The service.
	 *  @return True, if is required service proxy.
	 */
	public static boolean isRequiredServiceProxy(Object service)
	{
		boolean ret = false;
		if(ProxyFactory.isProxyClass(service.getClass()))
		{
			Object tmp = ProxyFactory.getInvocationHandler(service);
			if(tmp instanceof AbstractServiceInvocationHandler)
			{
				AbstractServiceInvocationHandler handler = (AbstractServiceInvocationHandler)tmp;
				ret = handler.isRequired();
			}
		}
		return ret;
	}
	
	/**
	 * When searching for declared service -> map required service declaration to service query.
	 */
	public static <T> ServiceQuery<T> getServiceQuery(Component ia, RequiredServiceInfo info)
	{
		// TODO??? : no, but hardconstraints should be added, NFR props are not for search
//		info.getNFRProperties();

		// todo:
//		info.getDefaultBinding().getComponentName();
//		info.getDefaultBinding().getComponentType();
		
		ServiceQuery<T> ret = new ServiceQuery<T>(info.getType(), info.getDefaultBinding().getScope(), ia.getId());
		//ret.setMultiplicity(info.isMultiple() ? Multiplicity.ZERO_MANY : Multiplicity.ONE);
		
		Multiplicity m = new Multiplicity();
		if(info.getMin()!=RequiredServiceInfo.UNDEFINED)
			m.setFrom(info.getMin());
		if(info.getMax()!=RequiredServiceInfo.UNDEFINED)
			m.setTo(info.getMax());
		
		if(info.getTags()!=null && info.getTags().size()>0)
			ret.setServiceTags(info.getTags().toArray(new String[info.getTags().size()]), ia);
		
		return ret;
	}
	
	/**
	 *  Gets a proxy for a known service at a target component.
	 *  @return Service proxy.
	 * /
	public static <S> S getServiceProxy(IComponent component, final ComponentIdentifier providerid, final Class<S> servicetype)
	{				
		S ret = null;
		
		boolean local = component.getId().getGlobalProcessIdentifier().equals(providerid.getRoot());
		if(local)
		{
			ret = component.getFeature(IRequiredServiceFeature.class).getLocalService(new ServiceQuery<>( servicetype).setProvider(providerid));
		}
		else
		{
			try
			{
				final IServiceIdentifier sid = BasicService.createServiceIdentifier(providerid, new ClassInfo(servicetype), null, "NULL", null, ServiceScope.GLOBAL, null, true);

				Class<?>[] interfaces = new Class[]{servicetype, IService.class};
				ProxyInfo pi = new ProxyInfo(interfaces);
				pi.addMethodReplacement(new MethodInfo("equals", new Class[]{Object.class}), new IMethodReplacement()
				{
					public Object invoke(Object obj, Object[] args)
					{
						return Boolean.valueOf(args[0]!=null && ProxyFactory.isProxyClass(args[0].getClass())
							&& ProxyFactory.getInvocationHandler(obj).equals(ProxyFactory.getInvocationHandler(args[0])));
					}
				});
				pi.addMethodReplacement(new MethodInfo("hashCode", new Class[0]), new IMethodReplacement()
				{
					public Object invoke(Object obj, Object[] args)
					{
						return Integer.valueOf(ProxyFactory.getInvocationHandler(obj).hashCode());
					}
				});
				pi.addMethodReplacement(new MethodInfo("toString", new Class[0]), new IMethodReplacement()
				{
					public Object invoke(Object obj, Object[] args)
					{
						return "Fake proxy for service("+sid+")";
					}
				});
				pi.addMethodReplacement(new MethodInfo("getId", new Class[0]), new IMethodReplacement()
				{
					public Object invoke(Object obj, Object[] args)
					{
						return sid;
					}
				});
				Method getclass = SReflect.getMethod(Object.class, "getClass", new Class[0]);
				pi.addExcludedMethod(new MethodInfo(getclass));
				
				RemoteReference rr = new RemoteReference(providerid, sid);
				ProxyReference pr = new ProxyReference(pi, rr);
				InvocationHandler handler = new RemoteMethodInvocationHandler(component, pr);
				ret = (S)ProxyFactory.newProxyInstance(component.getClassLoader(), 
					interfaces, handler);
//				ret = (S)ProxyFactory.newProxyInstance(component.getClassLoader(), 
//					interfaces, new RemoteMethodInvocationHandler(component, pr));
			}
			catch(Exception e)
			{
				SUtil.rethrowAsUnchecked(e);
			}
		}
		
		return ret;
	}*/
	
}
