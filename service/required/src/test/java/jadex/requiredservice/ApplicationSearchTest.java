package jadex.requiredservice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;

import org.junit.jupiter.api.Test;

import jadex.core.Application;
import jadex.core.IComponentManager;
import jadex.core.INoCopyStep;
import jadex.providedservice.IService;
import jadex.providedservice.ServiceQuery;
import jadex.providedservice.annotation.Service;

/**
 *  Test searching for services in the same application.
 */
public class ApplicationSearchTest
{
	@Service
	interface ITestService {}

	@Test
	public void testApplicationSearch()
	{		
		Application app = new Application("testapp");
		Application app2 = new Application("testapp2");

		try
		{
			// Create two services in two apps.
			app.create(new ITestService(){}, "testservice").get();
			app2.create(new ITestService(){}, "testservice2").get();
			
			// Create a service not part of any app.
			IComponentManager.get().create(new ITestService(){}, "testservice3").get();
			
			// Search for services in each app.
			Collection<ITestService>	services	= app.run((INoCopyStep<Collection<ITestService>>)
				comp -> comp.getFeature(IRequiredServiceFeature.class).searchServices(ITestService.class).get()).get();
			Collection<ITestService>	services2	= app2.run((INoCopyStep<Collection<ITestService>>)
					comp -> comp.getFeature(IRequiredServiceFeature.class).searchServices(ITestService.class).get()).get();
			
			// Search for also non-app services in one app
			Collection<ITestService>	all_services	= app.run((INoCopyStep<Collection<ITestService>>)
					comp -> comp.getFeature(IRequiredServiceFeature.class).searchServices(
						new ServiceQuery<>(ITestService.class).setAppId(null)).get()).get();
			
			
			// Check that each app only sees its own service.
			assertEquals(1, services.size());
			assertEquals(1, services2.size());
			assertEquals(app.getId(), ((IService) services.iterator().next()).getServiceId().getProviderId().getAppId());
			assertEquals(app2.getId(), ((IService) services2.iterator().next()).getServiceId().getProviderId().getAppId());
			
			// Check that searching with null appId returns all three services.
			assertEquals(3, all_services.size());
		}
		finally
		{
			app.terminate();
			app2.terminate();
			app.waitForLastComponentTerminated();
			app2.waitForLastComponentTerminated();
		}
	}
}
