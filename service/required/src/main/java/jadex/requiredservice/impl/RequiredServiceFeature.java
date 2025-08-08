package jadex.requiredservice.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.execution.future.ComponentFutureFunctionality;
import jadex.execution.future.FutureFunctionality;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.ITerminableFuture;
import jadex.future.ITerminableIntermediateFuture;
import jadex.future.IntermediateFuture;
import jadex.future.SubscriptionIntermediateFuture;
import jadex.future.TerminableIntermediateFuture;
import jadex.providedservice.IService;
import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.impl.search.IServiceRegistry;
import jadex.providedservice.impl.search.ServiceEvent;
import jadex.providedservice.impl.search.ServiceQuery;
import jadex.providedservice.impl.search.ServiceQuery.Multiplicity;
import jadex.providedservice.impl.search.ServiceRegistry;
import jadex.requiredservice.IRemoteServiceHandler;
import jadex.requiredservice.IRequiredServiceFeature;
import jadex.requiredservice.ServiceNotFoundException;

/**
 *  Component feature that handles injection and search/query services.
 */
public class RequiredServiceFeature implements IRequiredServiceFeature
{
	/** The component. */
	protected IComponent self;
	
	/**
	 *  Create the injection feature.
	 */
	public RequiredServiceFeature(IComponent self)
	{
		this.self	= self;
	}
		
	//-------- IRequired2Feature interface --------

	@Override
	public <T> T getLocalService(Class<T> type)
	{
		return getLocalService(new ServiceQuery<>(type));
	}
	
	@Override
	public <T> T getLocalService(ServiceQuery<T> query)
	{
		enhanceQuery(query, false);
		IServiceIdentifier	sid	= ServiceRegistry.getRegistry().searchService(query);
		if(sid==null)
		{
			throw new ServiceNotFoundException(query);
		}
		@SuppressWarnings("unchecked")
		T	ret	= (T)ServiceRegistry.getRegistry().getLocalService(sid);
		return ret;
	}

	@Override
	public <T> Collection<T> getLocalServices(Class<T> type)
	{
		return getLocalServices(new ServiceQuery<>(type));
	}
	
	@Override
	public <T> Collection<T> getLocalServices(ServiceQuery<T> query)
	{
		enhanceQuery(query, false);
		Collection<IServiceIdentifier>	sids	= ServiceRegistry.getRegistry().searchServices(query);
		List<T>	ret	= new ArrayList<>();
		for(IServiceIdentifier sid: sids)
		{
			@SuppressWarnings("unchecked")
			T	service	= (T)ServiceRegistry.getRegistry().getLocalService(sid);
			ret.add(service);
		}
		return ret;
	}

	@Override
	public <T> IFuture<T> searchService(Class<T> type)
	{
		return searchService(new ServiceQuery<>(type));
	}

	@Override
	public <T> IFuture<T> searchService(ServiceQuery<T> query)
	{
		//System.out.println("searchService: "+query);
		
		Future<T> ret = new Future<>();
		
		enhanceQuery(query, false);
		IServiceIdentifier sid = ServiceRegistry.getRegistry().searchService(query);
		if(sid==null)
		{
			if(getRemoteServiceHandler()!=null && isRemote(query))
			{
				//System.out.println("searchService remote: "+query);
				// Search remote service.
				ITerminableFuture<IServiceIdentifier> fut = getRemoteServiceHandler().searchService(query);
				fut.then(sid2 ->
				{
					if(sid2!=null)
					{
						//System.out.println("searchService Found remote service: "+sid2);
						@SuppressWarnings("unchecked")
						T service = (T)getRemoteServiceHandler().getRemoteServiceProxy(self, sid2).get();
						ret.setResult(service);
					}
					else
					{
						//System.out.println("searchService no service: "+sid2);
						ret.setException(new ServiceNotFoundException(query));
					}
				}).catchEx(ret);
			}
			else
			{
				//System.out.println("searchService no remote handler: "+query);
				ret.setException(new ServiceNotFoundException(query));
			}
		}
		else
		{
			//System.out.println("searchService found local service: "+query);
			@SuppressWarnings("unchecked")
			T service = (T)ServiceRegistry.getRegistry().getLocalService(sid);
			ret.setResult(service);
		}
		
		return ret;
	}

	@Override
	public <T> ITerminableIntermediateFuture<T> searchServices(Class<T> type)
	{
		return searchServices(new ServiceQuery<>(type));
	}
	
	@Override
	public <T> ITerminableIntermediateFuture<T> searchServices(ServiceQuery<T> query)
	{
		TerminableIntermediateFuture<T>	ret	= new TerminableIntermediateFuture<>();
		
		enhanceQuery(query, false);
		Collection<IServiceIdentifier> sids = ServiceRegistry.getRegistry().searchServices(query);
		for(IServiceIdentifier sid: sids)
		{
			@SuppressWarnings("unchecked")
			T service = (T)ServiceRegistry.getRegistry().getLocalService(sid);
			ret.addIntermediateResult(service);
		}
		
		if(getRemoteServiceHandler()!=null && isRemote(query))
		{
			// Search remote service.
			ITerminableIntermediateFuture<IServiceIdentifier> fut = getRemoteServiceHandler().searchServices(query);
			fut.next(sid2 ->
			{
				if(sid2!=null)
				{
					// Found remote service.
					@SuppressWarnings("unchecked")
					T service = (T)getRemoteServiceHandler().getRemoteServiceProxy(self, sid2).get();
					ret.addIntermediateResultIfUndone(service);
				}
			}).finished(x ->
			{
				ret.setFinishedIfUndone();
			})
			.catchEx(ex ->
			{
				if(!sids.isEmpty())
					ret.setFinishedIfUndone();
				else
					ret.setException(new ServiceNotFoundException(query));
			});
		}
		else
		{
			//if(!sids.isEmpty())
				ret.setFinishedIfUndone();
			//else
			//	ret.setException(new ServiceNotFoundException(query));
		}
		
		return ret;
	}

	@Override
	public <T> ISubscriptionIntermediateFuture<T> addQuery(Class<T> type)
	{
		return addQuery(new ServiceQuery<>(type));
	}
	
	@Override
	public <T> ISubscriptionIntermediateFuture<T> addQuery(ServiceQuery<T> query)
	{
		final ISubscriptionIntermediateFuture<T> ret[] = new ISubscriptionIntermediateFuture[1];
		
		enhanceQuery(query, true);
		
		SlidingCuckooFilter filter = query.isEventMode() ? null : new SlidingCuckooFilter();
		Set<IServiceIdentifier> results	= query.isEventMode() ? new LinkedHashSet<>() : null;
		
		// Query local registry
		IServiceRegistry registry = ServiceRegistry.getRegistry();
		ISubscriptionIntermediateFuture<Object> localresults = (ISubscriptionIntermediateFuture<Object>)registry.addQuery(query);
		
		@SuppressWarnings({"unchecked", "rawtypes"})
		ISubscriptionIntermediateFuture<T> tfut	= (ISubscriptionIntermediateFuture)FutureFunctionality
			// Component functionality as local registry pushes results on arbitrary thread.
			.getDelegationFuture(localresults, new ComponentFutureFunctionality(getComponent())
		{
			protected int resultcnt = 0;
			
			@Override
			public Object handleIntermediateResult(Object result) throws Exception
			{
				// New event result?
				Object res = null;
				
				if(query.isEventMode())
				{
					// Forward event if consistent with current results.
					ServiceEvent event = (ServiceEvent)result;
					if(event.getType()==ServiceEvent.SERVICE_ADDED && results.add(event.getService())
						|| event.getType()==ServiceEvent.SERVICE_CHANGED && results.contains(event.getService())
						|| event.getType()==ServiceEvent.SERVICE_REMOVED && results.remove(event.getService()))
					{
//						System.out.println("Received SP event: "+result+"\n\t"+results);
						res = result;
					}
				}
				// New non-event result?
				else if(!filter.contains(result.toString()))
				{
					filter.insert(result.toString());
					res	= result;
				}	
			
				if(res!=null)
				{		
					// check multiplicity constraints
					resultcnt++;
					int max = query.getMultiplicity().getTo();
					
					if(max<0 || resultcnt<=max)
					{
						return processResult(result);
					}
					else
					{
						return DROP_INTERMEDIATE_RESULT;
					}
				}
				else
				{
					return DROP_INTERMEDIATE_RESULT;
				}
			}
			
			@Override
			public void handleAfterIntermediateResult(Object result) throws Exception
			{
				if(DROP_INTERMEDIATE_RESULT.equals(result))
					return;
				
				int max = query.getMultiplicity().getTo();
				// if next result is not allowed any more
				if(max>0 && resultcnt+1>max)
				{
					((IntermediateFuture)ret[0]).setFinishedIfUndone();
					Exception reason = new RuntimeException("Max number of values received: "+max);
					localresults.terminate(reason);
					if(ret[1]!=null)
						ret[1].terminate(reason);
				}
			}
			
			@Override
			public void handleTerminated(Exception reason) 
			{
				if(ret[1]!=null)
					ret[1].terminate(reason);
			}
		});
		ret[0] = tfut;
		
		if(isRemote(query))
		{
			if(getRemoteServiceHandler()==null)
			{
				System.out.println("No remote service handler available for remote query: "+query);
			}
			else
			{
				ret[1] = getRemoteServiceHandler().addQuery(query);
				
				ret[1].next(
					result->
					{
						@SuppressWarnings("unchecked")
						IntermediateFuture<T> ifut = (IntermediateFuture<T>)ret[0];
						ifut.addIntermediateResultIfUndone(result);
					})
				.catchEx(exception -> {}); 
			}
		}
		
		return ret[0];
	}
	

	//-------- helper methods --------

	/**
	 *  Enhance a query before processing.
	 *  Does some necessary preprocessing and needs to be called at least once before processing the query.
	 *  @param query The query to be enhanced.
	 */
	protected <T> void enhanceQuery(ServiceQuery<T> query, boolean multi)
	{
		// Set owner if not set
		if(query.getOwner()==null)
			query.setOwner(getComponent().getId());
		
		if(query.getMultiplicity()==null)
		{
			// Fix multiple flag according to single/multi method 
			query.setMultiplicity(multi ? Multiplicity.ZERO_MANY : Multiplicity.ONE);
		}
		
		// Network names not set by user?
		if(Arrays.equals(query.getGroupNames(), ServiceQuery.GROUPS_NOT_SET))
		{
			// Local or unrestricted?
			if(!isRemote(query) || Boolean.TRUE.equals(query.isUnrestricted()))
//				|| query.getServiceType()!=null && ServiceIdentifier.isUnrestricted(self, query.getServiceType().getType(self.getClassLoader()))) 
			{
				// Unrestricted -> Don't check networks.
				query.setGroupNames((String[])null);
			}
			else
			{
				if(getRemoteServiceHandler()!=null)
				{
					// Remote -> use network names from remote service handler.
					query.setGroupNames(getRemoteServiceHandler().getGroupNames().toArray(new String[0]));
				}
				else
				{
					throw new RuntimeException("Cannot enhance query with network names, as no remote service handler is available. " +
						"Please add the remote service feature to your component.");
				}
			}
		}
		
		//if(isRemote(query))
		//	System.out.println("Query enhanced: "+query+" "+query.getGroupNames());
		
	}
	
	/**
	 *  Check if a query is potentially remote.
	 *  @return True, if scope is set to a remote scope (e.g. global or network).
	 */
	protected static boolean isRemote(ServiceQuery<?> query)
	{
		//return query.getSearchStart()!=null && query.getSearchStart().getRoot()!=getComponent().getId().getRoot() || 
		return !query.getScope().isLocal();
	}

	/**
	 * 
	 * @param result
	 * @param info
	 * @return
	 */
	protected Object processResult(Object result)
	{
		if(result instanceof IService)
		{
			// If service object -> return it.
			return result;
		}
		else if(result instanceof ServiceEvent)
		{
			// If service event -> return it.
			return result;
		}
		else if(result instanceof IServiceIdentifier)
		{
			// If service identifier -> return service proxy.
			return getServiceProxy((IServiceIdentifier)result);
		}
		else
		{
			throw new IllegalArgumentException("Cannot process result: "+result+" of type "+result.getClass());
		}
	}
	
	protected IComponent getComponent()
	{
		return self;
	}
	
	/**
	 *  Create the user-facing object from the received search or query result.
	 *  Result may be service object, service identifier (local or remote), or event.
	 *  User object is either event or service (with or without required proxy).
	 */
	public IService getServiceProxy(IServiceIdentifier sid)
	{
		IService ret = null;
		
		// If service identifier -> find/create service object or proxy
			
		// Local component -> fetch local service object.
		if(sid.getProviderId().getGlobalProcessIdentifier().equals(getComponent().getId().getGlobalProcessIdentifier()))
		{
			ret = ServiceRegistry.getRegistry().getLocalService(sid); 		
			
			if(ret==null)
				System.out.println("Could not find local service for: "+sid+" "+ServiceRegistry.getRegistry().getAllServices());
		}
		
		// Remote component -> create remote proxy
		else if(getRemoteServiceHandler()!=null)
		{
			//System.out.println("Creating remote service proxy for: "+sid+" "+getRemoteServiceHandler().getRemoteServiceProxy(self, sid));
			
			ret = getRemoteServiceHandler().getRemoteServiceProxy(self, sid).get();
			
			/*// public static IService createRemoteServiceProxy(IComponent localcomp, IServiceIdentifier remotesvc)
			Class<?> handlercl = SReflect.findClass0("jadex.remoteservice.impl.RemoteMethodInvocationHandler", 
				null, IComponentManager.get().getClassLoader());
			if(handlercl==null)
				throw new RuntimeException("Cannot create proxy for remote service without remote service feataure");
			try
			{
				Method m = handlercl.getMethod("createRemoteServiceProxy", new Class[] {IComponent.class, IServiceIdentifier.class});
				ret = (IService)m.invoke(m, new Object[] {getComponent(), sid});
			}
			catch(Exception e)
			{
				e.printStackTrace();
				SUtil.rethrowAsUnchecked(e);
			}*/
		}
		
		// else service event -> just return event, as desired by user (specified in query return type)
		
		//if(ret!=null)
		//	ret = addRequiredServiceProxy(ret, info);
		
		return ret;
	}
	
	public IRemoteServiceHandler getRemoteServiceHandler()
	{
		IRemoteServiceHandler handler = null;
		try 
		{
			//Class<?> cl = SReflect.findClass("jadex.registry.IRegistryClientService", 
			//	null, IComponentManager.get().getClassLoader());
			//System.out.println("cl is: "+cl);
			//handler = (IRemoteServiceHandler)self.getFeature(IRequiredServiceFeature.class).getLocalService(cl);
			handler = (IRemoteServiceHandler)self.getFeature(IRequiredServiceFeature.class).getLocalService(IRemoteServiceHandler.class);
			//System.out.println("found remote service handler: "+handler);
		}
		catch(Exception e) 
		{
			//System.out.println("No remote service handler found: "+e);
			//e.printStackTrace();
		}
		return handler;	
	}
}
