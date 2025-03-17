package jadex.required2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.IThrowingFunction;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.provided2.annotation.Service;
import jadex.provided2.impl.search.ServiceQuery;

/**
 *  Test automatic provided service registration.
 */
public class Required2Test
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
		// helper objects
		IComponentHandle	caller	= IComponentManager.get().create(new Object()).get(TIMEOUT);
		IThrowingFunction<IComponent, IHelloService>	getservice
		 = comp -> comp.getFeature(IRequired2Feature.class).getLocalService(new ServiceQuery<>(IHelloService.class));
		
		// Test that service is not found
		assertThrows(ServiceNotFoundException.class, () -> caller.scheduleStep(getservice).get(TIMEOUT));
		
		// Test that service is found.
		IComponentHandle	provider	= IComponentManager.get().create((IHelloService)name -> new Future<>("Hello "+name)).get(TIMEOUT);
		assertInstanceOf(IHelloService.class, caller.scheduleStep(getservice).get(TIMEOUT));
		provider.terminate().get(TIMEOUT);
		
		// Test that service is not found after shutdown
		assertThrows(ServiceNotFoundException.class, () -> caller.scheduleStep(getservice).get(TIMEOUT));
	}

	@Test
	public void	testServiceCall()
	{
		// helper objects
		IComponentHandle	caller	= IComponentManager.get().create(new Object()).get(TIMEOUT);
		IThrowingFunction<IComponent, IHelloService>	getservice
		 = comp -> comp.getFeature(IRequired2Feature.class).getLocalService(new ServiceQuery<>(IHelloService.class));
		IComponentHandle	provider	= IComponentManager.get().create((IHelloService)name ->
		{
			return new Future<>("Hello "+name);
		}).get(TIMEOUT);
		
		// Test if service call returns on component thread
		IHelloService	service	= caller.scheduleStep(getservice).get(TIMEOUT);
		Future<ComponentIdentifier>	fut	= new Future<ComponentIdentifier>();
		caller.scheduleStep(() -> 
		{
			service.sayHello("world")
				.then(hello -> fut.setResult(IComponentManager.get().getCurrentComponent().getId()))
				.catchEx(e -> fut.setException(e));
		});
		assertEquals(caller.getId(), fut.get(TIMEOUT));
		
		// Cleanup
		provider.terminate().get(TIMEOUT);
	}
}
