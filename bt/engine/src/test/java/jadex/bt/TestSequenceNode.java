package jadex.bt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import jadex.bt.Node.NodeState;
import jadex.common.SUtil;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.TerminableFuture;

public class TestSequenceNode 
{
	@Test
	public void testSequenceSuccess()
	{
		Node findres = new ActionNode(event -> {
		    System.out.println("Searching for resources...");
		    return new Future<NodeState>(NodeState.SUCCEEDED);
		});
		
		Node collectres = new ActionNode(event -> {
		    System.out.println("Collecting resources...");
		    return new Future<NodeState>(NodeState.SUCCEEDED);
		});
		
		Node drivehome = new ActionNode(event -> {
		    System.out.println("Driving home...");
		    return new Future<NodeState>(NodeState.SUCCEEDED);
		});
		
		CompositeNode sequence = new SequenceNode().addChild(findres).addChild(collectres).addChild(drivehome);
		
		Event event = new Event("start", null);
		IFuture<NodeState> ret = sequence.execute(event);
		
		NodeState state = ret.get();
		assertEquals(state, NodeState.SUCCEEDED, "node state");
		
		//ret.then(res -> System.out.println("result of tree: "+res)).catchEx(ex -> ex.printStackTrace());
	}
	
	@Test
	public void testSequenceFailure()
	{
		Node findres = new ActionNode(event -> 
		{
		    System.out.println("Searching for resources...");
		    return new Future<NodeState>(NodeState.SUCCEEDED);
		});
		
		Node collectres = new ActionNode(event -> 
		{
		    System.out.println("Collecting resources...");
		    return new Future<NodeState>(NodeState.FAILED);
		});
		
		Node drivehome = new ActionNode(event -> 
		{
		    System.out.println("Driving home...");
		    return new Future<NodeState>(NodeState.SUCCEEDED);
		});
		
		CompositeNode sequence = new SequenceNode().addChild(findres).addChild(collectres).addChild(drivehome);
		
		Event event = new Event("start", null);
		IFuture<NodeState> ret = sequence.execute(event);
		
		NodeState state = ret.get();
		assertEquals(state, NodeState.FAILED, "node state");
		
		//ret.then(res -> System.out.println("result of tree: "+res)).catchEx(ex -> ex.printStackTrace());
	}
	
	@Test
	public void testEmptySequence() 
	{
	    SequenceNode sequence = new SequenceNode();
	    
	    Event event = new Event("start", null);
	    IFuture<NodeState> ret = sequence.execute(event);
	    
	    NodeState state = ret.get();
	    assertEquals(state, NodeState.SUCCEEDED, "node state");
	}
	
	@Test
	public void testSequenceAbort() 
	{
	    Node findres = new ActionNode(event -> 
	    {
	        System.out.println("Searching for resources...");
	        TerminableFuture<NodeState> ret = new TerminableFuture<>();
	        return ret;
	    });
	    
	    CompositeNode sequence = new SequenceNode().addChild(findres);
	    
	    Event event = new Event("start", null);
	    IFuture<NodeState> ret = sequence.execute(event);
	    
	    sequence.abort();
	    
	    NodeState state = ret.get();
	    assertEquals(state, NodeState.FAILED, "node state");
	}
	
	@Test
	public void testSequenceWithRunningNode() 
	{
	    Node findres = new ActionNode(event -> 
	    {
	        System.out.println("Searching for resources...");
	        TerminableFuture<NodeState> ret = new TerminableFuture<>();
	        // Simulate running state
	        return ret;
	    });
	    
	    CompositeNode sequence = new SequenceNode().addChild(findres);
	    
	    Event event = new Event("start", null);
	    IFuture<NodeState> ret = sequence.execute(event);
	    
	    assertFalse(ret.isDone(), "sequence state");
	}
	
	@Test
	public void testSequenceWithDelayedNodes() throws InterruptedException 
	{
	    Node findres = new ActionNode(event -> 
	    {
	        System.out.println("Searching for resources...");
	        TerminableFuture<NodeState> ret = new TerminableFuture<>();
	        new Thread(() -> 
	        {
	        	SUtil.sleep(100);
	            ret.setResult(NodeState.SUCCEEDED);
	        }).start();
	        return ret;
	    });
	    
	    CompositeNode sequence = new SequenceNode().addChild(findres);
	    
	    Event event = new Event("start", null);
	    IFuture<NodeState> ret = sequence.execute(event);
	    
	    NodeState state = ret.get();
	    assertEquals(state, NodeState.SUCCEEDED, "node state");
	}
	
	@Test
	public void testSequenceReset() 
	{
	    Node findres = new ActionNode(event -> 
	    {
	        System.out.println("Searching for resources...");
	        return new Future<NodeState>(NodeState.SUCCEEDED);
	    });
	    
	    CompositeNode sequence = new SequenceNode().addChild(findres);
	    
	    Event event = new Event("start", null);
	    IFuture<NodeState> ret = sequence.execute(event);
	    
	    NodeState state = ret.get();
	    assertEquals(state, NodeState.SUCCEEDED, "node state");
	    
	    ret = sequence.execute(event);
	    state = ret.get();
	    assertEquals(state, NodeState.SUCCEEDED, "node state");
	}
}
