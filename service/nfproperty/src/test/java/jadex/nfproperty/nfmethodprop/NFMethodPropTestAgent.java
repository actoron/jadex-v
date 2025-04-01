package jadex.nfproperty.nfmethodprop;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import jadex.common.MethodInfo;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.injection.annotation.Inject;
import jadex.nfproperty.INFPropertyFeature;
import jadex.nfproperty.sensor.service.ExecutionTimeProperty;
import jadex.providedservice.IService;
import jadex.requiredservice.IRequiredServiceFeature;

/**
 *  Tests waitqueue and execution time non-functional properties on 
 *  provided services.
 */
public class NFMethodPropTestAgent 
{
	/** The agent. */
	@Inject
	protected IComponent agent;
	
	@Test
	public void waitTimes()
	{
		IComponentManager.get().create(new ProviderAgent()).get();
		
		IComponentHandle exta = IComponentManager.get().create(new NFMethodPropTestAgent()).get();
	
		double[] res = exta.scheduleStep(agent ->
		{
			double[] ret = new double[3];
			ITestService ser = (ITestService)agent.getFeature(IRequiredServiceFeature.class).searchService(ITestService.class).get();
			
			//final List<TestReport> results = new ArrayList<TestReport>();
			final long wa = 250;
			final long wb = wa*2;
			
			for(int i=0; i<5; i++)
			{
				ser.methodA(wa).get();
				ser.methodB(wb).get();
			}
			
			Method ma = ser.getClass().getMethod("methodA", new Class[]{long.class});
			double w = ((Long)agent.getFeature(INFPropertyFeature.class).getMethodNFPropertyValue(((IService)ser).getServiceId(), new MethodInfo(ma), ExecutionTimeProperty.NAME).get()).doubleValue();
			ret[0] = Math.abs(w-wa)/wa;
			
			Method mb = ser.getClass().getMethod("methodB", new Class[]{long.class});
			w = ((Long)agent.getFeature(INFPropertyFeature.class).getMethodNFPropertyValue(((IService)ser).getServiceId(), new MethodInfo(mb), ExecutionTimeProperty.NAME).get()).doubleValue();
			ret[1] = Math.abs(w-wb)/wb;
			
			w = ((Long)agent.getFeature(INFPropertyFeature.class).getNFPropertyValue(((IService)ser).getServiceId(), ExecutionTimeProperty.NAME).get()).doubleValue();
			long wab = (wa+wb)/2;
			ret[2] = Math.abs(w-wab)/wab;

			agent.terminate().get();
			
			return ret;
		}).get();
		
		assertTrue(res[0]<0.15, "methodA waiting time should be in tolerance: "+res[0]);
		assertTrue(res[1]<0.15, "methodB waiting time should be in tolerance: "+res[1]);
		assertTrue(res[2]<0.15, "methodAB waiting time should be in tolerance: "+res[2]);
	}
	
	/*public static void main(String[] args) 
	{
		IExternalAccess prov = IComponentManager.get().create(new ProviderAgent()).get();
		IExternalAccess tester = IComponentManager.get().create(new NFMethodPropTestAgent()).get();
		IComponentManager.get().waitForLastComponentTerminated();
	}*/
}

