package jadex.requiredservicemicro.tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collection;

import org.junit.jupiter.api.Test;

import jadex.core.IComponentManager;
import jadex.core.IExternalAccess;
import jadex.micro.annotation.Agent;
import jadex.providedservice.IService;
import jadex.providedservice.ServiceScope;
import jadex.providedservice.annotation.Service;
import jadex.providedservice.impl.search.ServiceQuery;
import jadex.requiredservice.IRequiredServiceFeature;
import jadex.requiredservice.annotation.RequiredService;
import jadex.requiredservice.annotation.RequiredServices;

/**
 *  Test searching for tagged services.
 */
public class RequiredServiceTagsTest
{
	// Junit requires to have exactly ONE constructor. If a con with parameter is used
	// one has to provide a parameter resolver :-(
	@Agent
	@Service
	@RequiredServices({
		@RequiredService(name="testser1", type=ITestService.class),
		@RequiredService(name="testser2", type=ITestService.class, tags="$host"),
		@RequiredService(name="testser3", type=ITestService.class, tags="blatag")
	})
	public class TestAgent 
	{
	}
	
	@Test
	public void findServiceWithoutTag()
	{
		IComponentManager.get().create(new ProviderAgent()).get();
		IExternalAccess exta = IComponentManager.get().create(new TestAgent()).get();
	
		IService res = exta.scheduleStep(agent ->
		{
			IService ser = (IService)agent.getFeature(IRequiredServiceFeature.class).getService("testser1").get();
			
			agent.terminate().get();
			
			return ser;
		}).get();
		
		assertNotNull(res, "Should should be found");
	}
	
	@Test
	public void findServiceWithTag()
	{
		IComponentManager.get().create(new ProviderAgent()).get();
		IExternalAccess exta = IComponentManager.get().create(new TestAgent()).get();
	
		IService res = exta.scheduleStep(agent ->
		{
			IService ser = (IService)agent.getFeature(IRequiredServiceFeature.class).getService("testser2").get();
			
			agent.terminate().get();
			
			return ser;
		}).get();
		
		assertNotNull(res, "Should should be found");
	}
	
	@Test
	public void notFindServiceWithWrongTag()
	{
		IComponentManager.get().create(new ProviderAgent()).get();
		IExternalAccess exta = IComponentManager.get().create(new TestAgent()).get();
	
		IService res = exta.scheduleStep(agent ->
		{
			IService ser = null;
			
			try
			{
				ser = (IService)agent.getFeature(IRequiredServiceFeature.class).getService("testser3").get();
			}
			catch(Exception e)
			{
				
			}
			
			agent.terminate().get();
			
			return ser;
		}).get();
		
		assertEquals(null, res, "Should not be found");
	}
	
	@Test
	public void notFindWithSetServiceTags()
	{
		IComponentManager.get().create(new ProviderAgent()).get();
		IExternalAccess exta = IComponentManager.get().create(new TestAgent()).get();
	
		Collection<ITestService> res = exta.scheduleStep(agent ->
		{
			Collection<ITestService> sers = agent.getFeature(IRequiredServiceFeature.class).getLocalServices(new ServiceQuery<>(ITestService.class, ServiceScope.VM).setServiceTags(new String[]{"horst"}, agent));
			
			agent.terminate().get();
			
			return sers;
		}).get();
		
		assertEquals(0, res.size(), "Should not be found");
	}
	
	@Test
	public void findWithSetServiceTags()
	{
		IComponentManager.get().create(new ProviderAgent()).get();
		IExternalAccess exta = IComponentManager.get().create(new TestAgent()).get();
	
		ITestService res = exta.scheduleStep(agent ->
		{
			ITestService ser = agent.getFeature(IRequiredServiceFeature.class).getLocalService(new ServiceQuery<>(ITestService.class).setServiceTags(new String[]{"$host"}, agent)); 
			
			agent.terminate().get();
			
			return ser;
		}).get();
		
		assertNotNull(res, "Should be found");
	}
	
	@Test
	public void findWithSetServiceTagNull()
	{
		IComponentManager.get().create(new ProviderAgent()).get();
		IExternalAccess exta = IComponentManager.get().create(new TestAgent()).get();
	
		ITestService res = exta.scheduleStep(agent ->
		{
			ITestService ser = agent.getFeature(IRequiredServiceFeature.class).getLocalService(new ServiceQuery<>(ITestService.class).setServiceTags(new String[]{"null"}, agent)); 
			
			agent.terminate().get();
			
			return ser;
		}).get();
		
		assertNotNull(res, "Should be found");
	}
		
	/*public static void main(String[] args) 
	{
		IComponentManager.get().create(new ProviderAgent()).get();
		IComponentManager.get().create(new RequiredServiceTagsTest()).get();
		IComponentManager.get().waitForLastComponentTerminated();
	}*/
}

