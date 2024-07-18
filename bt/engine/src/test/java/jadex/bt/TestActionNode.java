package jadex.bt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import jadex.bt.Node.NodeState;
import jadex.common.SUtil;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.TerminableFuture;

public class TestActionNode 
{
    @Test
    public void testActionNodeSuccess() 
    {
        ActionNode actionNode = new ActionNode(event -> 
        {
            System.out.println("Performing action...");
            return new Future<NodeState>(NodeState.SUCCEEDED);
        });

        Event event = new Event("start", null);
        IFuture<NodeState> ret = actionNode.execute(event);

        NodeState state = ret.get();
        assertEquals(NodeState.SUCCEEDED, state, "Node state should be SUCCEEDED");
    }

    @Test
    public void testActionNodeFailure() 
    {
        ActionNode actionNode = new ActionNode(event -> 
        {
            System.out.println("Performing action...");
            return new Future<NodeState>(NodeState.FAILED);
        });

        Event event = new Event("start", null);
        IFuture<NodeState> ret = actionNode.execute(event);

        NodeState state = ret.get();
        assertEquals(NodeState.FAILED, state, "Node state should be FAILED");
    }

    @Test
    public void testActionNodeAbort() 
    {
        ActionNode actionNode = new ActionNode(event -> 
        {
            System.out.println("Performing action...");
            return new Future<>();
            // Simulate a running action
        });

        Event event = new Event("start", null);
        IFuture<NodeState> ret = actionNode.execute(event);

        actionNode.abort();
        NodeState state = ret.get();
        assertEquals(NodeState.FAILED, state, "Node state should be FAILED after abort");
    }

    @Test
    public void testActionNodeException() 
    {
        ActionNode actionNode = new ActionNode(event -> 
        {
            System.out.println("Performing action...");
            throw new RuntimeException("Test exception");
        });

        Event event = new Event("start", null);
        IFuture<NodeState> ret = actionNode.execute(event);

        NodeState state = ret.get();
        assertEquals(NodeState.FAILED, state, "Node state should be FAILED after exception in action");
        //assertThrows(RuntimeException.class, ret::get, "Node should throw an exception");
    }
    
    @Test
    public void testActionNodeAbortAction() 
    {
    	AtomicBoolean stopped = new AtomicBoolean();

        ActionNode actionNode = new ActionNode(event -> 
        {
        	System.out.println("Performing action...");
            AtomicBoolean aborted = new AtomicBoolean();
            TerminableFuture<NodeState> ret = new TerminableFuture<>(ex -> aborted.set(true));
            new Thread(() -> 
	        {
	        	while(!aborted.get())
	        	{
	        		SUtil.sleep(30);
	        		System.out.println(".");
	        	}
	        	stopped.set(true);
	        }).start();
            return ret;
        });

        Event event = new Event("start", null);
        IFuture<NodeState> ret = actionNode.execute(event);

        actionNode.abort();
        
        SUtil.sleep(1000);
        
        NodeState state = ret.get();
        assertEquals(NodeState.FAILED, state, "Node state should be FAILED after exception in action");
        
        assertEquals(true, stopped.get(), "Node action should be stopped");
    }
}
