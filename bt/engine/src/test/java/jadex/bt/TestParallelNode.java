package jadex.bt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import jadex.bt.actions.TerminableUserAction;
import jadex.bt.impl.Event;
import jadex.bt.nodes.ActionNode;
import jadex.bt.nodes.CompositeNode;
import jadex.bt.nodes.Node.AbortMode;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.nodes.ParallelNode;
import jadex.bt.state.ExecutionContext;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.TerminableFuture;

public class TestParallelNode 
{
    @Test
    public void testParallelAllSucceed() 
    {
        ActionNode<Object> action1 = new ActionNode<>(new TerminableUserAction<>((event, context) -> 
        {
            System.out.println("Action 1 succeeds...");
            return new TerminableFuture<NodeState>(NodeState.SUCCEEDED);
        }));

        ActionNode<Object> action2 = new ActionNode<>(new TerminableUserAction<>((event, context) -> 
        {
            System.out.println("Action 2 succeeds...");
            return new TerminableFuture<NodeState>(NodeState.SUCCEEDED);
        }));

        CompositeNode<Object> parallel = new ParallelNode<>()
        	.setSuccessMode(ParallelNode.ResultMode.ON_ALL)
            .setFailureMode(ParallelNode.ResultMode.ON_ONE)
            .addChild(action1).addChild(action2);

        Event event = new Event("start", null);
        ExecutionContext<Object> context = new ExecutionContext<Object>();
        IFuture<NodeState> ret = parallel.execute(event, context);

        NodeState state = ret.get();
        assertEquals(NodeState.SUCCEEDED, state, "node state");
    }

    @Test
    public void testParallelOneFails() 
    {
    	ActionNode<Object> action1 = new ActionNode<>(new TerminableUserAction<>((event, context) -> 
        {
            System.out.println("Action 1 succeeds...");
            return new TerminableFuture<NodeState>(NodeState.SUCCEEDED);
        }));

    	ActionNode<Object> action2 = new ActionNode<>(new TerminableUserAction<>((event, context) ->  
        {
            System.out.println("Action 2 fails...");
            return new TerminableFuture<NodeState>(NodeState.FAILED);
        }));

        CompositeNode<Object> parallel = new ParallelNode<>()
        	.setSuccessMode(ParallelNode.ResultMode.ON_ALL)
        	.setFailureMode(ParallelNode.ResultMode.ON_ONE)
        	.addChild(action1).addChild(action2);

        Event event = new Event("start", null);
        ExecutionContext<Object> context = new ExecutionContext<Object>();
        IFuture<NodeState> ret = parallel.execute(event, context);

        NodeState state = ret.get();
        assertEquals(NodeState.FAILED, state, "node state");
    }

    @Test
    public void testParallelOneSucceedsOnOneMode() 
    {
    	ActionNode<Object> action1 = new ActionNode<>(new TerminableUserAction<>((event, context) ->  
        {
            System.out.println("Action 1 succeeds...");
            return new TerminableFuture<>(NodeState.SUCCEEDED);
        }));

    	ActionNode<Object> action2 = new ActionNode<>(new TerminableUserAction<>((event, context) -> 
        {
            System.out.println("Action 2 running...");
            return new TerminableFuture<>();
            // Simulate a running action
        }));

        CompositeNode<Object> parallel = new ParallelNode<>()
        	.setSuccessMode(ParallelNode.ResultMode.ON_ONE)
            .setFailureMode(ParallelNode.ResultMode.ON_ALL)
            .addChild(action1).addChild(action2);
        
        Event event = new Event("start", null);
        ExecutionContext<Object> context = new ExecutionContext<Object>();
        IFuture<NodeState> ret = parallel.execute(event, context);

        NodeState state = ret.get();
        assertEquals(NodeState.SUCCEEDED, state, "node state");
    }

    @Test
    public void testParallelAllFail() 
    {
    	ActionNode<Object> action1 = new ActionNode<>(new TerminableUserAction<>((event, context) -> 
        {
            System.out.println("Action 1 fails...");
            TerminableFuture<NodeState> ret = new TerminableFuture<>();
	    	ret.setResult(NodeState.FAILED);
	        return ret;
            
        }));

    	ActionNode<Object> action2 = new ActionNode<>(new TerminableUserAction<>((event, context) -> 
        {
            System.out.println("Action 2 fails...");
            TerminableFuture<NodeState> ret = new TerminableFuture<>();
	    	ret.setResult(NodeState.FAILED);
	        return ret;
        }));

        CompositeNode<Object> parallel = new ParallelNode<>()
        	.setSuccessMode(ParallelNode.ResultMode.ON_ALL)
            .setFailureMode(ParallelNode.ResultMode.ON_ALL)
        	.addChild(action1).addChild(action2);
        
        Event event = new Event("start", null);
        ExecutionContext<Object> context = new ExecutionContext<Object>();
        IFuture<NodeState> ret = parallel.execute(event, context);

        NodeState state = ret.get();
        assertEquals(NodeState.FAILED, state, "node state");
    }

    @Test
    public void testParallelAbort() 
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

        CompositeNode<Object> parallel = new ParallelNode<>()
        	.setSuccessMode(ParallelNode.ResultMode.ON_ALL)
            .setFailureMode(ParallelNode.ResultMode.ON_ONE)
            .addChild(action1).addChild(action2);
        
        Event event = new Event("start", null);
        ExecutionContext<Object> context = new ExecutionContext<Object>();
        IFuture<NodeState> ret = parallel.execute(event, context);

        parallel.abort(AbortMode.SELF, NodeState.FAILED, context);

        NodeState state = ret.get();
        assertEquals(NodeState.FAILED, state, "node state");
    }
}