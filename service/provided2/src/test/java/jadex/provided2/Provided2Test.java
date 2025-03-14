package jadex.provided2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.injection.annotation.OnEnd;
import jadex.injection.annotation.OnStart;
import jadex.provided2.annotation.Service;
import jadex.provided2.impl.search.ServiceQuery;
import jadex.provided2.impl.search.ServiceRegistry;

/**
 *  Test automatic provided service registration.
 */
public class Provided2Test
{
	public static final long	TIMEOUT	= 10000;
	
	//-------- test interfaces --------
	
	@Service
	public interface IMyService {}
	
	@Service
	@FunctionalInterface
	public interface IMyLambdaService
	{
		public IFuture<ComponentIdentifier>	myMethod();
	}
	
	//-------- test methods --------
	
	@Test
	public void	testPojoService()
	{
		Future<Void>	start	= new Future<>();
		Future<Void>	end	= new Future<>();
		
		IComponentHandle	comp = IComponentManager.get().create(new IMyService()
		{
			@OnStart
			void onStart()
			{
				start.setResult(null);
			}
			
			@OnEnd
			void onEnd()
			{
				end.setResult(null);
			}
		}).get(TIMEOUT);
		
		assertTrue(start.isDone());
		assertFalse(end.isDone());
		
		// TODO: test service registration
		
		comp.terminate().get(TIMEOUT);
		assertTrue(end.isDone());
	}

	@Test
	public void	testFieldService()
	{
		Future<Void>	compstart	= new Future<>();
		Future<Void>	compend	= new Future<>();
		Future<Void>	servicestart	= new Future<>();
		Future<Void>	serviceend	= new Future<>();
		
		IComponentHandle	comp = IComponentManager.get().create(new Object()
		{
			@SuppressWarnings("unused")
			IMyService	myservice	= new IMyService()
			{
				@OnStart
				void onStart()
				{
					assertFalse(compstart.isDone());
					servicestart.setResult(null);
				}
				
				@OnEnd
				void onEnd()
				{
					assertFalse(compend.isDone());
					serviceend.setResult(null);
				}
			};
			
			@OnStart
			void onStart()
			{
				assertTrue(servicestart.isDone());
				compstart.setResult(null);
			}
			
			@OnEnd
			void onEnd()
			{
				assertTrue(serviceend.isDone());
				compend.setResult(null);
			}
		}).get(TIMEOUT);
		
		assertTrue(servicestart.isDone());
		assertTrue(compstart.isDone());
		assertFalse(serviceend.isDone());
		assertFalse(compend.isDone());
		
		// TODO: test service registration
		
		comp.terminate().get(TIMEOUT);
		assertTrue(serviceend.isDone());
		assertTrue(compend.isDone());
	}

	@Test
	public void	testLambdaService()
	{
		// Create service component
		IComponentHandle	handle	= IComponentManager.get().create(
			(IMyLambdaService)() ->
				new Future<>(IComponentManager.get().getCurrentComponent().getId())
		).get();
		
		// Find service in registry
		ServiceQuery<IMyLambdaService>	query	= new ServiceQuery<>(IMyLambdaService.class).setOwner(handle.getId()).setNetworkNames();
		IServiceIdentifier	sid	= ServiceRegistry.getRegistry().searchService(query);
		assertNotNull(sid);
		
		// Get service proxy
		IMyLambdaService	service	= (IMyLambdaService)ServiceRegistry.getRegistry().getLocalService(sid);
		assertNotNull(service);
		
		// IService
		((IService)service).getServiceId();
		
		// Call service from other component
		Future<ComponentIdentifier>	cidfut	= new Future<>();
		Future<ComponentIdentifier>	retfut	= new Future<>();
		IComponentHandle	handle2	= IComponentManager.get().create(new Object()
		{
			@OnStart
			void callService()
			{
				service.myMethod().then(cid -> 
				{
					cidfut.setResult(cid);
					retfut.setResult(IComponentManager.get().getCurrentComponent().getId());
				});
			}
		}).get(TIMEOUT);
		
		// Check if service cid is returned
		assertEquals(handle.getId(), cidfut.get(TIMEOUT));

		// Check if return call is scheduled on caller thread
		assertEquals(handle2.getId(), retfut.get(TIMEOUT));
	}
}
