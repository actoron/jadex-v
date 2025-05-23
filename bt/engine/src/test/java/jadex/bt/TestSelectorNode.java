package jadex.bt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import jadex.bt.actions.TerminableUserAction;
import jadex.bt.impl.Event;
import jadex.bt.nodes.ActionNode;
import jadex.bt.nodes.CompositeNode;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.Node.AbortMode;
import jadex.bt.nodes.Node.NodeState;
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
        ExecutionContext<Object> context = new ExecutionContext<Object>();
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
        ExecutionContext<Object> context = new ExecutionContext<Object>();
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
        ExecutionContext<Object> context = new ExecutionContext<Object>();
        IFuture<NodeState> ret = selector.execute(event, context);

        NodeState state = ret.get();
        assertEquals(NodeState.FAILED, state, "node state");
    }

    @Test
    public void testSelectorAbort() 
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
        ExecutionContext<Object> context = new ExecutionContext<Object>();
        IFuture<NodeState> ret = selector.execute(event, context);

        selector.abort(AbortMode.SELF, NodeState.FAILED, context);

        NodeState state = ret.get();
        assertEquals(NodeState.FAILED, state, "node state");
    }
}