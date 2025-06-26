package jadex.providedservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.annotation.NoCopy;
import jadex.core.impl.ComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnEnd;
import jadex.injection.annotation.OnStart;
import jadex.providedservice.annotation.ProvideService;
import jadex.providedservice.annotation.Service;
import jadex.providedservice.impl.search.ServiceQuery;
import jadex.providedservice.impl.search.ServiceRegistry;

/**
 *  Test automatic provided service registration.
 */
public class ProvidedServiceTest
{
	public static final long	TIMEOUT	= 10000;
	
	//-------- test interfaces/classes --------
	
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
	
	@ProvideService
	public class AnnoImpl	implements IMyService {}
	
	@ProvideService
	public class MultiImpl	implements IMyService, IMyLambdaService
	{
		@Override
		public IFuture<ComponentIdentifier> myMethod()
		{
			return new Future<>(new UnsupportedOperationException());
		}
	}
	
	@ProvideService
	public class BrokenAnnoImpl	{}
	
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
		comp.scheduleStep(() ->
		{
			assertTrue(start.isDone());
			assertFalse(end.isDone());
			return null;
		}).get(TIMEOUT);
		
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
			@ProvideService
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
		comp.scheduleStep(() ->
		{
			assertTrue(servicestart.isDone());
			assertTrue(compstart.isDone());
			assertFalse(serviceend.isDone());
			assertFalse(compend.isDone());
			return null;
		}).get(TIMEOUT);
		
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
	public void	testBrokenFieldService()
	{
//		IComponentHandle	handle	= IComponentManager.get().create(new Object()
//		{
//			@ProvideService
//			Object	myservice	= new IMyService(){};
//		}).get(TIMEOUT);
//		
//		SUtil.runWithoutOutErr(
//			() -> assertThrows(ComponentTerminatedException.class, () -> handle.scheduleStep(() -> {return null;}).get(TIMEOUT)));
		
		assertThrows(RuntimeException.class,
			() -> IComponentManager.get().create(new Object()
		{
			@ProvideService
			Object	myservice	= new IMyService(){};
		}).get(TIMEOUT));
	}

	@Test
	public void	testBrokenMethodService()
	{
//		IComponentHandle	handle	= IComponentManager.get().create(new Object()
//		{
//			@ProvideService
//			Object	createService(IComponent comp)
//			{
//				return new IMyService(){};
//			}
//		}).get(TIMEOUT);
//		
//		SUtil.runWithoutOutErr(
//			() -> assertThrows(ComponentTerminatedException.class, () -> handle.scheduleStep(() -> {return null;}).get(TIMEOUT)));
		
		assertThrows(RuntimeException.class,
			() -> IComponentManager.get().create(new Object()
		{
			@ProvideService
			Object	createService(IComponent comp)
			{
				return new IMyService(){};
			}
		}).get(TIMEOUT));
	}

	@Test
	public void	testMethodService()
	{
		IComponentHandle	comp = IComponentManager.get().create(new Object()
		{
			@ProvideService
			IMyService	createService(IComponent comp)
			{
				return new IMyService(){};
			}
		}).get(TIMEOUT);
		comp.scheduleStep(() -> null).get(TIMEOUT);
		
		// Test that service can be found
		assertNotNull(searchService(comp, IMyService.class));
		
		// Test that service is no longer found
		comp.terminate().get(TIMEOUT);
		assertNull(searchSid0(comp, IMyService.class));		
	}

	@Test
	public void	testClassService()
	{
		IComponentHandle	comp = IComponentManager.get().create(new AnnoImpl()).get(TIMEOUT);
		comp.scheduleStep(() -> null).get(TIMEOUT);
		
		// Test that service can be found
		assertNotNull(searchService(comp, IMyService.class));
		
		// Test that service is no longer found
		comp.terminate().get(TIMEOUT);
		assertNull(searchSid0(comp, IMyService.class));		
	}

	@Test
	public void	testMultiService()
	{
		IComponentHandle	comp = IComponentManager.get().create(new MultiImpl()).get(TIMEOUT);
		comp.scheduleStep(() -> null).get(TIMEOUT);
		
		// Test that services can be found
		assertNotNull(searchService(comp, IMyService.class));
		assertNotNull(searchService(comp, IMyLambdaService.class));
		
		// Test that services are no longer found
		comp.terminate().get(TIMEOUT);
		assertNull(searchSid0(comp, IMyService.class));		
		assertNull(searchSid0(comp, IMyLambdaService.class));		
	}

	@Test
	public void	testBrokenClassService()
	{
//		IComponentHandle	handle	=
//			IComponentManager.get().create(new BrokenAnnoImpl()).get(TIMEOUT);
//		
//		SUtil.runWithoutOutErr(
//			() -> assertThrows(ComponentTerminatedException.class, () -> handle.scheduleStep(() -> {return null;}).get(TIMEOUT)));
		assertThrows(RuntimeException.class,
			() -> IComponentManager.get().create(new BrokenAnnoImpl()).get(TIMEOUT));
	}

	@Test
	public void	testLambdaService()
	{
		// Create service component
		IComponentHandle	handle	= IComponentManager.get().create(
			(IMyLambdaService)() ->
			{
				// Hack!!! wait so the service call future is not finished before .then() is called.
				IComponentManager.get().getCurrentComponent().getFeature(IExecutionFeature.class)
					.waitForDelay(100).get(TIMEOUT);
				Thread.yield();
				return new Future<>(IComponentManager.get().getCurrentComponent().getId());
			}
		).get();
		handle.scheduleStep(() -> null).get(TIMEOUT);
		
		IMyLambdaService service = searchService(handle, IMyLambdaService.class);
		
		// Test cast to IService
		assertNotNull(((IService)service).getServiceId());
		
		// Call service from non-component thread
		Future<ComponentIdentifier>	cidfut0	= new Future<>();
		Future<ComponentIdentifier>	retfut0	= new Future<>();
		service.myMethod().then(cid -> 
		{
			cidfut0.setResult(cid);
			retfut0.setResult(IComponentManager.get().getCurrentComponent().getId());
		});
		// Check if service cid is returned
		assertEquals(handle.getId(), cidfut0.get(TIMEOUT));
		// Check if return call is scheduled on global runner thread
		assertEquals(ComponentManager.get().getGlobalRunner().getId(), retfut0.get(TIMEOUT));
		
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
		
		// cleanup
		handle.terminate().get(TIMEOUT);
	}

	@Test
	public void	testNoCopyService()
	{
		// Create service component
		IComponentHandle	handle	= IComponentManager.get().create(
			(INoCopyService)obj -> new Future<>(obj)
		).get();
		handle.scheduleStep(() -> null).get(TIMEOUT);
		
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
		
		// cleanup
		handle.terminate().get(TIMEOUT);
	}

	@Test
	public void	testCopyParameter()
	{
		// Create service component
		IComponentHandle	handle	= IComponentManager.get().create(
			(ICopyParameterService)obj -> new Future<>(obj)
		).get();
		handle.scheduleStep(() -> null).get(TIMEOUT);
		
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
		
		// cleanup
		handle.terminate().get(TIMEOUT);
	}

	@Test
	public void	testCopyResult()
	{
		// Create service component
		IComponentHandle	handle	= IComponentManager.get().create(
			(ICopyResultService)obj -> new Future<>(obj)
		).get();
		handle.scheduleStep(() -> null).get(TIMEOUT);

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
		
		// cleanup
		handle.terminate().get(TIMEOUT);
	}
	
	@Test
	public void	testSidInjection()
	{
		// Test method injection.
		Future<IServiceIdentifier>	fut	= new Future<>();
		IComponentHandle	handle	= IComponentManager.get().create(new IMyService()
		{
			@OnStart
			void start(IServiceIdentifier sid)
			{
				fut.setResult(sid);
			}
		}).get(TIMEOUT);
		assertNotNull(fut.get(TIMEOUT));
		// cleanup
		handle.terminate().get(TIMEOUT);
		
		// Test field injection.
		handle	= IComponentManager.get().create(new IMyService()
		{
			@Inject
			IServiceIdentifier sid;
		}).get(TIMEOUT);
		// cleanup
		handle.terminate().get(TIMEOUT);
		
		// Test broken no service injection.
//		IComponentHandle	handle2	= IComponentManager.get().create(new Object()
//		{
//			@Inject
//			IServiceIdentifier sid;
//		}).get(TIMEOUT);
//		
//		SUtil.runWithoutOutErr(
//			() -> assertThrows(ComponentTerminatedException.class, () -> handle2.scheduleStep(() -> {return null;}).get(TIMEOUT)));
		assertThrows(RuntimeException.class,
			() -> IComponentManager.get().create(new Object()
		{
			@Inject
			IServiceIdentifier sid;
		}).get(TIMEOUT));

		// Test broken multi service injection.
//		IComponentHandle	handle3	= IComponentManager.get().create(new MultiImpl()
//		{
//			@Inject
//			IServiceIdentifier sid;
//		}).get(TIMEOUT);
//		
//		SUtil.runWithoutOutErr(
//			() -> assertThrows(ComponentTerminatedException.class, () -> handle3.scheduleStep(() -> {return null;}).get(TIMEOUT)));
		assertThrows(RuntimeException.class,
			() -> IComponentManager.get().create(new MultiImpl()
		{
			@Inject
			IServiceIdentifier sid;
		}).get(TIMEOUT));
	}
	
	//-------- helper methods --------

	protected static <T> T searchService(IComponentHandle handle, Class<T> type)
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

	protected static <T> IServiceIdentifier searchSid0(IComponentHandle handle, Class<T> type)
	{
		// Find service in registry
		ServiceQuery<T>	query	= new ServiceQuery<>(type).setOwner(handle.getId()).setNetworkNames();
		IServiceIdentifier	ret	= ServiceRegistry.getRegistry().searchService(query);
//		System.out.println("search: "+handle.getId()+", "+type+", "+ret);
		return ret;
	}
}
