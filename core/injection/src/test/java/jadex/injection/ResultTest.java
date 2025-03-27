package jadex.injection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jadex.common.NameValue;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.injection.annotation.OnEnd;
import jadex.injection.annotation.OnStart;
import jadex.injection.annotation.ProvideResult;

/**
 *  Test handling of results in injection feature.
 */
public class ResultTest
{
	public static final long	TIMEOUT	= 10000;
	
	/**
	 *  Test manually adding result.
	 */
	@Test
	public void  testManualResult()
	{
		IFuture<String>	fut	= IComponentManager.get().run(new Object()
		{
			@OnStart
			void start(IInjectionFeature feature)
			{
				feature.addResult("result", "success");
			}
		});
		assertEquals("success", fut.get(TIMEOUT));
	}
	
	/**
	 *  Test fetching result from annotated field
	 */
	@Test
	public void  testFieldAnnotation()
	{
		IFuture<String>	fut	= IComponentManager.get().run(new Object()
		{
			@ProvideResult
			String	result	= "success";
			
			@OnStart
			void start(IComponent comp)
			{
				comp.terminate();
			}
		});
		assertEquals("success", fut.get(TIMEOUT));
	}
	
	/**
	 *  Test fetching result from annotated method
	 */
	@Test
	public void  testMethodAnnotation()
	{
		IFuture<String>	fut	= IComponentManager.get().run(new Object()
		{
			@ProvideResult
			String	result()
			{
				return "success";
			}
			
			@OnStart
			void start(IComponent comp)
			{
				comp.terminate();
			}
		});
		assertEquals("success", fut.get(TIMEOUT));
	}

	/**
	 *  Test getting results.
	 */
	@Test
	public void	testGetResults()
	{
		boolean[]	terminated	= new boolean[1];
		IComponentHandle	handle	= IComponentManager.get().create(new Object()
		{
			@ProvideResult
			String	field	= "fieldvalue";
			
			@ProvideResult
			String	method(IComponent comp)
			{
				// Test that getResult() is scheduled or not
				if(!terminated[0])
				{
					assertEquals(comp, IComponentManager.get().getCurrentComponent());
				}
				else
				{
					assertNull(IComponentManager.get().getCurrentComponent());
				}
				
				return "methodvalue";
			}
		}).get(TIMEOUT);
		
		@SuppressWarnings("serial")
		Map<String, Object>	expected	= new LinkedHashMap<>()
		{
			{
				this.put("field", "fieldvalue");
				this.put("method", "methodvalue");
			}
		};
		assertEquals(expected, handle.getResults().get(TIMEOUT));
		
		// Test get results after termination
		handle.terminate().get(TIMEOUT);
		terminated[0]	= true;
		assertEquals(expected, handle.getResults().get(TIMEOUT));
	}


	/**
	 *  Test result subscription.
	 */
	@Test
	public void	testSubscribeToResults()
	{
		IComponentHandle	handle	= IComponentManager.get().create(new Object()
		{
			@OnStart
			void start(IInjectionFeature feature)
			{
				feature.addResult("start", "startvalue");
			}
			
			@OnEnd
			void end(IInjectionFeature feature)
			{
				feature.addResult("end", "endvalue");
			}
		}).get(TIMEOUT);
		
		ISubscriptionIntermediateFuture<NameValue>	sub	= handle.subscribeToResults();
		
		NameValue	res	= sub.getNextIntermediateResult(TIMEOUT);
		assertEquals(new NameValue("start", "startvalue"), res);
		
		handle.terminate().get(TIMEOUT);
		
		res	= sub.getNextIntermediateResult(TIMEOUT);
		assertEquals(new NameValue("end", "endvalue"), res);
	}
}
