package jadex.mj.requiredservice.impl;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jadex.bytecode.ProxyFactory;
import jadex.common.ClassInfo;
import jadex.common.MethodInfo;
import jadex.common.SAccess;
import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.common.Tuple2;
import jadex.common.UnparsedExpression;
import jadex.future.CounterResultListener;
import jadex.future.DelegationResultListener;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.IResultListener;
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
import jadex.mj.core.impl.Component;
import jadex.mj.feature.execution.ComponentTerminatedException;
import jadex.mj.feature.execution.IExecutionFeature;
import jadex.mj.feature.execution.impl.ILifecycle;
import jadex.mj.feature.providedservice.IService;
import jadex.mj.feature.providedservice.IServiceIdentifier;
import jadex.mj.feature.providedservice.ServiceScope;
import jadex.mj.feature.providedservice.annotation.Service;
import jadex.mj.feature.providedservice.impl.search.IServiceRegistry;
import jadex.mj.feature.providedservice.impl.search.MultiplicityException;
import jadex.mj.feature.providedservice.impl.search.ServiceEvent;
import jadex.mj.feature.providedservice.impl.search.ServiceNotFoundException;
import jadex.mj.feature.providedservice.impl.search.ServiceQuery;
import jadex.mj.feature.providedservice.impl.search.ServiceQuery.Multiplicity;
import jadex.mj.feature.providedservice.impl.search.ServiceRegistry;
import jadex.mj.feature.providedservice.impl.service.impl.BasicService;
import jadex.mj.feature.providedservice.impl.service.impl.IInternalService;
import jadex.mj.feature.providedservice.impl.service.impl.IServiceInvocationInterceptor;
import jadex.mj.feature.providedservice.impl.service.impl.ServiceIdentifier;
import jadex.mj.feature.providedservice.impl.service.impl.ServiceInvocationHandler;
import jadex.mj.feature.providedservice.impl.service.impl.interceptors.DecouplingInterceptor;
import jadex.mj.feature.providedservice.impl.service.impl.interceptors.DecouplingReturnInterceptor;
import jadex.mj.feature.providedservice.impl.service.impl.interceptors.FutureFunctionality;
import jadex.mj.feature.providedservice.impl.service.impl.interceptors.MethodInvocationInterceptor;
import jadex.mj.micro.MicroClassReader;
import jadex.mj.micro.MicroAgent;
import jadex.mj.model.AbstractModelLoader;
import jadex.mj.model.IModelFeature;
import jadex.mj.model.modelinfo.ModelInfo;
import jadex.mj.requiredservice.IRequiredServiceFeature;
import jadex.mj.requiredservice.RequiredServiceBinding;
import jadex.mj.requiredservice.RequiredServiceInfo;
import jadex.mj.requiredservice.ServiceCallEvent;

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
	
	@Override
	public IFuture<Void> onStart()
	{
		Future<Void> ret = new Future<Void>();
		
		ModelInfo model = (ModelInfo)self.getFeature(IModelFeature.class).getModel();
		
		RequiredServiceModel mymodel = (RequiredServiceModel)model.getFeatureModel(IRequiredServiceFeature.class);
		if(mymodel==null)
		{
			mymodel = (RequiredServiceModel)RequiredServiceLoader.readFeatureModel(((MicroAgent)self).getPojo().getClass(), this.getClass().getClassLoader());
			final RequiredServiceModel fmymodel = mymodel;
			AbstractModelLoader loader = AbstractModelLoader.getLoader(self.getClass());
			loader.updateCachedModel(() ->
			{
				model.putFeatureModel(IRequiredServiceFeature.class, fmymodel);
			});
		}
		
		// Required services. (Todo: prefix for capabilities)
		Map<String, RequiredServiceInfo> rservices = mymodel.getRequiredServices();
		
		//String config = model.getConfiguration();
		
		Map<String, RequiredServiceInfo> sermap = new LinkedHashMap<String, RequiredServiceInfo>(rservices);
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
		
		
		final RequiredServiceModel fmymodel = mymodel;
		String[] sernames = mymodel.getServiceInjectionNames();
		
		Stream<Tuple2<String, ServiceInjectionInfo[]>> s = Arrays.stream(sernames).map(sername -> new Tuple2<String, ServiceInjectionInfo[]>(sername, fmymodel.getServiceInjections(sername)));
		
		Map<String, ServiceInjectionInfo[]> serinfos = s.collect(Collectors.toMap(t -> t.getFirstEntity(), t -> t.getSecondEntity())); 
		
		Object pojo = ((MicroAgent)self).getPojo(); // hack
		injectServices(getComponent(), pojo, sernames, serinfos, mymodel)
			.then(q ->
		{
			ret.setResult(null);
		}).catchEx(ret);
		
		return ret;
	}
	
	@Override
	public IFuture<Void> onEnd()
	{
		return IFuture.DONE;
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
		if(ServiceScope.EXPRESSION.equals(scope))
		{
			scope = (ServiceScope)SJavaParser.getParsedValue(info.getDefaultBinding().getScopeExpression(), 
				getComponent().getFeature(IModelFeature.class).getModel().getAllImports(), getComponent().getFeature(IModelFeature.class).getFetcher(), getComponent().getClassLoader());
			info = new RequiredServiceInfo(info.getName(), info.getType(), info.getMin(), info.getMax(),
				new RequiredServiceBinding(info.getDefaultBinding()).setScope(scope),
				//info.getNFRProperties(), 
				info.getTags());
		}
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
		if(ServiceScope.DEFAULT.equals(query.getScope()))
		{
			// Default to application if service type not set or not system service
			query.setScope(query.getServiceType()!=null && ServiceIdentifier.isSystemService(query.getServiceType().getType(self.getClassLoader()))
				? ServiceScope.PLATFORM : ServiceScope.APPLICATION);
		}
		
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
		
		if(binding==null || !ServiceInvocationHandler.PROXYTYPE_RAW.equals(binding.getProxytype()))
		{
	//		System.out.println("create: "+service.getServiceIdentifier().getServiceType());
			ServiceInvocationHandler handler = new ServiceInvocationHandler(ia, service, true); // ia.getDescription().getCause()
			handler.addFirstServiceInterceptor(new MethodInvocationInterceptor());
//			handler.addFirstServiceInterceptor(new AuthenticationInterceptor(ia, true));
			// Dropped for v4???
//			if(binding!=null && binding.isRecover())
//				handler.addFirstServiceInterceptor(new RecoveryInterceptor(ia.getExternalAccess(), info, binding, fetcher));
			if(binding==null || ServiceInvocationHandler.PROXYTYPE_DECOUPLED.equals(binding.getProxytype())) // done on provided side
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
						interceptors[i].getValue(), ia.getFeature(IModelFeature.class).getModel().getAllImports(), ia.getFeature(IModelFeature.class).getFetcher(), ia.getClassLoader());
					handler.addServiceInterceptor(interceptor);
				}
			}
			// Decoupling interceptor on required chains ensures that wrong incoming calls e.g. from gui thread
			// are automatically pushed to the req component thread
			if(binding==null || ServiceInvocationHandler.PROXYTYPE_DECOUPLED.equals(binding.getProxytype())) // done on provided side
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
			if(tmp instanceof ServiceInvocationHandler)
			{
				ServiceInvocationHandler handler = (ServiceInvocationHandler)tmp;
				ret = handler.isRequired();
			}
		}
		return ret;
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

				String sername = (String)SJavaParser.evaluateExpressionPotentially(sernames[i], component.getFeature(IModelFeature.class).getModel().getAllImports(), component.getFeature(IModelFeature.class).getFetcher(), component.getClassLoader());
						
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

	protected static boolean fillMethodParameter(Method m, Object[] args, Object result)
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
		}
		return ret;
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
		
		boolean invoke = fillMethodParameter(m, args, result);
		
		if(!invoke && result instanceof ServiceEvent)
		{
			ServiceEvent event = (ServiceEvent)result;
			IServiceIdentifier sid = event.getService();
			result = getServiceProxy(sid, null, component);  
			if(event.getType()==ServiceEvent.SERVICE_ADDED)
			{
				invoke = fillMethodParameter(m, args, result);
			}
			else if(event.getType()==ServiceEvent.SERVICE_REMOVED)
			{
				// do not invoke @OnService with removed service?! Do we want @OnServiceRemoved or @OnService(type=removed)
			}
		}
		
		if(invoke)
		{
			component.getFeature(IExecutionFeature.class).scheduleStep(() ->
			{
				try
				{
					SAccess.setAccessible(m, true);
					m.invoke(target, args);
				}
				catch(Throwable t)
				{
					throw SUtil.throwUnchecked(t);
				}
				return IFuture.DONE;
			});
		}
		else
		{
			System.out.println("cannot invoke method as result type does not fit parameter types: "+result+" "+m);
		}
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
		if(ServiceScope.EXPRESSION.equals(scope))
		{
			scope = (ServiceScope)SJavaParser.getParsedValue(info.getDefaultBinding().getScopeExpression(), component.getFeature(IModelFeature.class).getModel().getAllImports(), component.getFeature(IModelFeature.class).getFetcher(), component.getClassLoader());
			info	= new RequiredServiceInfo(info.getName(), info.getType(), info.getMin(), info.getMax(),
				new RequiredServiceBinding(info.getDefaultBinding()).setScope(scope),
				//info.getNFRProperties(), 
				info.getTags());
		}
		return getServiceQuery(component, info);
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
		
		if(info.getTags()!=null)
			ret.setServiceTags(info.getTags().toArray(new String[info.getTags().size()]));//, ia.getExternalAccess());
		
		return ret;
	}
	
	/**
	 * 
	 */
	public static Class<?> guessParameterType(Class<?>[] ptypes, ClassLoader cl)
	{
		Class<?> iftype = null;
		
		for(Class<?> ptype: ptypes)
		{
			if(MicroClassReader.isAnnotationPresent(ptype, Service.class, cl))
			{
				iftype = ptype;
				break;
			}
		}
		
		if(iftype==null || Object.class.equals(iftype))
			throw new RuntimeException("No service interface found for service query");
		
		return iftype;
	}
}
