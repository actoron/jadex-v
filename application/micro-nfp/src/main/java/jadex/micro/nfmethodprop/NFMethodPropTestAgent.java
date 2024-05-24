package jadex.micro.nfmethodprop;

import java.lang.reflect.Method;

import jadex.common.MethodInfo;
import jadex.core.IComponent;
import jadex.core.IExternalAccess;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;
import jadex.nfproperty.INFPropertyFeature;
import jadex.nfproperty.sensor.service.ExecutionTimeProperty;
import jadex.providedservice.IService;
import jadex.providedservice.annotation.Service;
import jadex.requiredservice.IRequiredServiceFeature;

/**
 *  Tests waitqueue and execution time non-functional properties on 
 *  provided services.
 */
@Agent
@Service
//@RequiredServices(@RequiredService(name="testser", type=ITestService.class))
//@ComponentTypes(@ComponentType(name="provider", filename="jadex.micro.testcases.nfmethodprop.ProviderAgent.class"))
//@Configurations(@Configuration(name="default", components=@Component(type="provider")))
//@Results(@Result(name="testresults", description= "The test results.", clazz=Testcase.class))
public class NFMethodPropTestAgent //extends JunitAgentTest
{
	/** The agent. */
	@Agent
	protected IComponent agent;
	
	/**
	 *  The agent body. 
	 */
	@OnStart
	public void body()
	{		
		ITestService ser = (ITestService)agent.getFeature(IRequiredServiceFeature.class).getService(ITestService.class).get();
		
		//final List<TestReport> results = new ArrayList<TestReport>();
		final long wa = 250;
		final long wb = wa*2;
		
		for(int i=0; i<5; i++)
		{
			ser.methodA(wa).get();
			ser.methodB(wb).get();
		}
		
		try
		{
			//TestReport tr1 = new TestReport("#1", "Test if wait time of method a is ok");
			//results.add(tr1);
			Method ma = ser.getClass().getMethod("methodA", new Class[]{long.class});
//			INFMixedPropertyProvider prov = (INFMixedPropertyProvider)((IService)ser).getExternalComponentFeature(INFPropertyComponentFeature.class);
//			double w = ((Long)SNFPropertyProvider.getMethodNFPropertyValue(agent.getExternalAccess(), ((IService)ser).getId(), new MethodInfo(ma), ExecutionTimeProperty.NAME).get()).doubleValue();
			double w = ((Long)agent.getFeature(INFPropertyFeature.class).getMethodNFPropertyValue(((IService)ser).getServiceId(), new MethodInfo(ma), ExecutionTimeProperty.NAME).get()).doubleValue();
//			double w = ((Long)((IService)ser).getMethodNFPropertyValue(new MethodInfo(ma), ExecutionTimeProperty.NAME).get()).doubleValue();
			double d = Math.abs(w-wa)/wa;
			if(d<0.15)
			{
				//tr1.setSucceeded(true);
				System.out.println("methodA ok: "+d+" "+w+" "+wa);
			}
			else
			{
				System.out.println("methodA value differs more than 15 percent:"+d+" "+w+" "+wa);
				//tr1.setReason("Value differs more than 15 percent: "+d+" "+w+" "+wa);
			}
			
			//TestReport tr2 = new TestReport("#2", "Test if wait time of method b is ok");
			//results.add(tr2);
			Method mb = ser.getClass().getMethod("methodB", new Class[]{long.class});
//			w = ((Long)SNFPropertyProvider.getMethodNFPropertyValue(agent.getExternalAccess(), ((IService)ser).getId(), new MethodInfo(mb), ExecutionTimeProperty.NAME).get()).doubleValue();
			w = ((Long)agent.getFeature(INFPropertyFeature.class).getMethodNFPropertyValue(((IService)ser).getServiceId(), new MethodInfo(mb), ExecutionTimeProperty.NAME).get()).doubleValue();
//			w = ((Long)((IService)ser).getMethodNFPropertyValue(new MethodInfo(mb), ExecutionTimeProperty.NAME).get()).doubleValue();
			d = Math.abs(w-wb)/wb;
			if(d<0.15)
			{
				//tr2.setSucceeded(true);
				System.out.println("methodA ok: "+d+" "+w+" "+wa);
			}
			else
			{
				System.out.println("methodB value differs more than 15 percent:"+d+" "+w+" "+wa);
				//tr2.setReason("Value differs more than 15 percent: "+d+" "+w+" "+wb);
			}
			
			//TestReport tr3 = new TestReport("#3", "Test if wait time of service is ok");
			//results.add(tr3);
//			w = ((Long)((IService)ser).getNFPropertyValue(ExecutionTimeProperty.NAME).get()).doubleValue();
			w = ((Long)agent.getFeature(INFPropertyFeature.class).getNFPropertyValue(((IService)ser).getServiceId(), ExecutionTimeProperty.NAME).get()).doubleValue();
//			w = ((Long)SNFPropertyProvider.getNFPropertyValue(agent.getExternalAccess(), ((IService)ser).getId(), ExecutionTimeProperty.NAME).get()).doubleValue();
			long wab = (wa+wb)/2;
			d = Math.abs(w-wab)/wab;
			if(d<0.15)
			{
				System.out.println("methodAB ok"+d+" "+w+" "+wab);
				//tr3.setSucceeded(true);
			}
			else
			{
				System.out.println("methodAB value differs more than 15 percent:"+d+" "+w+" "+wab);
				//tr3.setReason("Value differs more than 15 percent: "+d+" "+w+" "+wab);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		//agent.getFeature(IArgumentsResultsFeature.class).getResults().put("testresults", new Testcase(results.size(), 
		//	(TestReport[])results.toArray(new TestReport[results.size()])));
		
		IComponent.terminate(((IService)ser).getServiceId().getProviderId());
		agent.terminate();
	}
	
	public static void main(String[] args) 
	{
		IExternalAccess prov = IComponent.create(new ProviderAgent()).get();
		IExternalAccess tester = IComponent.create(new NFMethodPropTestAgent()).get();
		IComponent.waitForLastComponentTerminated();
	}
}

