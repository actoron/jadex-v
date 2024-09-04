package jadex.bt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import jadex.bt.actions.UserAction;
import jadex.bt.decorators.ConditionalDecorator;
import jadex.bt.decorators.Decorator;
import jadex.bt.decorators.RetryDecorator;
import jadex.bt.decorators.TimeoutDecorator;
import jadex.bt.impl.ComponentTimerCreator;
import jadex.bt.impl.Event;
import jadex.bt.impl.TimerCreator;
import jadex.bt.nodes.ActionNode;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.state.ExecutionContext;
import jadex.common.SUtil;
import jadex.core.IComponent;
import jadex.core.IExternalAccess;
import jadex.core.IThrowingConsumer;
import jadex.core.IThrowingFunction;
import jadex.execution.IExecutionFeature;
import jadex.execution.LambdaAgent;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.TerminableFuture;

public class DecoratorTest 
{
	@Test
	public void testRetryDecorator() 
	{
	    AtomicInteger attempt = new AtomicInteger(0);
	    Node<Object> an = new ActionNode<>(new UserAction<>((event, context) -> 
	    {
	    	System.out.println("action called: "+attempt.get());
	        Future<NodeState> ret = new Future<>();
	        if(attempt.incrementAndGet() < 3) 
	        {
	            ret.setResult(NodeState.FAILED);
	        } 
	        else 
	        {
	            ret.setResult(NodeState.SUCCEEDED);
	        }
	        return ret;
	    }));

	    RetryDecorator<Object> rd = new RetryDecorator<>(3);
	    an.addDecorator(rd);

	    Event event = new Event("start", null);
	    ExecutionContext<Object> context = new ExecutionContext<Object>();
	    IFuture<NodeState> ret = an.execute(event, context);
	    NodeState state = ret.get();
	    
	    System.out.println("state: "+state+" "+attempt.get());

	    assertEquals(NodeState.SUCCEEDED, state, "Node should succeed after retries");
	    assertEquals(3, attempt.get(), "Should have retried till attempt is 3");
	}
	
	@Test
	public void testRetryDelayDecorator() 
	{
	    AtomicInteger attempt = new AtomicInteger(0);
	    Node<Object> an = new ActionNode<>(new UserAction<>((event, context) -> 
	    {
	    	System.out.println("action called: "+attempt.get());
	        Future<NodeState> ret = new Future<>();
	        if(attempt.incrementAndGet() < 3) 
	        {
	            ret.setResult(NodeState.FAILED);
	        } 
	        else 
	        {
	            ret.setResult(NodeState.SUCCEEDED);
	        }
	        return ret;
	    }));

	    RetryDecorator<Object> rd = new RetryDecorator<>(3, 1000);
	    an.addDecorator(rd);

	    long start = System.currentTimeMillis();
	    
	    Event event = new Event("start", null);
	    ExecutionContext<Object> context = new ExecutionContext<Object>().setTimerCreator(new TimerCreator<Object>());
	    IFuture<NodeState> ret = an.execute(event, context);
	    NodeState state = ret.get();
	    
	    long end = System.currentTimeMillis();
	    long needed = end-start;
	    
	    System.out.println("state: "+state+" "+attempt.get()+" "+needed);

	    assertEquals(NodeState.SUCCEEDED, state, "Node should succeed after retries");
	    assertEquals(3, attempt.get(), "Should have retried 3 times");
	    assertTrue(needed>2000, "Should need more than 2 secs");
	}
	
	/*@Test
	public void testConditionalDecorator() 
	{
	    Blackboard blackboard = new Blackboard();
	    blackboard.set("condition", true);

	    Node<Object> an = new ActionNode<>((event, context) -> 
	    {
	        return new Future<>(NodeState.SUCCEEDED);
	    });

	    ConditionalDecorator<Object> cd = new ConditionalDecorator<>(bb -> (Boolean)bb.get("condition"));
	    an.setBlackboard(blackboard);
	    an.addBeforeDecorator(cd);

	    Event event = new Event("start", null);
	    IFuture<NodeState> ret = an.execute(event);
	    NodeState state = ret.get();

	    assertEquals(NodeState.SUCCEEDED, state, "Node should succeed as condition is true");

	    blackboard.set("condition", false);
	    ret = an.execute(event);
	    state = ret.get();

	    assertEquals(NodeState.FAILED, state, "Node should fail as condition is false");
	}*/
	
	@Test
	public void testComponentTimeoutDecoratorWithoutTimeout()
	{
		TerminableFuture<Void> fut = new TerminableFuture<>();
		IExecutionFeature exe = mock(IExecutionFeature.class);
		IComponent comp = mock(IComponent.class);
		when(comp.getFeature(IExecutionFeature.class)).thenReturn(exe);
		IExternalAccess access = mock(IExternalAccess.class);
		when(access.scheduleAsyncStep(any(IThrowingFunction.class))).thenReturn(fut);
		when(exe.waitForDelay(1000)).thenReturn(fut);
		when(comp.getExternalAccess()).thenReturn(access);
		
		Node<IComponent> an = new ActionNode<>(new UserAction<>((event, context) -> 
		{
		    Future<NodeState> ret = new Future<>();
		    new Thread(() -> 
		    {
		    	SUtil.sleep(500);
                ret.setResult(NodeState.SUCCEEDED);
		    }).start();
		    return ret;
		}));
		
		TimeoutDecorator<IComponent> td = new TimeoutDecorator<>(1000);
		an.addDecorator(td);
		
		ExecutionContext<IComponent> context = new ExecutionContext<IComponent>().setUserContext(comp).setTimerCreator(new TimerCreator<IComponent>());
	    IFuture<NodeState> res = an.execute(new Event("start", null), context);
	    new Thread(() -> 
        {
        	SUtil.sleep(1000);
        	fut.setResultIfUndone(null); // time elapsed -> timeout triggering here too late
        }).start();
	    
        NodeState state = res.get();
        
        System.out.println("state: "+state);
		
		assertEquals(NodeState.SUCCEEDED, state);
	}
	
	@Test
	public void testComponentTimeoutDecoratorWithTimeout()
	{
		TerminableFuture<Void> fut = new TerminableFuture<>();
		IExecutionFeature exe = mock(IExecutionFeature.class);
		IComponent comp = mock(IComponent.class);
		when(comp.getFeature(IExecutionFeature.class)).thenReturn(exe);
		IExternalAccess access = mock(IExternalAccess.class);
		when(access.scheduleAsyncStep(any(IThrowingFunction.class))).thenReturn(fut);
		when(exe.waitForDelay(1000)).thenReturn(fut);
		when(comp.getExternalAccess()).thenReturn(access);
		
		Node<IComponent> an = new ActionNode<>(new UserAction<>((event, context) -> 
		{
		    Future<NodeState> ret = new Future<>();
		    new Thread(() -> 
		    {
		    	SUtil.sleep(1000);
                ret.setResult(NodeState.SUCCEEDED);
		    }).start();
		    return ret;
		}));
		
		TimeoutDecorator<IComponent> td = new TimeoutDecorator<>(500);
		an.addDecorator(td);
		
		ExecutionContext<IComponent> context = new ExecutionContext<IComponent>().setUserContext(comp).setTimerCreator(new TimerCreator<IComponent>());
	    IFuture<NodeState> res = an.execute(new Event("start", null), context);
	    fut.setResult(null); // time elapsed
	    
        NodeState state = res.get();
        
        System.out.println("state: "+state);
		
		assertEquals(NodeState.FAILED, state);
	}
	
	@Test
	public void testRealComponentTimeoutDecoratorWithoutTimeout()
	{
		IExternalAccess comp = LambdaAgent.create((IThrowingConsumer<IComponent>)a -> System.out.println("started: "+a.getId()));
		
		Node<IExternalAccess> an = new ActionNode<>(new UserAction<>((event, compo) -> 
		{
		    Future<NodeState> ret = new Future<>();
		    new Thread(() -> 
		    {
		    	SUtil.sleep(500);
                ret.setResult(NodeState.SUCCEEDED);
		    }).start();
		    return ret;
		}));
		
		TimeoutDecorator<IExternalAccess> td = new TimeoutDecorator<>(1000);
		an.addDecorator(td);
		
		ExecutionContext<IExternalAccess> context = new ExecutionContext<IExternalAccess>().setTimerCreator(new ComponentTimerCreator<IExternalAccess>());
		context.setUserContext(comp);

	    IFuture<NodeState> res = an.execute(new Event("start", null), context);
	    
        NodeState state = res.get();
        
        System.out.println("state: "+state);
		
		assertEquals(NodeState.SUCCEEDED, state);
	}
	
	@Test
	public void testRealComponentTimeoutDecoratorWithTimeout()
	{
		IExternalAccess comp = LambdaAgent.create((IThrowingConsumer<IComponent>)a -> System.out.println("started: "+a.getId()));
		
		Node<IExternalAccess> an = new ActionNode<>(new UserAction<>((event, IComponent) -> 
		{
		    Future<NodeState> ret = new Future<>();
		    new Thread(() -> 
		    {
		    	SUtil.sleep(10000);
                ret.setResult(NodeState.SUCCEEDED);
		    }).start();
		    return ret;
		}));
		
		TimeoutDecorator<IExternalAccess> td = new TimeoutDecorator<>(500);
		an.addDecorator(td);
		
		ExecutionContext<IExternalAccess> context = new ExecutionContext<IExternalAccess>().setTimerCreator(new ComponentTimerCreator<IExternalAccess>());
		context.setUserContext(comp);
		
	    IFuture<NodeState> res = an.execute(new Event("start", null), context);
	    
        NodeState state = res.get();
        
        System.out.println("state: "+state);
		
		assertEquals(NodeState.FAILED, state);
	}
	
	@Test
	public void testResultInvertDecorator() 
	{
	    Node<Object> an = new ActionNode<>(new UserAction<>((event, context) -> 
	    {
	    	System.out.println("action called");
	        return new Future<>(NodeState.FAILED);
	    }));

	    Decorator<Object> id = new ConditionalDecorator<>().setFunction((event, state, context) -> NodeState.SUCCEEDED==state? NodeState.FAILED: NodeState.SUCCEEDED);
	    an.addDecorator(id);

	    Event event = new Event("start", null);
		ExecutionContext<Object> context = new ExecutionContext<Object>();
	    IFuture<NodeState> ret = an.execute(event, context);
	    NodeState state = ret.get();

	    assertEquals(NodeState.SUCCEEDED, state, "Node should succeed due to inversion");
	}
	
	@Test
	public void testSuccessDecorator() 
	{
	    Node<Object> an = new ActionNode<>(new UserAction<>((event, context) -> 
	    {
	        System.out.println("action called");
	        return new Future<>(NodeState.FAILED);
	    }));

	    Decorator<Object> sd = new ConditionalDecorator<>().setFunction((event, state, context) -> NodeState.SUCCEEDED);
	    an.addDecorator(sd);

	    Event event = new Event("start", null);
	    ExecutionContext<Object> context = new ExecutionContext<Object>();

	    IFuture<NodeState> ret = an.execute(event, context);
	    NodeState state = ret.get();

	    assertEquals(NodeState.SUCCEEDED, state, "Node should succeed regardless of the action result");
	}
	
	@Test
	public void testFailureDecorator() 
	{
	    Node<Object> an = new ActionNode<>(new UserAction<>((event, context) -> 
	    {
	        System.out.println("action called");
	        return new Future<>(NodeState.SUCCEEDED);
	    }));

	    Decorator<Object> sd = new ConditionalDecorator<>().setFunction((event, state, context) -> NodeState.FAILED);
	    an.addDecorator(sd);

	    Event event = new Event("start", null);
	    ExecutionContext<Object> context = new ExecutionContext<Object>();

	    IFuture<NodeState> ret = an.execute(event, context);
	    NodeState state = ret.get();

	    assertEquals(NodeState.FAILED, state, "Node should fail regardless of the action result");
	}
	
	/*@Test
	public void testCooldownDecorator() 
	{
	    TerminableFuture<Void> fut = new TerminableFuture<>();
	    IExecutionFeature exe = mock(IExecutionFeature.class);
	    IComponent comp = mock(IComponent.class);
	    when(comp.getFeature(IExecutionFeature.class)).thenReturn(exe);
	    IExternalAccess access = mock(IExternalAccess.class);
	    when(access.scheduleAsyncStep(any(IThrowingFunction.class))).thenReturn(fut);
	    when(exe.waitForDelay(1000)).thenReturn(fut);
		when(comp.getExternalAccess()).thenReturn(access);
	    
		//IExternalAccess comp = LambdaAgent.create((IThrowingConsumer<IComponent>)a -> System.out.println("started: "+a.getId()));
		
	    Node<IComponent> an = new ActionNode<>(new UserAction<>((event, agent) -> 
	    {
	    	System.out.println("action called");
	        return new Future<>(NodeState.SUCCEEDED);
	    }));
	    an.addBeforeDecorator(new CooldownDecorator<>(1000));
	    an.addAfterDecorator(new RepeatDecorator<IComponent>());
	    
	    ExecutionContext<IComponent> context = new ExecutionContext<IComponent>().setUserContext(comp);

	    Event event = new Event("start", null);
	    IFuture<NodeState> ret1 = an.execute(event, context);
	    NodeState state1 = ret1.get();
	    
	    System.out.println("state1: "+state1);
	    
	    /*assertEquals(NodeState.SUCCEEDED, state1, "Node should succeed on first execution");

	    IFuture<NodeState> ret2 = an.execute(event, context);
	    NodeState state2 = ret2.get();
	    
	    System.out.println("state2: "+state2);
	    
	    assertEquals(NodeState.FAILED, state2, "Node should return running during cooldown period");

	    SUtil.sleep(1000);

	    IFuture<NodeState> ret3 = an.execute(event, context);
	    NodeState state3 = ret3.get();
	    
	    System.out.println("state3: "+state3);
	    
	    assertEquals(NodeState.SUCCEEDED, state3, "Node should succeed after cooldown period");
	}*/
}