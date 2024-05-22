package jadex.micro.nfservicetags;

import java.util.Collection;

import jadex.core.IComponent;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;
import jadex.nfproperty.sensor.service.TagProperty;
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
@Agent
@Service
@RequiredServices({
	@RequiredService(name="testser1", type=ITestService.class),
	@RequiredService(name="testser2", type=ITestService.class, tags="$host"),
	@RequiredService(name="testser3", type=ITestService.class, tags="blatag")
})
//@ComponentTypes(@ComponentType(name="provider", filename="jadex.micro.testcases.nfservicetags.ProviderAgent.class"))
//@Configurations(@Configuration(name="default", components=@Component(type="provider")))
//@Results(@Result(name="testresults", description= "The test results.", clazz=Testcase.class))
public class NFServiceTagsTestAgent //extends JunitAgentTest
{
	/** The agent. */
	@Agent
	protected IComponent agent;
	
	/**
	 *  The agent body. 
	 */
	//@AgentBody
	@OnStart
	public void body()
	{
		//final List<TestReport> results = new ArrayList<TestReport>();
		
		//TestReport tr1 = new TestReport("#1", "Test if can find service withouts tags.");
		String tstname = "Test if can find service withouts tags.";
		try
		{
			IService ser = (IService)agent.getFeature(IRequiredServiceFeature.class).getService("testser1").get();
			System.out.println("Succeeded: "+tstname+" "+ser.getServiceId().getTags());
			//tr1.setSucceeded(true);
		}
		catch(Exception e)
		{
			System.out.println("Failed: "+tstname);
			//tr1.setReason("Exception occurred: "+e);
		}
		//results.add(tr1);
		
		//TestReport tr2 = new TestReport("#2", "Test if can find service with tags in required service defition.");
		tstname = "Test if can find service with tags in required service defition.";
		try
		{
			agent.getFeature(IRequiredServiceFeature.class).getService("testser2").get();
			System.out.println("Succeeded: "+tstname);
			//tr2.setSucceeded(true);
		}
		catch(Exception e)
		{
			System.out.println("Failed: "+tstname);
			//tr2.setReason("Exception occurred: "+e);
		}
		//results.add(tr2);
		
		//TestReport tr3 = new TestReport("#3", "Test if can find service with tags in required service defition that are not defined on service.");
		tstname = "Test if can find service with tags in required service defition that are not defined on service.";
		try
		{
			agent.getFeature(IRequiredServiceFeature.class).getService("testser3").get();
			//tr3.setReason("Found service that does not have the tag");
			System.out.println("Succeeded: "+tstname);
		}
		catch(Exception e)
		{
			System.out.println("Failed: "+tstname);
			//tr3.setSucceeded(true);
		}
		//results.add(tr3);
		
		//TestReport tr4 = new TestReport("#4", "Test if can find service via SServiceProvider.getServices()");
		tstname = "Test if can find service via getServices()";
		//try
		//{
			Collection<ITestService> sers = agent.getFeature(IRequiredServiceFeature.class).getLocalServices(new ServiceQuery<>(ITestService.class, ServiceScope.VM).setServiceTags(new String[]{"$host"}, agent));
			if(sers.isEmpty())
			{
				System.out.println("Succeeded: "+tstname);
				//tr4.setFailed("No service found");
			}
			else
			{
				System.out.println("Failed: "+tstname);
				//tr4.setSucceeded(true);
			}
		/*}
		catch(Exception e)
		{
			tr4.setReason("Exception occurred: "+e);
		}
		results.add(tr4);*/
		
		//TestReport tr5 = new TestReport("#5", "Test if can find service via SServiceProvider.getService()");
		tstname = "Test if can find service via getService()";
		try
		{
			agent.getFeature(IRequiredServiceFeature.class).getLocalService(new ServiceQuery<>(ITestService.class).setServiceTags(new String[]{"$host"}, agent)); 
			//tr5.setSucceeded(true);
			System.out.println("Succeeded: "+tstname);
		}
		catch(Exception e)
		{
			//tr5.setReason("Exception occurred: "+e);
			System.out.println("Failed: "+tstname);
		}
		//results.add(tr5);
		
		tstname = "Test if can find null tagged service service via getService()";
		//TestReport tr6 = new TestReport("#6", "Test if can find null tagged service service via SServiceProvider.getService()");
		try
		{
			agent.getFeature(IRequiredServiceFeature.class).getLocalService(new ServiceQuery<>(ITestService.class).setServiceTags(new String[]{null}, agent)); 
			System.out.println("Succeeded: "+tstname);
			//tr6.setSucceeded(true);
		}
		catch(Exception e)
		{
			System.out.println("Failed: "+tstname);
			//tr6.setReason("Exception occurred: "+e);
		}
		//results.add(tr6);
		
		/*agent.getFeature(IArgumentsResultsFeature.class).getResults().put("testresults", new Testcase(results.size(), 
			(TestReport[])results.toArray(new TestReport[results.size()])));
		agent.killComponent();*/
	}
	
	public static void main(String[] args) 
	{
		IComponent.create(new ProviderAgent("$host", null)).get();
		IComponent.create(new NFServiceTagsTestAgent()).get();
		IComponent.waitForLastComponentTerminated();
	}
}

