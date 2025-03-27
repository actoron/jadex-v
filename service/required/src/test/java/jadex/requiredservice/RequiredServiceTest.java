package jadex.requiredservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadex.core.ComponentIdentifier;
import jadex.core.ComponentTerminatedException;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.IThrowingFunction;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;
import jadex.providedservice.annotation.Service;
import jadex.requiredservice.annotation.InjectService;

/**
 *  Test automatic provided service registration.
 */
public class RequiredServiceTest
{
	public static final long	TIMEOUT	= 10000;
	
	//-------- test interfaces --------
	
	@Service
	@FunctionalInterface
	interface IHelloService
	{
		public IFuture<String> sayHello(String name);
	}
	
	//-------- test methods --------
	
	@Test
	public void	testGetLocalService()
	{
		IComponentHandle	provider	= null;
		try
		{
			// helper objects
			IComponentHandle	caller	= IComponentManager.get().create(new Object()).get(TIMEOUT);
			IThrowingFunction<IComponent, IHelloService>	getservice
				= comp -> comp.getFeature(IRequiredServiceFeature.class).getLocalService(IHelloService.class);
			
			// Test that service is not found
			assertThrows(ServiceNotFoundException.class, () -> caller.scheduleStep(getservice).get(TIMEOUT));
			
			// Test that service is found.
			provider	= IComponentManager.get().create((IHelloService)name -> new Future<>("Hello "+name)).get(TIMEOUT);
			assertInstanceOf(IHelloService.class, caller.scheduleStep(getservice).get(TIMEOUT));
		}
		finally
		{
			if(provider!=null)
			{
				provider.terminate().get(TIMEOUT);
			}
		}
	}

	@Test
	public void	testSearchService()
	{
		IComponentHandle	provider	= null;
		try
		{
			// helper objects
			IComponentHandle	caller	= IComponentManager.get().create(new Object()).get(TIMEOUT);
			IThrowingFunction<IComponent, IHelloService>	search
				= comp -> comp.getFeature(IRequiredServiceFeature.class).searchService(IHelloService.class).get(TIMEOUT);
			
			// Test that service is not found
			assertThrows(ServiceNotFoundException.class, () -> caller.scheduleStep(search).get(TIMEOUT));
			
			// Test that service is found.
			provider	= IComponentManager.get().create((IHelloService)name -> new Future<>("Hello "+name)).get(TIMEOUT);
			assertInstanceOf(IHelloService.class, caller.scheduleStep(search).get(TIMEOUT));
		}
		finally
		{
			if(provider!=null)
			{
				provider.terminate().get(TIMEOUT);
			}
		}
	}

	@Test
	public void	testGetLocalServices()
	{
		IComponentHandle	provider	= null;
		IComponentHandle	provider2	= null;
		try
		{
			// helper objects
			IComponentHandle	caller	= IComponentManager.get().create(new Object()).get(TIMEOUT);
			IThrowingFunction<IComponent, Collection<IHelloService>>	getservices
				= comp -> comp.getFeature(IRequiredServiceFeature.class).getLocalServices(IHelloService.class);
			
			// Test that services are not found
			assertTrue(caller.scheduleStep(getservices).get(TIMEOUT).isEmpty());
			
			// Test that services are found.
			provider	= IComponentManager.get().create((IHelloService)name -> new Future<>("Hello "+name)).get(TIMEOUT);
			provider2	= IComponentManager.get().create((IHelloService)name -> new Future<>("Hello "+name)).get(TIMEOUT);
			assertEquals(2, caller.scheduleStep(getservices).get(TIMEOUT).size());
		}
		finally
		{
			if(provider!=null)
			{
				provider.terminate().get(TIMEOUT);
			}
			if(provider2!=null)
			{
				provider2.terminate().get(TIMEOUT);
			}
		}
	}

	@Test
	public void	testSearchServices()
	{
		IComponentHandle	provider	= null;
		IComponentHandle	provider2	= null;
		try
		{
			// helper objects
			IComponentHandle	caller	= IComponentManager.get().create(new Object()).get(TIMEOUT);
			IThrowingFunction<IComponent, Collection<IHelloService>>	search
				= comp -> comp.getFeature(IRequiredServiceFeature.class).searchServices(IHelloService.class).get(TIMEOUT);
			
			// Test that services are not found
			assertTrue(caller.scheduleStep(search).get(TIMEOUT).isEmpty());
			
			// Test that services are found.
			provider	= IComponentManager.get().create((IHelloService)name -> new Future<>("Hello "+name)).get(TIMEOUT);
			provider2	= IComponentManager.get().create((IHelloService)name -> new Future<>("Hello "+name)).get(TIMEOUT);
			assertEquals(2, caller.scheduleStep(search).get(TIMEOUT).size());
		}
		finally
		{
			if(provider!=null)
			{
				provider.terminate().get(TIMEOUT);
			}
			if(provider2!=null)
			{
				provider2.terminate().get(TIMEOUT);
			}
		}
	}

	@Test
	public void	testQueryServices()
	{
		IComponentHandle[]	provider	= new IComponentHandle[1];
		try
		{
			// helper objects
			IComponentHandle	caller	= IComponentManager.get().create(new Object()).get(TIMEOUT);
			caller.scheduleStep(comp ->
			{
				// Add query
				List<IHelloService> results	= new ArrayList<>();
				ISubscriptionIntermediateFuture<IHelloService>	query	=
					comp.getFeature(IRequiredServiceFeature.class).addQuery(IHelloService.class);
				query.next(result ->
				{
					results.add(result);
				});
	
				// Check nothing found initially
				assertTrue(results.isEmpty());
				
				// Check that a service is found
				provider[0]	= IComponentManager.get().create((IHelloService)name -> new Future<>("Hello "+name)).get(TIMEOUT);
				
				// Schedule check to make sure it is executed after result add.
				caller.scheduleStep(() ->
				{
					assertEquals(1, results.size());
					return null;
				}).get(TIMEOUT);
				
				return null;
			}).get(TIMEOUT);
		}
		finally
		{
			if(provider[0]!=null)
			{
				provider[0].terminate().get(TIMEOUT);
			}
		}
	}

	@Test
	public void	testServiceCall()
	{
		IComponentHandle	provider	= null;
		try
		{
			// helper objects
			IComponentHandle	caller	= IComponentManager.get().create(new Object()).get(TIMEOUT);
			IThrowingFunction<IComponent, IHelloService>	getservice
				= comp -> comp.getFeature(IRequiredServiceFeature.class).getLocalService(IHelloService.class);
			provider	= IComponentManager.get().create((IHelloService)name -> new Future<>("Hello "+name)).get(TIMEOUT);
			
			// Test that service call returns on component thread
			IHelloService	service	= caller.scheduleStep(getservice).get(TIMEOUT);
			Future<ComponentIdentifier>	fut	= new Future<ComponentIdentifier>();
			caller.scheduleStep(() -> 
			{
				service.sayHello("world")
					.then(hello -> fut.setResult(IComponentManager.get().getCurrentComponent().getId()))
					.catchEx(e -> fut.setException(e));
			});
			assertEquals(caller.getId(), fut.get(TIMEOUT));
		}
		finally
		{
			if(provider!=null)
			{
				provider.terminate().get(TIMEOUT);
			}
		}
	}
	
	@Test
	public void	testFieldInjection()
	{
		// Start service
		IComponentHandle	provider	= IComponentManager.get().create((IHelloService)name -> new Future<>("Hello "+name)).get(TIMEOUT);
		
		try
		{
			// Check that service is injected into field
			Future<IHelloService>	fut	= new Future<>();
			IComponentManager.get().create(new Object()
			{
				@Inject
				IHelloService	myservice;
				
				@OnStart
				void start()
				{
					fut.setResult(myservice);
				}
			});
			assertNotNull(fut.get(TIMEOUT));
		}
		finally
		{
			provider.terminate().get(TIMEOUT);
		}
		
		// Test that previous field service is not provided.
		assertThrows(ServiceNotFoundException.class,
			() -> IComponentManager.get().create(new Object()
			{
				@Inject
				IHelloService myservice;
			}).get(TIMEOUT));
	}

	@Test
	public void	testSubannoFieldInjection()
	{
		// Start service
		IComponentHandle	provider	= IComponentManager.get().create((IHelloService)name -> new Future<>("Hello "+name)).get(TIMEOUT);
		
		try
		{
			// Check that service is injected into field
			Future<IHelloService>	fut	= new Future<>();
			IComponentManager.get().create(new Object()
			{
				@InjectService
				IHelloService	myservice;
				
				@OnStart
				void start()
				{
					fut.setResult(myservice);
				}
			});
			assertNotNull(fut.get(TIMEOUT));
		}
		finally
		{
			provider.terminate().get(TIMEOUT);
		}
		
		// Test that previous field service is not provided.
		assertThrows(ServiceNotFoundException.class,
			() -> IComponentManager.get().create(new Object()
			{
				@InjectService
				IHelloService myservice;
			}).get(TIMEOUT));
	}

	@Test
	public void	testParameterInjection()
	{
		// Start service
		IComponentHandle	provider	= IComponentManager.get().create((IHelloService)name -> new Future<>("Hello "+name)).get(TIMEOUT);
		
		try
		{
			// Check that service is injected into parameter
			Future<IHelloService>	fut	= new Future<>();
			IComponentManager.get().create(new Object()
			{
				@OnStart
				void start(IHelloService myservice)
				{
					fut.setResult(myservice);
				}
			});
			assertNotNull(fut.get(TIMEOUT));
		}
		finally
		{
			provider.terminate().get(TIMEOUT);
		}
	}

	@Test
	public void	testFieldNotFound()
	{
		// Check that exception is thrown
		assertThrows(ServiceNotFoundException.class,
			() -> IComponentManager.get().create(new Object()
			{
				@Inject
				IHelloService myservice;
			}).get(TIMEOUT));
	}

	@Test
	public void	testParameterNotFound()
	{
		// Check that exception occurs in OnStart step.
		IComponentHandle	handle	= IComponentManager.get().create(new Object()
		{
			@OnStart
			void start(IHelloService myservice)
			{
				System.out.println("myservice: "+myservice);
			}
		}).get(TIMEOUT);
		
		assertThrows(ComponentTerminatedException.class, () -> handle.scheduleStep(() -> {return null;}).get());
	}

	@Test
	public void	testMethodInjection()
	{
		IComponentHandle	provider[]	= new IComponentHandle[1];
		IComponentHandle	provider2	= null;
		try
		{
			// helper objects
			List<IHelloService>	services	= new ArrayList<>();
			IComponentHandle	caller	= IComponentManager.get().create(new Object()
			{
				@Inject
				void addService(IComponent comp, IHelloService hello)
				{
					assertNotNull(comp);
					services.add(hello);
				}
			}).get(TIMEOUT);
			
			assertTrue(services.isEmpty());
			
			// TODO: why step necessary!?
			caller.scheduleStep(() ->
			{
				provider[0]	= IComponentManager.get().create((IHelloService)name -> new Future<>("Hello "+name)).get(TIMEOUT);
				return null;
			}).get(TIMEOUT);
			
			// Schedule check to make sure it is executed after result add.
			caller.scheduleStep(() ->
			{
				assertEquals(1, services.size());
				return null;
			}).get();
			
			provider2	= IComponentManager.get().create((IHelloService)name -> new Future<>("Hello "+name)).get(TIMEOUT);
			// Schedule check to make sure it is executed after result add.
			caller.scheduleStep(() ->
			{
				assertEquals(2, services.size());
				return null;
			}).get(TIMEOUT);
		}
		finally
		{
			if(provider[0]!=null)
			{
				provider[0].terminate().get(TIMEOUT);
			}
			if(provider2!=null)
			{
				provider2.terminate().get(TIMEOUT);
			}
		}
	}

	@Test
	public void	testBrokenMethodInjection()
	{
		assertThrows(UnsupportedOperationException.class, () -> IComponentManager.get().create(new Object()
		{
			@Inject
			void addService(IHelloService hello1, IHelloService hello2)
			{
			}
		}).get(TIMEOUT));			
	}

	@Test
	public void	testPlainField()
	{
		// Test that nothing happens to a field with service type but no annos.
		Future<IHelloService>	fut	= new Future<>();
		IComponentManager.get().create(new Object()
		{
			IHelloService hello2;
			
			@OnStart
			void start()
			{
				fut.setResult(hello2);
			}
		}).get(TIMEOUT);
		
		assertNull(fut.get(TIMEOUT));
	}
}
