package jadex.required2.impl;

import java.util.Arrays;
import java.util.Collection;

import jadex.core.IComponent;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.ITerminableIntermediateFuture;
import jadex.provided2.IServiceIdentifier;
import jadex.provided2.impl.search.ServiceQuery;
import jadex.provided2.impl.search.ServiceQuery.Multiplicity;
import jadex.provided2.impl.search.ServiceRegistry;
import jadex.required2.IRequired2Feature;
import jadex.required2.ServiceNotFoundException;

/**
 *  Component feature that handles injection and search/query services.
 */
public class Required2Feature implements IRequired2Feature
{
	/** The component. */
	protected IComponent	self;
	
	/**
	 *  Create the injection feature.
	 */
	public Required2Feature(IComponent self)
	{
		this.self	= self;
	}
		
	//-------- IRequired2Feature interface --------

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
	public <T> Collection<T> getLocalServices(ServiceQuery<T> query)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> IFuture<T> searchService(ServiceQuery<T> query)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> ITerminableIntermediateFuture<T> searchServices(ServiceQuery<T> query)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> ISubscriptionIntermediateFuture<T> addQuery(ServiceQuery<T> query)
	{
		throw new UnsupportedOperationException();
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
	public boolean isRemote(ServiceQuery<?> query)
	{
		//return query.getSearchStart()!=null && query.getSearchStart().getRoot()!=getComponent().getId().getRoot() || 
		return !query.getScope().isLocal();
	}
}
