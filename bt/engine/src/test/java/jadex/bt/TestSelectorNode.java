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
		Node alwaysSucceed = new ActionNode(event -> 
		{
			System.out.println("Always succeed...");
			return new Future<NodeState>(NodeState.SUCCEEDED);
        });

        Node alwaysFail = new ActionNode(event -> 
        {
        	System.out.println("Always fail...");
        	return new Future<NodeState>(NodeState.FAILED);
        });

        CompositeNode selector = new SelectorNode().addChild(alwaysSucceed).addChild(alwaysFail);

        Event event = new Event("start", null);
        IFuture<NodeState> ret = selector.execute(event);

        NodeState state = ret.get();
        assertEquals(NodeState.SUCCEEDED, state, "node state");
	}

    @Test
    public void testSelectorSuccessOnSecond() 
    {
        Node alwaysFail = new ActionNode(event -> 
        {
            System.out.println("Always fail...");
            return new Future<NodeState>(NodeState.FAILED);
        });

        Node alwaysSucceed = new ActionNode(event -> 
        {
            System.out.println("Always succeed...");
            return new Future<NodeState>(NodeState.SUCCEEDED);
        });

        CompositeNode selector = new SelectorNode().addChild(alwaysFail).addChild(alwaysSucceed);

        Event event = new Event("start", null);
        IFuture<NodeState> ret = selector.execute(event);

        NodeState state = ret.get();
        assertEquals(NodeState.SUCCEEDED, state, "node state");
    }

    @Test
    public void testSelectorFailure() 
    {
        Node alwaysFail1 = new ActionNode(event -> 
        {
            System.out.println("Always fail 1...");
            return new Future<NodeState>(NodeState.FAILED);
        });

        Node alwaysFail2 = new ActionNode(event -> 
        {
            System.out.println("Always fail 2...");
            return new Future<NodeState>(NodeState.FAILED);
        });

        CompositeNode selector = new SelectorNode().addChild(alwaysFail1).addChild(alwaysFail2);

        Event event = new Event("start", null);
        IFuture<NodeState> ret = selector.execute(event);

        NodeState state = ret.get();
        assertEquals(NodeState.FAILED, state, "node state");
    }

    @Test
    public void testSelectorAbort() 
    {
        Node alwaysFail = new ActionNode(event -> 
        {
            System.out.println("Always fail...");
            return new Future<>();
            // Simulate a running action
        });

        Node alwaysSucceed = new ActionNode(event -> 
        {
            System.out.println("Always succeed...");
            return new Future<NodeState>(NodeState.SUCCEEDED);
        });

        CompositeNode selector = new SelectorNode().addChild(alwaysFail).addChild(alwaysSucceed);

        Event event = new Event("start", null);
        IFuture<NodeState> ret = selector.execute(event);

        selector.abort();

        NodeState state = ret.get();
        assertEquals(NodeState.FAILED, state, "node state");
    }
}