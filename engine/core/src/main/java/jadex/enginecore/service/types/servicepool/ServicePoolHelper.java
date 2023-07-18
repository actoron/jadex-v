package jadex.enginecore.service.types.servicepool;

import jadex.enginecore.IInternalAccess;
import jadex.enginecore.service.IService;
import jadex.enginecore.service.IServiceIdentifier;
import jadex.enginecore.service.ServiceScope;
import jadex.enginecore.service.search.ServiceQuery;
import jadex.future.Future;
import jadex.future.IFuture;

/**
 *  Helper methods for advanced service pool management.
 */
public class ServicePoolHelper
{
	/**
	 *  Test if a service is pooled.
	 *  @param service The service.
	 *  @return True, if it is a pooled service.
	 */
	public static IFuture<Integer> getFreeCapacity(IInternalAccess ia, IService service)
	{
		Future<Integer> ret = new Future<Integer>();
		IServiceIdentifier sid = service.getServiceId();
		ia.searchService(new ServiceQuery<IServicePoolService>(IServicePoolService.class).setProvider(sid.getProviderId()).setScope(ServiceScope.GLOBAL))
			.then(ps -> ps.getFreeCapacity(sid.getServiceType().getType(ia.getClassLoader())).delegateTo(ret))
			.catchEx(ex -> { 
				//ex.printStackTrace(); 
				ret.setResult(-1); 
			});
		return ret;
	}
	
	/**
	 *  Test if a service is pooled.
	 *  @param service The service.
	 *  @return True, if it is a pooled service.
	 */
	public static IFuture<Integer> getMaxCapacity(IInternalAccess ia, IService service)
	{
		Future<Integer> ret = new Future<Integer>();
		IServiceIdentifier sid = service.getServiceId();
		ia.searchService(new ServiceQuery<IServicePoolService>(IServicePoolService.class).setProvider(sid.getProviderId()))
			.then(ps -> ps.getMaxCapacity(sid.getServiceType().getType(ia.getClassLoader())).delegateTo(ret))
			.catchEx(ex -> { 
				//ex.printStackTrace(); 
				ret.setResult(-1); 
			});
		return ret;
	}
	
	/**
	 *  Test if a service is pooled.
	 *  @param service The service.
	 *  @return True, if it is a pooled service.
	 */
	public static IFuture<Boolean> isPooledService(IInternalAccess ia, IService service)
	{
		Future<Boolean> ret = new Future<Boolean>();
		IServiceIdentifier sid = service.getServiceId();
		ia.searchService(new ServiceQuery<IServicePoolService>(IServicePoolService.class).setProvider(sid.getProviderId()))
			.then(ps -> ret.setResult(Boolean.TRUE))
			.catchEx(ex -> ret.setResult(Boolean.FALSE));
		return ret;
	}
}
