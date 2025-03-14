package jadex.provided2.impl.search;

import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import jadex.common.ClassInfo;
import jadex.provided2.IService;

/**
 *  Interface for the search functionality to get the registry data.
 */
public interface IRegistryDataProvider
{
	/**
	 *  Get services per type.
	 *  @param type The interface type. If type is null all services are returned.
	 *  @return First matching service or null.
	 */
	// read
	public Iterator<IService> getServices(ClassInfo type);
	
	/**
	 *  Get queries per type.
	 *  @param type The interface type. If type is null all services are returned.
	 *  @return The queries.
	 */
	// read
	public <T> Set<ServiceQueryInfo<T>> getQueries(ClassInfo type);
	
	/**
	 *  Test if a service is included.
	 *  @param ser The service.
	 *  @return True if is included.
	 */
	public boolean isIncluded(UUID cid, IService ser);
}
