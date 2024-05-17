package jadex.micro.nfservicetags;

import java.util.List;

import jadex.core.IComponent;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;
import jadex.nfproperty.INFPropertyFeature;
import jadex.nfproperty.sensor.service.TagProperty;
import jadex.providedservice.IProvidedServiceFeature;
import jadex.providedservice.IService;
import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.annotation.Service;

@Agent
//@Arguments(@Argument(name="tagarg", clazz=Integer.class, defaultvalue="44"))
//@Results(@Result(name="testresults", description= "The test results.", clazz=Testcase.class))
@Service
public class TagsAgent implements ITestService2, ITestService3 //extends JunitAgentTest 
{
	/** The agent. */
	@Agent
	protected IComponent agent;
	
	// tools not in cp :-(
//	public TagsAgent() 
//    {
//		super();
//		getConfig().setGui(true);
//    }
	
	/**
	 *  The agent body. 
	 */
	@OnStart
	public void body()
	{
		//final List<TestReport> results = new ArrayList<TestReport>();
		
		//TestReport tr1 = new TestReport("#1", "Test if service has tags.");
//		try
//		{
			IService ser = (IService)agent.getFeature(IProvidedServiceFeature.class).getProvidedService(ITestService2.class);
			IServiceIdentifier sid = ser.getServiceId();
			Object val = agent.getFeature(INFPropertyFeature.class).getNFPropertyValue(sid, TagProperty.NAME).get();
//			System.out.println(val);
			
			if(val instanceof List && ((List)val).size()==4)
				System.out.println("Succeeded: Test if service has tags.");
				//tr1.setSucceeded(true);
			else
				System.out.println("Failed: Test if service has tags.");
				//tr1.setReason("Wrong tag values: "+val);
//		}
//		catch(Exception e)
//		{
//			tr1.setReason("Exception occurred: "+e);
//		}
//		results.add(tr1);
		
//		TestReport tr2 = new TestReport("#2", "Test if service has conditional tags.");
//		try
//		{
			ser = (IService)agent.getFeature(IProvidedServiceFeature.class).getProvidedService(ITestService3.class);
			sid = ser.getServiceId();
			val = agent.getFeature(INFPropertyFeature.class).getNFPropertyValue(sid, TagProperty.NAME).get();
//			System.out.println(val);
			
			if(val instanceof List && ((List)val).size()==1)
				System.out.println("Succeeded: Test if service has conditional tags.");
				//tr2.setSucceeded(true);
			else
				System.out.println("Failed: Test if service has conditional tags.");
				//tr2.setReason("Wrong tag values: "+val);
//		}
//		catch(Exception e)
//		{
//			tr2.setReason("Exception occurred: "+e);
//		}
//		results.add(tr2);
		
		// agent.getFeature(IArgumentsResultsFeature.class).getResults().put("testresults", new Testcase(results.size(), 
		//	(TestReport[])results.toArray(new TestReport[results.size()])));
		// agent.killComponent();
	}
	
}