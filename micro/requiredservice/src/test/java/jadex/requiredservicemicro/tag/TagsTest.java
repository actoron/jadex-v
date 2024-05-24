package jadex.requiredservicemicro.tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import org.junit.jupiter.api.Test;

import jadex.core.IComponent;
import jadex.core.IExternalAccess;
import jadex.micro.annotation.Agent;
import jadex.providedservice.IProvidedServiceFeature;
import jadex.providedservice.IService;
import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.annotation.Service;

public class TagsTest 
{
	// Junit requires to have exactly ONE constructor. If a con with parameter is used
	// one has to provide a parameter resolver :-(
	@Agent
	@Service
	public class TagsAgent implements ITestService2, ITestService3 //extends JunitAgentTest 
	{
		/** The agent. */
		@Agent
		protected IComponent agent;
		
		// must be public if accessed via $args.testarg
		public int testarg;
		
		public TagsAgent(int testarg) 
		{
			this.testarg = testarg;
		}
		
		public int getTestarg() 
		{
			return testarg;
		}
	}

	@Test
	public void testHasTags()
	{
		IExternalAccess exta = IComponent.create(new TagsAgent(4711)).get();
	
		Collection<String> res = exta.scheduleStep(agent ->
		{
			IService ser = (IService)agent.getFeature(IProvidedServiceFeature.class).getProvidedService(ITestService2.class);
			IServiceIdentifier sid = ser.getServiceId();
			Collection<String> vals = sid.getTags();
			System.out.println("ITestService2: "+vals);
			
			agent.terminate().get();
			
			return vals;
		}).get();
		
		assertEquals(4, res.size(), "Should have 4 tags");
	}
	
	@Test
	public void testArgumentTags()
	{
		IExternalAccess exta = IComponent.create(new TagsAgent(4711)).get();
	
		Collection<String> res = exta.scheduleStep(agent ->
		{
			IService ser = (IService)agent.getFeature(IProvidedServiceFeature.class).getProvidedService(ITestService3.class);
			IServiceIdentifier sid = ser.getServiceId();
			Collection<String> vals = sid.getTags();
			System.out.println("ITestService3: "+vals);
		
			agent.terminate().get();
			
			return vals;
		}).get();
		
		assertEquals(2, res.size());
		assertTrue(res.stream().allMatch(v -> "4711".equals(v)), "All tags should be 4711");
	}
	
	/*public static void main(String[] args) 
	{
		IComponent.create(new TagsAgent(4711)).get();
		IComponent.waitForLastComponentTerminated();
	}*/
	
}