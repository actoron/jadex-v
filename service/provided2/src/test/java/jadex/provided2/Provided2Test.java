package jadex.provided2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.annotation.NoCopy;
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
	
	@Service
	@FunctionalInterface
	public interface INoCopyService
	{
		public @NoCopy IFuture<Object>	noCopy(@NoCopy Object o);
	}
	
	@Service
	@FunctionalInterface
	public interface ICopyParameterService
	{
		public @NoCopy IFuture<Object>	copyParameter(Object o);
	}
	
	@Service
	@FunctionalInterface
	public interface ICopyResultService
	{
		public IFuture<Object>	copyResult(@NoCopy Object o);
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
		
		// Check correct setup calls
		assertTrue(start.isDone());
		assertFalse(end.isDone());
		
		// Test that service can be found
		assertNotNull(searchService(comp, IMyService.class));
		
		// Check correct terminate calls
		comp.terminate().get(TIMEOUT);
		assertTrue(end.isDone());
		
		// Test that service is no longer found
		assertNull(searchSid0(comp, IMyService.class));		
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
		
		// Check correct setup calls
		assertTrue(servicestart.isDone());
		assertTrue(compstart.isDone());
		assertFalse(serviceend.isDone());
		assertFalse(compend.isDone());
		
		// Test that service can be found
		assertNotNull(searchService(comp, IMyService.class));
		
		// Check correct terminate calls
		comp.terminate().get(TIMEOUT);
		assertTrue(serviceend.isDone());
		assertTrue(compend.isDone());

		// Test that service is no longer found
		assertNull(searchSid0(comp, IMyService.class));		
	}

	@Test
	public void	testLambdaService()
	{
		// Create service component
		IComponentHandle	handle	= IComponentManager.get().create(
			(IMyLambdaService)() ->
				new Future<>(IComponentManager.get().getCurrentComponent().getId())
		).get();
		
		IMyLambdaService service = searchService(handle, IMyLambdaService.class);
		
		// Test cast to IService
		assertNotNull(((IService)service).getServiceId());
		
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

	@Test
	public void	testNoCopyService()
	{
		// Create service component
		IComponentHandle	handle	= IComponentManager.get().create(
			(INoCopyService)obj -> new Future<>(obj)
		).get();
		
		INoCopyService service = searchService(handle, INoCopyService.class);
		
		// Call service from other component
		Object	obj1	= new Object();
		Future<Object>	obj2fut	= new Future<>();
		IComponentManager.get().create(new Object()
		{
			@OnStart
			void callService()
			{
				service.noCopy(obj1).then(obj2 -> 
				{
					obj2fut.setResult(obj2);
				});
			}
		}).get(TIMEOUT);
		
		// Check if object is not copied
		assertSame(obj1, obj2fut.get(TIMEOUT));
	}

	@Test
	public void	testCopyParameter()
	{
		// Create service component
		IComponentHandle	handle	= IComponentManager.get().create(
			(ICopyParameterService)obj -> new Future<>(obj)
		).get();
		
		ICopyParameterService service = searchService(handle, ICopyParameterService.class);
		
		// Call service from other component
		Object	obj1	= new Object();
		Future<Object>	obj2fut	= new Future<>();
		IComponentManager.get().create(new Object()
		{
			@OnStart
			void callService()
			{
				service.copyParameter(obj1).then(obj2 -> 
				{
					obj2fut.setResult(obj2);
				});
			}
		}).get(TIMEOUT);
		
		// Check if object is not copied
		assertNotSame(obj1, obj2fut.get(TIMEOUT));
	}

	@Test
	public void	testCopyResult()
	{
		// Create service component
		IComponentHandle	handle	= IComponentManager.get().create(
			(ICopyResultService)obj -> new Future<>(obj)
		).get();
		
		ICopyResultService service = searchService(handle, ICopyResultService.class);
		
		// Call service from other component
		Object	obj1	= new Object();
		Future<Object>	obj2fut	= new Future<>();
		IComponentManager.get().create(new Object()
		{
			@OnStart
			void callService()
			{
				service.copyResult(obj1).then(obj2 -> 
				{
					obj2fut.setResult(obj2);
				});
			}
		}).get(TIMEOUT);
		
		// Check if object is not copied
		assertNotSame(obj1, obj2fut.get(TIMEOUT));
	}

	protected <T> T searchService(IComponentHandle handle, Class<T> type)
	{
		// Find service in registry
		ServiceQuery<T>	query	= new ServiceQuery<>(type).setOwner(handle.getId()).setNetworkNames();
		IServiceIdentifier	sid	= ServiceRegistry.getRegistry().searchService(query);
		assertNotNull(sid);
		
		// Get service proxy
		@SuppressWarnings("unchecked")
		T	service	= (T)ServiceRegistry.getRegistry().getLocalService(sid);
		assertNotNull(service);
		return service;
	}

	protected <T> IServiceIdentifier searchSid0(IComponentHandle handle, Class<T> type)
	{
		// Find service in registry
		ServiceQuery<T>	query	= new ServiceQuery<>(type).setOwner(handle.getId()).setNetworkNames();
		return ServiceRegistry.getRegistry().searchService(query);
	}
}
