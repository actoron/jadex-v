package jadex.injection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.future.IFuture;
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
}
