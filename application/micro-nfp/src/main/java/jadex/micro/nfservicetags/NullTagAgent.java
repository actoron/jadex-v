package jadex.micro.nfservicetags;

import java.util.Collection;

import jadex.core.IComponent;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Argument;
import jadex.micro.annotation.Arguments;
import jadex.model.annotation.OnStart;
import jadex.nfproperty.INFPropertyFeature;
import jadex.nfproperty.annotation.NFProperties;
import jadex.nfproperty.annotation.NFProperty;
import jadex.nfproperty.sensor.service.TagProperty;

/**
 *  Test an agent with a tag that resolves to null.
 */
@Agent
@Arguments(@Argument(name = TagProperty.NAME, clazz = String.class, defaultvalue="java.util.Arrays.asList(new Object[]{null})"))
@NFProperties(@NFProperty(value = TagProperty.class))
//@Results(@Result(name="testresults", description= "The test results.", clazz=Testcase.class))
public class NullTagAgent //extends JunitAgentTest
{
	@Agent
	protected IComponent agent;
	
	/**
	 *  The agent body. 
	 */
	//@AgentBody
	@OnStart
	public void body()
	{
//		final List<TestReport> results = new ArrayList<TestReport>();
		
//		TestReport tr1 = new TestReport("#1", "Test if tag null.");
//		try
//		{
//			Object tagval = SNFPropertyProvider.getNFPropertyValue(agent.getExternalAccess(), TagProperty.NAME).get();
			Object tagval = agent.getFeature(INFPropertyFeature.class).getNFPropertyValue(TagProperty.NAME).get();
			if(tagval instanceof Collection && ((Collection)tagval).size()==1 && ((Collection)tagval).iterator().next()==null)
			{
				System.out.println("Succeeded: Test if tag null.");
				//tr1.setSucceeded(true);
			}
			else
			{
				System.out.println("Failed: Test if tag null.");
				//tr1.setFailed("Tag value was not null: "+tagval);
			}
//		}
//		catch(Exception e)
//		{
//			tr1.setReason("Exception occurred: "+e);
//		}
//		results.add(tr1);
		
//		agent.getFeature(IArgumentsResultsFeature.class).getResults().put("testresults", new Testcase(results.size(), 
//			(TestReport[])results.toArray(new TestReport[results.size()])));
//		agent.killComponent();
	}
}
