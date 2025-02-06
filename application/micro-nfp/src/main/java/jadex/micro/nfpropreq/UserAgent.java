package jadex.micro.nfpropreq;

import java.util.Collection;

import jadex.common.MethodInfo;
import jadex.common.Tuple2;
import jadex.core.IComponent;
import jadex.future.ITerminableIntermediateFuture;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;
import jadex.nfproperty.INFPropertyFeature;
import jadex.nfproperty.impl.search.ComposedEvaluator;
import jadex.nfproperty.sensor.service.ExecutionTimeEvaluator;
import jadex.providedservice.annotation.Service;
import jadex.requiredservice.IRequiredServiceFeature;

// todo!

@Agent
@Service
//@RequiredServices(@RequiredService(name="aser", type=IAService.class, scope=ServiceScope.VM, //  multiple=true,
//	nfprops=@NFRProperty(value=ExecutionTimeProperty.class, methodname="test")))
public class UserAgent
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
		// todo: make ITerminable in DefaultServiceFetcher
		
		try
		{
			while(true)
			{
				ComposedEvaluator<IAService> ranker = new ComposedEvaluator<IAService>();
				ranker.addEvaluator(new ExecutionTimeEvaluator(agent.getComponentHandle(), new MethodInfo(IAService.class.getMethod("test", new Class[0])), true));
				ITerminableIntermediateFuture<IAService> sfut = agent.getFeature(IRequiredServiceFeature.class).getServices("aser");
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
