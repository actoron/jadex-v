package jadex.enginecore.service.component.multiinvoke;

import java.lang.reflect.Method;

import jadex.common.IFilter;
import jadex.common.Tuple2;
import jadex.enginecore.service.IService;
import jadex.future.IIntermediateFuture;

/**
 *  Interface for multiplex call distributor.
 *  
 *  It is fed with:
 *  - the services found one by one (addService)
 *  - when the search has finished (serviceSearchFinished) 
 *  
 *  It determines:
 *  - which services are called
 *  - with which arguments
 *  - when finished
 */
public interface IMultiplexDistributor
{
	/**
	 *  Init the call distributor.
	 */
	public IIntermediateFuture<Object> init(Method method, Object[] args, 
		IFilter<Tuple2<IService, Object[]>> filter, IParameterConverter conv);
	
	/**
	 *  Add a new service.
	 *  @param service The service.
	 */
	public void addService(IService service);
	
	/**
	 *  Search for services has finished.
	 */
	public void serviceSearchFinished();
	
}
