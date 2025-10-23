package jadex.injection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.ChangeEvent;
import jadex.core.ChangeEvent.Type;
import jadex.core.annotation.NoCopy;
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
	public static final long	TIMEOUT	= -1;
	
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
				feature.setResult("result", "success");
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
			
			@ProvideResult("thefield")
			String	field2	= "fieldvalue2";
			
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

			@ProvideResult("themethod")
			String	method2()
			{
				return "methodvalue2";
			}
			
			// Test magic name replacement (getXxx -> xxx)
			@ProvideResult()
			String	getMyMethod()
			{
				return "methodvalue3";
			}
		}).get(TIMEOUT);
		
		Map<String, Object>	expected	= new LinkedHashMap<>()
		{
			{
				this.put("field", "fieldvalue");
				this.put("thefield", "fieldvalue2");
				this.put("method", "methodvalue");
				this.put("themethod", "methodvalue2");
				this.put("mymethod", "methodvalue3");
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
				feature.setResult("start", "startvalue");
			}
			
			@OnEnd
			void end(IInjectionFeature feature)
			{
				feature.setResult("end", "endvalue");
			}
		}).get(TIMEOUT);
		
		ISubscriptionIntermediateFuture<ChangeEvent>	sub	= handle.subscribeToResults();
		
		// Subscribe is executed after OnStart and thus gives event type INITIAL
		ChangeEvent	res	= sub.getNextIntermediateResult(TIMEOUT);
		assertEquals(new ChangeEvent(Type.INITIAL, "start", "startvalue", null, null), res);
//		assertEquals(new ResultEvent("start", "startvalue"), res);
		
		handle.terminate().get(TIMEOUT);
		
		res	= sub.getNextIntermediateResult(TIMEOUT);
		assertEquals(new ChangeEvent(Type.CHANGED, "end", "endvalue", null, null), res);
	}
	
	/**
	 *  Test if results are copied.
	 */
	@Test
	public void	testCopyGetResults()
	{
		List<String>	value	= Collections.singletonList("hello");
		
		IComponentHandle	handle	= IComponentManager.get().create(new Object()
		{
			@ProvideResult
			List<String>	field	= value;
			
			@ProvideResult
			List<String>	method()
			{
				return value;
			}
		}).get(TIMEOUT);
		
		assertEquals(value, handle.getResults().get(TIMEOUT).get("field"));
		assertNotSame(value, handle.getResults().get(TIMEOUT).get("field"));
		assertEquals(value, handle.getResults().get(TIMEOUT).get("method"));
		assertNotSame(value, handle.getResults().get(TIMEOUT).get("method"));
	}

	/**
	 *  Test if annotated results are not copied.
	 */
	@Test
	public void	testNoCopyGetResults()
	{
		List<String>	value	= Collections.singletonList("hello");
		class NoCopyPojo implements Supplier<List<String>>
		{
			@ProvideResult
			@NoCopy
			List<String>	field	= value;

			// Method annotation
			@ProvideResult
			@NoCopy
			List<String>	method1()
			{
				return value;
			}

			// Return type annotation
			@ProvideResult
			protected	@NoCopy	List<String>	method2()
			{
				return value;
			}

			// Get access to internal field (wrapped by ListWrapper due to dynamic value)
			@Override
			public List<String> get()
			{
				return field;
			}
		}
		
		NoCopyPojo	pojo	= new NoCopyPojo();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TIMEOUT);
		
		assertEquals(value, handle.getResults().get(TIMEOUT).get("field"));
		assertSame(pojo.get(), handle.getResults().get(TIMEOUT).get("field"));
		assertEquals(value, handle.getResults().get(TIMEOUT).get("method1"));
		assertSame(value, handle.getResults().get(TIMEOUT).get("method1"));
		assertEquals(value, handle.getResults().get(TIMEOUT).get("method2"));
		assertSame(value, handle.getResults().get(TIMEOUT).get("method2"));
	}
}
