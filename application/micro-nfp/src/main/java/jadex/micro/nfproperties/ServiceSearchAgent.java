package jadex.micro.nfproperties;

import java.util.Arrays;
import java.util.Collection;

import jadex.common.Tuple2;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.IResultListener;
import jadex.future.ITerminableIntermediateFuture;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;
import jadex.nfproperty.annotation.NFProperties;
import jadex.nfproperty.annotation.NFProperty;
import jadex.nfproperty.impl.search.BasicEvaluator;
import jadex.nfproperty.impl.search.ComposedEvaluator;
import jadex.nfproperty.impl.search.CountThresholdSearchTerminationDecider;
import jadex.nfproperty.sensor.unit.MemoryUnit;
import jadex.providedservice.ServiceScope;
import jadex.providedservice.annotation.Implementation;
import jadex.providedservice.annotation.ProvidedService;
import jadex.providedservice.annotation.ProvidedServices;
import jadex.providedservice.annotation.Service;
import jadex.providedservice.impl.search.SServiceProvider;
import jadex.providedservice.impl.search.ServiceQuery;
import jadex.requiredservice.IRequiredServiceFeature;

@Agent

@Service
@ProvidedServices(@ProvidedService(type=ICoreDependentService.class, implementation=@Implementation(NFPropertyTestService.class)))
@NFProperties({@NFProperty(FakeCpuLoadProperty.class),
			   @NFProperty(FakeFreeMemoryProperty.class),
			   @NFProperty(FakeNetworkBandwidthProperty.class),
			   @NFProperty(FakeReliabilityProperty.class)
})
public class ServiceSearchAgent
{
	protected static final int SEARCH_DELAY = 1000;
	
	@Agent
	protected IComponent agent;

	/**
	 *  Body.
	 */
	@OnStart
	public IFuture<Void> body()
	{
		final Future<Void> done = new Future<Void>();
		final ComposedEvaluator ce = new ComposedEvaluator();
		ce.addEvaluator(new BasicEvaluator<Double>(agent.getExternalAccess(), "fakecpuload")
		{
			public double calculateEvaluation(Double propertyvalue)
			{
				return (100.0 - propertyvalue) * 0.01;
			}
		});
		
		ce.addEvaluator(new BasicEvaluator<Double>(agent.getExternalAccess(), "fakereliability")
		{
			public double calculateEvaluation(Double propertyvalue)
			{
				return propertyvalue * 0.01;
			}
		});
		
		ce.addEvaluator(new BasicEvaluator<Long>(agent.getExternalAccess(), "fakefreemem", MemoryUnit.MB)
		{
			public double calculateEvaluation(Long propertyvalue)
			{
				return Math.min(4096.0, propertyvalue) / 4096.0;
			}
		});
		
		ce.addEvaluator(new BasicEvaluator<Long>(agent.getExternalAccess(), "fakenetworkbandwith", MemoryUnit.MB)
		{
			public double calculateEvaluation(Long propertyvalue)
			{
				return Math.min(100.0, propertyvalue) / 100.0;
			}
		});
		
//		BasicEvaluatorConstraints cts = new BasicEvaluatorConstraints(null, evaluator, evaluationsize)
//		SServiceProvider.getServices(agent.getServiceProvider(), ICoreDependentService.class, ServiceScope.PLATFORM, new Basic)
		
		agent.getFeature(IExecutionFeature.class).waitForDelay(SEARCH_DELAY).get();
//				SServiceProvider.getServices(agent.getServiceProvider(), ICoreDependentService.class, ServiceScope.PLATFORM)
//					.addResultListener(new ServiceRankingResultListener<ICoreDependentService>(ce, new CountThresholdSearchTerminationDecider<ICoreDependentService>(10), 
//					new IResultListener<Collection<ICoreDependentService>>()
//				{
//					public void resultAvailable(Collection<ICoreDependentService> result)
//					{
//						System.out.println(Arrays.toString(((List<ICoreDependentService>) result).toArray()));
//						agent.getComponentFeature(IExecutionFeature.class).scheduleStep(step, SEARCH_DELAY);
//					}
//
//					public void exceptionOccurred(Exception exception)
//					{
//						exception.printStackTrace();
//					}
//				}));
		
//				SServiceProvider.getServices(agent.getServiceProvider(), ICoreDependentService.class, ServiceScope.PLATFORM)
//					.addResultListener(new ServiceRankingResultListener<ICoreDependentService>(new IResultListener<Collection<Tuple2<ICoreDependentService, Double>>>()
//				{
//					public void resultAvailable(Collection<Tuple2<ICoreDependentService, Double>> result)
//					{
//						System.out.println(Arrays.toString(((List<Tuple2<ICoreDependentService, Double>>)result).toArray()));
//						agent.getComponentFeature(IExecutionFeature.class).scheduleStep(step, SEARCH_DELAY);
//					}
//	
//					public void exceptionOccurred(Exception exception)
//					{
//						exception.printStackTrace();
//					}
//				}, ce, new CountThresholdSearchTerminationDecider<ICoreDependentService>(10))); 
		
//				ITerminableIntermediateFuture<ICoreDependentService> fut = SServiceProvider.getServices(agent.getServiceProvider(), ICoreDependentService.class, ServiceScope.PLATFORM);
//				ITerminableIntermediateFuture<ICoreDependentService> res = SServiceProvider.rankServices(fut, ce, new CountThresholdSearchTerminationDecider<ICoreDependentService>(10));
//				res.addResultListener(new IResultListener<Collection<ICoreDependentService>>()
//				{
//					public void resultAvailable(Collection<ICoreDependentService> result)
//					{
//						System.out.println(Arrays.toString(((List<ICoreDependentService>)result).toArray()));
//						agent.getComponentFeature(IExecutionFeature.class).scheduleStep(step, SEARCH_DELAY);
//					}
//	
//					public void exceptionOccurred(Exception exception)
//					{
//						exception.printStackTrace();
//					}
//				}); 
		
		/*ITerminableIntermediateFuture<ICoreDependentService> fut = agent.getFeature(IRequiredServiceFeature.class).searchServices(new ServiceQuery<>(ICoreDependentService.class));
		ITerminableIntermediateFuture<Tuple2<ICoreDependentService, Double>> res = SServiceProvider.rankServicesWithScores(fut, ce, new CountThresholdSearchTerminationDecider<ICoreDependentService>(10));
		res.addResultListener(new IResultListener<Collection<Tuple2<ICoreDependentService, Double>>>()
		{
			public void resultAvailable(Collection<Tuple2<ICoreDependentService, Double>> result)
			{
				System.out.println(Arrays.toString(result.toArray()));
				agent.getFeature(IExecutionFeature.class).waitForDelay(SEARCH_DELAY, step);
			}

			public void exceptionOccurred(Exception exception)
			{
				exception.printStackTrace();
			}
		});*/
				
		
		return done;
	}
}
