package jadex.bt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import jadex.bt.Node.NodeState;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.TerminableFuture;

public class TestSelectorNode 
{
	@Test
	public void testSelectorSuccessOnFirst() 
	{
		Node<Object> alwaysSucceed = new ActionNode<>((event, context) -> 
		{
			System.out.println("Always succeed...");
			return new Future<NodeState>(NodeState.SUCCEEDED);
        });

        Node<Object> alwaysFail = new ActionNode<>((event, context) -> 
        {
        	System.out.println("Always fail...");
        	return new Future<NodeState>(NodeState.FAILED);
        });

        CompositeNode<Object> selector = new SelectorNode<>().addChild(alwaysSucceed).addChild(alwaysFail);

        Event event = new Event("start", null);
        IFuture<NodeState> ret = selector.execute(event);

        NodeState state = ret.get();
        assertEquals(NodeState.SUCCEEDED, state, "node state");
	}

    @Test
    public void testSelectorSuccessOnSecond() 
    {
        Node<Object> alwaysFail = new ActionNode<>((event, context) -> 
        {
            System.out.println("Always fail...");
            return new Future<NodeState>(NodeState.FAILED);
        });

        Node<Object> alwaysSucceed = new ActionNode<>((event, context) -> 
        {
            System.out.println("Always succeed...");
            return new Future<NodeState>(NodeState.SUCCEEDED);
        });

        CompositeNode<Object> selector = new SelectorNode<>().addChild(alwaysFail).addChild(alwaysSucceed);

        Event event = new Event("start", null);
        IFuture<NodeState> ret = selector.execute(event);

        NodeState state = ret.get();
        assertEquals(NodeState.SUCCEEDED, state, "node state");
    }

    @Test
    public void testSelectorFailure() 
    {
        Node<Object> alwaysFail1 = new ActionNode<>((event, context) -> 
        {
            System.out.println("Always fail 1...");
            return new Future<NodeState>(NodeState.FAILED);
        });

        Node<Object> alwaysFail2 = new ActionNode<>((event, context) -> 
        {
            System.out.println("Always fail 2...");
            return new Future<NodeState>(NodeState.FAILED);
        });

        CompositeNode<Object> selector = new SelectorNode<>().addChild(alwaysFail1).addChild(alwaysFail2);

        Event event = new Event("start", null);
        IFuture<NodeState> ret = selector.execute(event);

        NodeState state = ret.get();
        assertEquals(NodeState.FAILED, state, "node state");
    }

    @Test
    public void testSelectorAbort() 
    {
        Node<Object> alwaysFail = new ActionNode<>((event, context) -> 
        {
            System.out.println("Always fail...");
            return new Future<>();
            // Simulate a running action
        });

        Node<Object> alwaysSucceed = new ActionNode<>((event, context) -> 
        {
            System.out.println("Always succeed...");
            return new Future<NodeState>(NodeState.SUCCEEDED);
        });

        CompositeNode<Object> selector = new SelectorNode<>().addChild(alwaysFail).addChild(alwaysSucceed);

        Event event = new Event("start", null);
        IFuture<NodeState> ret = selector.execute(event);

        selector.abort();

        NodeState state = ret.get();
        assertEquals(NodeState.FAILED, state, "node state");
    }
}