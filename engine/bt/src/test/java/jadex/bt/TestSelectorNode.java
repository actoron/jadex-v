package jadex.bt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import jadex.bt.actions.TerminableUserAction;
import jadex.bt.impl.Event;
import jadex.bt.nodes.ActionNode;
import jadex.bt.nodes.CompositeNode;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.Node.AbortMode;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.nodes.ParallelNode;
import jadex.bt.nodes.RandomChildTraversalStrategy;
import jadex.bt.nodes.SelectorNode;
import jadex.bt.state.ExecutionContext;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.TerminableFuture;

public class TestSelectorNode 
{
	@Test
	public void testSelectorSuccessOnFirst() 
	{
		Node<Object> alwaysSucceed = new ActionNode<>(new TerminableUserAction<>((event, context) -> 
		{
			System.out.println("Always succeed...");
			return new TerminableFuture<NodeState>(NodeState.SUCCEEDED);
        }));

        Node<Object> alwaysFail = new ActionNode<>(new TerminableUserAction<>((event, context) -> 
        {
        	System.out.println("Always fail...");
        	return new TerminableFuture<NodeState>(NodeState.FAILED);
        }));

        CompositeNode<Object> selector = new SelectorNode<>().addChild(alwaysSucceed).addChild(alwaysFail);

        Event event = new Event("start", null);
        ExecutionContext<Object> context = new ExecutionContext<Object>(selector);
        IFuture<NodeState> ret = selector.execute(event, context);

        NodeState state = ret.get();
        assertEquals(NodeState.SUCCEEDED, state, "node state");
	}

    @Test
    public void testSelectorSuccessOnSecond() 
    {
        Node<Object> alwaysFail = new ActionNode<>(new TerminableUserAction<>((event, context) -> 
        {
            System.out.println("Always fail...");
            return new TerminableFuture<NodeState>(NodeState.FAILED);
        }));

        Node<Object> alwaysSucceed = new ActionNode<>(new TerminableUserAction<>((event, context) -> 
        {
            System.out.println("Always succeed...");
            return new TerminableFuture<NodeState>(NodeState.SUCCEEDED);
        }));

        CompositeNode<Object> selector = new SelectorNode<>().addChild(alwaysFail).addChild(alwaysSucceed);

        Event event = new Event("start", null);
        ExecutionContext<Object> context = new ExecutionContext<Object>(selector);
        IFuture<NodeState> ret = selector.execute(event, context);

        NodeState state = ret.get();
        assertEquals(NodeState.SUCCEEDED, state, "node state");
    }

    @Test
    public void testSelectorFailure() 
    {
        Node<Object> alwaysFail1 = new ActionNode<>(new TerminableUserAction<>((event, context) -> 
        {
            System.out.println("Always fail 1...");
            TerminableFuture<NodeState> ret = new TerminableFuture<NodeState>();
            ret.setResult(NodeState.FAILED);
            return ret;
        }));

        Node<Object> alwaysFail2 = new ActionNode<>(new TerminableUserAction((event, context) -> 
        {
            System.out.println("Always fail 2...");
            TerminableFuture<NodeState> ret = new TerminableFuture<NodeState>();
            ret.setResult(NodeState.FAILED);
            return ret;
        }));

        CompositeNode<Object> selector = new SelectorNode<>().addChild(alwaysFail1).addChild(alwaysFail2);

        Event event = new Event("start", null);
        ExecutionContext<Object> context = new ExecutionContext<Object>(selector);
        IFuture<NodeState> ret = selector.execute(event, context);

        NodeState state = ret.get();
        assertEquals(NodeState.FAILED, state, "node state");
    }

    @Test
    public void testSelectorAbortFail() 
    {
        Node<Object> alwaysFail = new ActionNode<>(new TerminableUserAction<>((event, context) -> 
        {
            System.out.println("Always fail...");
            return new TerminableFuture<>();
            // Simulate a running action
        }));

        Node<Object> alwaysSucceed = new ActionNode<>(new TerminableUserAction<>((event, context) -> 
        {
            System.out.println("Always succeed...");
            TerminableFuture<NodeState> ret = new TerminableFuture<NodeState>();
            ret.setResult(NodeState.SUCCEEDED);
            return ret;
        }));

        CompositeNode<Object> selector = new SelectorNode<>().addChild(alwaysFail).addChild(alwaysSucceed);

        Event event = new Event("start", null);
        ExecutionContext<Object> context = new ExecutionContext<Object>(selector);
        IFuture<NodeState> ret = selector.execute(event, context);

        selector.abort(AbortMode.SELF, NodeState.FAILED, context);

        NodeState state = ret.get();
        assertEquals(NodeState.FAILED, state, "node state");
    }

    @Test
    public void testSelectorAbortSuccess() 
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

        CompositeNode<Object> sel = new SelectorNode<>()
            .addChild(action1).addChild(action2);
        
        Event event = new Event("start", null);
        ExecutionContext<Object> context = new ExecutionContext<Object>(sel);
        IFuture<NodeState> ret = sel.execute(event, context);

        sel.abort(AbortMode.SELF, NodeState.SUCCEEDED, context);

        NodeState state = ret.get();
        assertEquals(NodeState.SUCCEEDED, state, "node state");
    }

    @Test
    public void testSelectorRandomTraversalProperties()
    {
        int seed = 1337;

        List<Integer> executed = new ArrayList<>();

        Node<Object> a = new ActionNode<>(new TerminableUserAction<>((e, x) ->
        {
            executed.add(1);
            return new TerminableFuture<>(NodeState.FAILED);
        }));

        Node<Object> b = new ActionNode<>(new TerminableUserAction<>((e, x) ->
        {
            executed.add(2);
            return new TerminableFuture<>(NodeState.FAILED);
        }));

        Node<Object> c = new ActionNode<>(new TerminableUserAction<>((e, x) ->
        {
            executed.add(3);
            return new TerminableFuture<>(NodeState.FAILED);
        }));

        CompositeNode<Object> selector = new SelectorNode<>("selector-random",
            new RandomChildTraversalStrategy<>(seed))
            .addChild(a)
            .addChild(b)
            .addChild(c);

        ExecutionContext<Object> ctx = new ExecutionContext<>(selector);
        NodeState state = selector.execute(new Event("start", null), ctx).get();

        assertEquals(NodeState.FAILED, state);
        
        List<Integer> expected = new ArrayList<>(List.of(1, 2, 3));
        Collections.shuffle(expected, new Random(seed));

        assertEquals(3, executed.size());
        assertEquals(expected, executed);
    }

}