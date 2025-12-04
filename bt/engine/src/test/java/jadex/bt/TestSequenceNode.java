package jadex.bt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import jadex.bt.actions.TerminableUserAction;
import jadex.bt.impl.Event;
import jadex.bt.nodes.ActionNode;
import jadex.bt.nodes.CompositeNode;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.Node.AbortMode;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.nodes.SelectorNode;
import jadex.bt.nodes.SequenceNode;
import jadex.bt.state.ExecutionContext;
import jadex.common.SUtil;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.TerminableFuture;

public class TestSequenceNode 
{
	@Test
	public void testSequenceSuccess()
	{
		Node<Object> findres = new ActionNode<>(new TerminableUserAction<>((event, context) -> 
		{
		    System.out.println("Searching for resources...");
		    return new TerminableFuture<NodeState>(NodeState.SUCCEEDED);
		}));
		
		Node<Object> collectres = new ActionNode<>(new TerminableUserAction<>((event, context) -> 
		{
		    System.out.println("Collecting resources...");
		    return new TerminableFuture<NodeState>(NodeState.SUCCEEDED);
		}));
		
		Node<Object> drivehome = new ActionNode<>(new TerminableUserAction<>((event, context) -> 
		{
		    System.out.println("Driving home...");
		    return new TerminableFuture<NodeState>(NodeState.SUCCEEDED);
		}));
		
		CompositeNode<Object> sequence = new SequenceNode<>().addChild(findres).addChild(collectres).addChild(drivehome);
		
		Event event = new Event("start", null);
		ExecutionContext<Object> context = new ExecutionContext<Object>();
		IFuture<NodeState> ret = sequence.execute(event, context);
		
		NodeState state = ret.get();
		assertEquals(state, NodeState.SUCCEEDED, "node state");
		
		//ret.then(res -> System.out.println("result of tree: "+res)).catchEx(ex -> ex.printStackTrace());
	}
	
	@Test
	public void testSequenceFailure()
	{
		Node<Object> findres = new ActionNode<>(new TerminableUserAction<>((event, context) -> 
		{
		    System.out.println("Searching for resources...");
		    return new TerminableFuture<NodeState>(NodeState.SUCCEEDED);
		}));
		
		Node<Object> collectres = new ActionNode<>(new TerminableUserAction<>((event, context) -> 
		{
		    System.out.println("Collecting resources...");
		    return new TerminableFuture<NodeState>(NodeState.FAILED);
		}));
		
		Node<Object> drivehome = new ActionNode<>(new TerminableUserAction<>((event, context) -> 
		{
		    System.out.println("Driving home...");
		    return new TerminableFuture<NodeState>(NodeState.SUCCEEDED);
		}));
		
		CompositeNode<Object> sequence = new SequenceNode<>().addChild(findres).addChild(collectres).addChild(drivehome);
		
		Event event = new Event("start", null);
		ExecutionContext<Object> context = new ExecutionContext<Object>();
		IFuture<NodeState> ret = sequence.execute(event, context);
		
		NodeState state = ret.get();
		assertEquals(state, NodeState.FAILED, "node state");
		
		//ret.then(res -> System.out.println("result of tree: "+res)).catchEx(ex -> ex.printStackTrace());
	}
	
	@Test
	public void testEmptySequence() 
	{
	    SequenceNode<Object> sequence = new SequenceNode<>();
	    
	    Event event = new Event("start", null);
	    ExecutionContext<Object> context = new ExecutionContext<Object>();
	    IFuture<NodeState> ret = sequence.execute(event, context);
	    
	    NodeState state = ret.get();
	    assertEquals(state, NodeState.SUCCEEDED, "node state");
	}
	
	@Test
	public void testSequenceAbortFail() 
	{
	    Node<Object> findres = new ActionNode<>(new TerminableUserAction<>((event, context) -> 
	    {
	        System.out.println("Searching for resources...");
	        TerminableFuture<NodeState> ret = new TerminableFuture<>();
	        return ret;
	    }));
	    
	    CompositeNode<Object> sequence = new SequenceNode<>().addChild(findres);
	    
	    Event event = new Event("start", null);
	    ExecutionContext<Object> context = new ExecutionContext<Object>();
	    IFuture<NodeState> ret = sequence.execute(event, context);
	    
	    sequence.abort(AbortMode.SELF, NodeState.FAILED, context);
	    
	    NodeState state = ret.get();
	    assertEquals(state, NodeState.FAILED, "node state");
	}

	@Test
    public void testSequenceAbortSuccess() 
    {
    	ActionNode<Object> action1 = new ActionNode<>(new TerminableUserAction<>((event, context) -> 
        {
            System.out.println("Action 1 running...");
            return new TerminableFuture<>();
            // Simulate a running action
        }));

    	ActionNode<Object> action2 = new ActionNode<>(new TerminableUserAction<>((event, context) -> 
        {
            System.out.println("Action 2 running...");
            return new TerminableFuture<>();
            // Simulate a running action
        }));

        CompositeNode<Object> seq = new SequenceNode<>()
            .addChild(action1).addChild(action2);
        
        Event event = new Event("start", null);
        ExecutionContext<Object> context = new ExecutionContext<Object>();
        IFuture<NodeState> ret = seq.execute(event, context);

        seq.abort(AbortMode.SELF, NodeState.SUCCEEDED, context);

        NodeState state = ret.get();
        assertEquals(NodeState.SUCCEEDED, state, "node state");
    }
	
	@Test
	public void testSequenceWithRunningNode() 
	{
	    Node<Object> findres = new ActionNode<>(new TerminableUserAction<>((event, context) -> 
	    {
	        System.out.println("Searching for resources...");
	        TerminableFuture<NodeState> ret = new TerminableFuture<>();
	        // Simulate running state
	        return ret;
	    }));
	    
	    CompositeNode<Object> sequence = new SequenceNode<>().addChild(findres);
	    
	    Event event = new Event("start", null);
	    ExecutionContext<Object> context = new ExecutionContext<Object>();
	    IFuture<NodeState> ret = sequence.execute(event, context);
	    
	    assertFalse(ret.isDone(), "sequence state");
	}
	
	@Test
	public void testSequenceWithDelayedNodes() throws InterruptedException 
	{
		Node<Object> findres = new ActionNode<>(new TerminableUserAction<>((event, context) -> 
	    {
	        System.out.println("Searching for resources...");
	        TerminableFuture<NodeState> ret = new TerminableFuture<>();
	        new Thread(() -> 
	        {
	        	SUtil.sleep(100);
	            ret.setResult(NodeState.SUCCEEDED);
	        }).start();
	        return ret;
	    }));
	    
	    CompositeNode<Object> sequence = new SequenceNode<>().addChild(findres);
	    
	    Event event = new Event("start", null);
	    ExecutionContext<Object> context = new ExecutionContext<Object>();
	    IFuture<NodeState> ret = sequence.execute(event, context);
	    
	    NodeState state = ret.get();
	    assertEquals(state, NodeState.SUCCEEDED, "node state");
	}
	
	@Test
	public void testSequenceReset() 
	{
		Node<Object> findres = new ActionNode<>(new TerminableUserAction<>((event, context) -> 
	    {
	        System.out.println("Searching for resources...");
	        return new TerminableFuture<NodeState>(NodeState.SUCCEEDED);
	    }));
	    
	    CompositeNode<Object> sequence = new SequenceNode<>().addChild(findres);
	    
	    Event event = new Event("start", null);
	    ExecutionContext<Object> context = new ExecutionContext<Object>();
	    IFuture<NodeState> ret = sequence.execute(event, context);
	    
	    NodeState state = ret.get();
	    assertEquals(state, NodeState.SUCCEEDED, "node state");
	    
	    ret = sequence.execute(event, context);
	    state = ret.get();
	    assertEquals(state, NodeState.SUCCEEDED, "node state");
	}
}
