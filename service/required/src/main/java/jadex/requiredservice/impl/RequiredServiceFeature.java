package jadex.requiredservice.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import jadex.core.IComponent;
import jadex.execution.future.ComponentFutureFunctionality;
import jadex.execution.future.FutureFunctionality;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.ITerminableIntermediateFuture;
import jadex.future.IntermediateFuture;
import jadex.future.TerminableIntermediateFuture;
import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.impl.search.IServiceRegistry;
import jadex.providedservice.impl.search.ServiceQuery;
import jadex.providedservice.impl.search.ServiceRegistry;
import jadex.providedservice.impl.search.ServiceQuery.Multiplicity;
import jadex.requiredservice.IRequiredServiceFeature;
import jadex.requiredservice.ServiceNotFoundException;

/**
 *  Component feature that handles injection and search/query services.
 */
public class RequiredServiceFeature implements IRequiredServiceFeature
{
	/** The component. */
	protected IComponent	self;
	
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
		Future<T>	ret	= new Future<>();
		
		enhanceQuery(query, false);
		IServiceIdentifier	sid	= ServiceRegistry.getRegistry().searchService(query);
		if(sid==null)
		{
			// TODO: remote search
			ret.setException(new ServiceNotFoundException(query));
		}
		else
		{
			@SuppressWarnings("unchecked")
			T	service	= (T)ServiceRegistry.getRegistry().getLocalService(sid);
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
		Collection<IServiceIdentifier>	sids	= ServiceRegistry.getRegistry().searchServices(query);
		for(IServiceIdentifier sid: sids)
		{
			@SuppressWarnings("unchecked")
			T	service	= (T)ServiceRegistry.getRegistry().getLocalService(sid);
			ret.addIntermediateResult(service);
		}
		
		// TODO: remote search
		
		ret.setFinished();
		
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
		enhanceQuery(query, true);
		
		// TODO: query remote
		
		// Query local registry
		IServiceRegistry registry = ServiceRegistry.getRegistry();
		ISubscriptionIntermediateFuture<Object> localresults = (ISubscriptionIntermediateFuture<Object>)registry.addQuery(query);
		
		final int[] resultcnt = new int[1];
		@SuppressWarnings("rawtypes")
		ISubscriptionIntermediateFuture[] ret = new ISubscriptionIntermediateFuture[1]; 
		@SuppressWarnings({"unchecked", "rawtypes"})
		ISubscriptionIntermediateFuture<T> ret0	= (ISubscriptionIntermediateFuture)FutureFunctionality
			// Component functionality as local registry pushes results on arbitrary thread.
			.getDelegationFuture(localresults, new ComponentFutureFunctionality(self)
		{
			@Override
			public Object handleIntermediateResult(Object result) throws Exception
			{
				// check multiplicity constraints
				resultcnt[0]++;
				int max = query.getMultiplicity().getTo();
				
				if(max<0 || resultcnt[0]<=max)
				{
					return processResult(result);
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
				if(max>0 && resultcnt[0]+1>max)
				{
					((IntermediateFuture)ret[0]).setFinishedIfUndone();
					Exception reason = new RuntimeException("Max number of values received: "+max);
					localresults.terminate(reason);
				}
			}
		});
		
		ret[0]	= ret0;
		
		return ret0;
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
			query.setOwner(self.getId());
		
		if(query.getMultiplicity()==null)
		{
			// Fix multiple flag according to single/multi method 
			query.setMultiplicity(multi ? Multiplicity.ZERO_MANY : Multiplicity.ONE);
		}
		
		// Network names not set by user?
		if(Arrays.equals(query.getNetworkNames(), ServiceQuery.NETWORKS_NOT_SET))
		{
			// Local or unrestricted?
			if(!isRemote(query) || Boolean.TRUE.equals(query.isUnrestricted()))
//				|| query.getServiceType()!=null && ServiceIdentifier.isUnrestricted(self, query.getServiceType().getType(self.getClassLoader()))) 
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
		if(result instanceof IServiceIdentifier)
		{
			// TODO: remote services
			return ServiceRegistry.getRegistry().getLocalService((IServiceIdentifier)result);
		}
		else
		{
			throw new UnsupportedOperationException(result.getClass().getName());
		}
	}
}
