package jadex.micro.nfpropreq;

import java.util.Collection;

import jadex.common.MethodInfo;
import jadex.common.Tuple2;
import jadex.core.IComponent;
import jadex.future.ITerminableIntermediateFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;
import jadex.nfproperty.INFPropertyFeature;
import jadex.nfproperty.impl.search.ComposedEvaluator;
import jadex.nfproperty.sensor.service.ExecutionTimeEvaluator;
import jadex.requiredservice.IRequiredServiceFeature;

// todo!

//@RequiredServices(@RequiredService(name="aser", type=IAService.class, scope=ServiceScope.VM, //  multiple=true,
//	nfprops=@NFRProperty(value=ExecutionTimeProperty.class, methodname="test")))
public class UserAgent
{
	@Inject
	protected IComponent agent;
		
	/**
	 *  The agent body.
	 */
	//@AgentBody
	@OnStart
	public void body()
	{
		// todo: make ITerminable in DefaultServiceFetcher
		
		try
		{
			while(true)
			{
				ComposedEvaluator<IAService> ranker = new ComposedEvaluator<IAService>();
				ranker.addEvaluator(new ExecutionTimeEvaluator(agent.getComponentHandle(), new MethodInfo(IAService.class.getMethod("test", new Class[0])), true));
				ITerminableIntermediateFuture<IAService> sfut = agent.getFeature(IRequiredServiceFeature.class).searchServices(IAService.class);
				Collection<Tuple2<IAService, Double>> res = agent.getFeature(INFPropertyFeature.class).rankServicesWithScores(sfut, ranker, null).get();
				System.out.println("Found: "+res);
				IAService aser = res.iterator().next().getFirstEntity();
				aser.test().get();
			}
		}
		catch(Exception e)
		{
			System.out.println("User agent problem: "+e);
		}
	}
	
}
