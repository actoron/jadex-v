package jadex.bt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import jadex.bt.Node.NodeState;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.TerminableFuture;

public class TestParallelNode 
{
    @Test
    public void testParallelAllSucceed() 
    {
        ActionNode<Object> action1 = new ActionNode<>((event, context) -> 
        {
            System.out.println("Action 1 succeeds...");
            return new Future<NodeState>(NodeState.SUCCEEDED);
        });

        ActionNode<Object> action2 = new ActionNode<>((event, context) -> 
        {
            System.out.println("Action 2 succeeds...");
            return new Future<NodeState>(NodeState.SUCCEEDED);
        });

        CompositeNode<Object> parallel = new ParallelNode<>()
        	.setSuccessMode(ParallelNode.ResultMode.ON_ALL)
            .setFailureMode(ParallelNode.ResultMode.ON_ONE)
            .addChild(action1).addChild(action2);

        Event event = new Event("start", null);
        IFuture<NodeState> ret = parallel.execute(event);

        NodeState state = ret.get();
        assertEquals(NodeState.SUCCEEDED, state, "node state");
    }

    @Test
    public void testParallelOneFails() 
    {
    	ActionNode<Object> action1 = new ActionNode<>((event, context) -> 
        {
            System.out.println("Action 1 succeeds...");
            return new Future<NodeState>(NodeState.SUCCEEDED);
        });

    	ActionNode<Object> action2 = new ActionNode<>((event, context) ->  
        {
            System.out.println("Action 2 fails...");
            return new Future<NodeState>(NodeState.FAILED);
        });

        CompositeNode<Object> parallel = new ParallelNode<>()
        	.setSuccessMode(ParallelNode.ResultMode.ON_ALL)
        	.setFailureMode(ParallelNode.ResultMode.ON_ONE)
        	.addChild(action1).addChild(action2);

        Event event = new Event("start", null);
        IFuture<NodeState> ret = parallel.execute(event);

        NodeState state = ret.get();
        assertEquals(NodeState.FAILED, state, "node state");
    }

    @Test
    public void testParallelOneSucceedsOnOneMode() 
    {
    	ActionNode<Object> action1 = new ActionNode<>((event, context) ->  
        {
            System.out.println("Action 1 succeeds...");
            return new Future<NodeState>(NodeState.SUCCEEDED);
        });

    	ActionNode<Object> action2 = new ActionNode<>((event, context) -> 
        {
            System.out.println("Action 2 running...");
            return new Future<>();
            // Simulate a running action
        });

        CompositeNode<Object> parallel = new ParallelNode<>()
        	.setSuccessMode(ParallelNode.ResultMode.ON_ONE)
            .setFailureMode(ParallelNode.ResultMode.ON_ALL)
            .addChild(action1).addChild(action2);
        
        Event event = new Event("start", null);
        IFuture<NodeState> ret = parallel.execute(event);

        NodeState state = ret.get();
        assertEquals(NodeState.SUCCEEDED, state, "node state");
    }

    @Test
    public void testParallelAllFail() 
    {
    	ActionNode<Object> action1 = new ActionNode<>((event, context) -> 
        {
            System.out.println("Action 1 fails...");
            return new Future<NodeState>(NodeState.FAILED);
        });

    	ActionNode<Object> action2 = new ActionNode<>((event, context) -> 
        {
            System.out.println("Action 2 fails...");
            return new Future<NodeState>(NodeState.FAILED);
        });

        CompositeNode<Object> parallel = new ParallelNode<>()
        	.setSuccessMode(ParallelNode.ResultMode.ON_ALL)
            .setFailureMode(ParallelNode.ResultMode.ON_ALL)
        	.addChild(action1).addChild(action2);
        
        Event event = new Event("start", null);
        IFuture<NodeState> ret = parallel.execute(event);

        NodeState state = ret.get();
        assertEquals(NodeState.FAILED, state, "node state");
    }

    @Test
    public void testParallelAbort() 
    {
    	ActionNode<Object> action1 = new ActionNode<>((event, context) -> 
        {
            System.out.println("Action 1 running...");
            return new Future<>();
            // Simulate a running action
        });

    	ActionNode<Object> action2 = new ActionNode<>((event, context) -> 
        {
            System.out.println("Action 2 running...");
            return new Future<>();
            // Simulate a running action
        });

        CompositeNode<Object> parallel = new ParallelNode<>()
        	.setSuccessMode(ParallelNode.ResultMode.ON_ALL)
            .setFailureMode(ParallelNode.ResultMode.ON_ONE)
            .addChild(action1).addChild(action2);
        
        Event event = new Event("start", null);
        IFuture<NodeState> ret = parallel.execute(event);

        parallel.abort();

        NodeState state = ret.get();
        assertEquals(NodeState.FAILED, state, "node state");
    }
}