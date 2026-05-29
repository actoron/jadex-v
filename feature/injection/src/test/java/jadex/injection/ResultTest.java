package jadex.injection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;

import jadex.core.ChangeEvent;
import jadex.core.ChangeEvent.Type;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.IThrowingConsumer;
import jadex.core.annotation.NoCopy;
import jadex.core.impl.ComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.injection.annotation.OnEnd;
import jadex.injection.annotation.OnStart;
import jadex.injection.annotation.ProvideResult;
import jadex.result.IResultFeature;

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
			void start(IResultFeature feature)
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
				// Test that getResult() is scheduled on component or global runner
				if(!terminated[0])
				{
					assertEquals(comp, IComponentManager.get().getCurrentComponent());
				}
				else
				{
					assertEquals(ComponentManager.get().getGlobalRunner(), IComponentManager.get().getCurrentComponent());
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
			void start(IResultFeature feature)
			{
				feature.setResult("start", "startvalue");
			}
			
			@OnEnd
			void end(IResultFeature feature)
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
		
		assertFalse(sub.hasNextIntermediateResult(TIMEOUT, true));
	}
	
	@Test
	public void	testSubscriptionTermination()
	{
		IComponentHandle	handle	= IComponentManager.get().create(new Object()).get(TIMEOUT);
		ISubscriptionIntermediateFuture<ChangeEvent>	sub	= handle.subscribeToResults();
		
		// Test that subscription works.
		handle.scheduleStep((IThrowingConsumer<IComponent>)comp
			-> comp.getFeature(IResultFeature.class).setResult("result", "value")).get(TIMEOUT);
		ChangeEvent	res	= sub.getNextIntermediateResult(TIMEOUT);
		assertEquals(new ChangeEvent(Type.CHANGED, "result", "value", null, null), res);
		
		// Test that no more results are published after termination.
		sub.terminate(new Exception("Test"));
		handle.scheduleStep(() -> null).get(TIMEOUT);	// Schedule step to ensure termination is processed.
		handle.scheduleStep((IThrowingConsumer<IComponent>)comp
			-> comp.getFeature(IResultFeature.class).setResult("result", "newvalue")).get(TIMEOUT);
	}
	
	@Test
	public void	testSubscriptionThreading()
	{
		IComponentHandle	handle	= IComponentManager.get().create(new Object()).get(TIMEOUT);
		ISubscriptionIntermediateFuture<ChangeEvent>	sub	= handle.subscribeToResults();
		
		// Test that subscription schedules on global runner.
		Future<IComponent>	compfut	= new Future<>();
		sub.next(ev -> compfut.setResult(IComponentManager.get().getCurrentComponent()));
		handle.scheduleStep((IThrowingConsumer<IComponent>)comp
			-> comp.getFeature(IResultFeature.class).setResult("result", "value")).get(TIMEOUT);
		assertEquals(ComponentManager.get().getGlobalRunner(), compfut.get(TIMEOUT));
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
		class NoCopyPojo
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
		}
		
		NoCopyPojo	pojo	= new NoCopyPojo();
		IComponentHandle	handle	= IComponentManager.get().create(pojo).get(TIMEOUT);
		
		assertEquals(value, handle.getResults().get(TIMEOUT).get("field"));
		assertSame(value, handle.getResults().get(TIMEOUT).get("field"));
		assertEquals(value, handle.getResults().get(TIMEOUT).get("method1"));
		assertSame(value, handle.getResults().get(TIMEOUT).get("method1"));
		assertEquals(value, handle.getResults().get(TIMEOUT).get("method2"));
		assertSame(value, handle.getResults().get(TIMEOUT).get("method2"));
	}
	
	@Test
	public void	testCopyDynamic()
	{
		List<String>	value1	= new ArrayList<>();
		value1.add("Hello");
		
		class TestNoCopyDynamic
		{
			@ProvideResult
			Val<List<String>>	result	= new Val<>(value1);
		}
		TestNoCopyDynamic	pojo	= new TestNoCopyDynamic();
		IComponentHandle	comp	= IComponentManager.get().create(pojo).get(TIMEOUT);
		
		// Test initial value event
		ISubscriptionIntermediateFuture<ChangeEvent>	fut	= comp.subscribeToResults();
		Object	result1	= 	fut.getNextIntermediateResult(TIMEOUT).value();
		assertEquals(value1, result1);
		assertNotSame(value1, result1);
		
		// Test changed value event
		List<String>	value2	= new ArrayList<>();
		value2.add("world");
		comp.scheduleStep(() -> pojo.result.set(value2)).get(TIMEOUT);
		Object	result2	= 	fut.getNextIntermediateResult(TIMEOUT).value();
		assertEquals(value2, result2);
		assertNotSame(value2, result2);
	}
	
	@Test
	public void	testNoCopyDynamic()
	{
		List<String>	value1	= new ArrayList<>();
		value1.add("Hello");
		
		class TestNoCopyDynamic
		{
			@ProvideResult
			@NoCopy
			// Also tests unwrapping val and wrapper
			Val<List<String>>	result	= new Val<>(value1);
		}
		TestNoCopyDynamic	pojo	= new TestNoCopyDynamic();
		IComponentHandle	comp	= IComponentManager.get().create(pojo).get(TIMEOUT);
		
		// Test initial value event
		ISubscriptionIntermediateFuture<ChangeEvent>	fut	= comp.subscribeToResults();
		Object	result1	= 	fut.getNextIntermediateResult(TIMEOUT).value();
		assertEquals(value1, result1);
		assertSame(value1, result1);
		
		// Test changed value event
		List<String>	value2	= new ArrayList<>();
		value2.add("world");
		comp.scheduleStep(() -> pojo.result.set(value2)).get(TIMEOUT);
		Object	result2	= 	fut.getNextIntermediateResult(TIMEOUT).value();
		assertEquals(value2, result2);
		assertSame(value2, result2);
	}
	
	@Test
	public void	testLambdaResult()
	{
		IComponentHandle	comp	= IComponentManager.get().create(new Callable<String>()
		{
			@ProvideResult
			String	hello	= "Hello";
			
			@OnStart
			void	start(IResultFeature res)
			{
				res.setResult("world", "world");
			}
			
			@Override
			public String call()
			{
				return "!";
			}
		}).get(TIMEOUT);
		
		ISubscriptionIntermediateFuture<ChangeEvent>	fut	= comp.subscribeToResults();
		assertEquals("Hello", fut.getNextIntermediateResult(TIMEOUT).value());
		assertEquals("world", fut.getNextIntermediateResult(TIMEOUT).value());
		assertEquals("!", fut.getNextIntermediateResult(TIMEOUT).value());
		
		Map<String, Object>	results	= new LinkedHashMap<>();
		results.put("hello", "Hello");
		results.put("world", "world");
		results.put("result", "!");
		assertEquals(results, comp.getResults().get(TIMEOUT));
		
	}
}
