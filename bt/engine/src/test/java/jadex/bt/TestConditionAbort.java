package jadex.bt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;

import jadex.bt.actions.TerminableUserAction;
import jadex.bt.decorators.FailureDecorator;
import jadex.bt.decorators.SuccessDecorator;
import jadex.bt.nodes.ActionNode;
import jadex.bt.nodes.CompositeNode;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.nodes.ParallelNode;
import jadex.bt.nodes.SelectorNode;
import jadex.bt.nodes.SequenceNode;
import jadex.bt.state.ExecutionContext;
import jadex.common.SUtil;
import jadex.core.ChangeEvent;
import jadex.core.ChangeEvent.Type;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.execution.impl.ComponentTimerCreator;
import jadex.future.TerminableFuture;
import jadex.injection.Dyn;
import jadex.injection.annotation.OnEnd;
import jadex.injection.annotation.OnStart;
import jadex.injection.annotation.ProvideResult;

public class TestConditionAbort 
{
    public static class AbortAgent implements IBTProvider
    {
        protected Dyn<Long> curtime = new Dyn<>(() -> System.currentTimeMillis()).setUpdateRate(500);
        protected long starttime = System.currentTimeMillis();
        
        /** Will be aborted with success or failure */
        protected boolean success;

        /** Which node type is used for abort */
        protected Class<?> nodetype;

        @ProvideResult
        protected Boolean finalstate;

        /** How long needs an action */
        protected long actiondur;

        /** How long to abort */
        protected long abortdur;    

        @OnStart
        public void start(IComponent agent)
        {
            /*SwingUtilities.invokeLater(()->
            {
                new BTViewer(agent.getComponentHandle()).setVisible(true);
            });*/
        }

        public AbortAgent(boolean success, Class<?> nodetype)
        {
            this(success, nodetype, 2000, 3000);     
        }

        public AbortAgent(boolean success, Class<?> nodetype, long actiondur, long abortdur)
        {
            this.success = success;
            this.nodetype = nodetype;   
            this.actiondur = actiondur;
            this.abortdur = abortdur;     
        }

        @OnEnd
        public void end(IComponent agent)
        {
           System.out.println("AbortAgent ended: "+agent.getId());
        }

        @Override
        public ExecutionContext<IComponent> createExecutionContext(IComponent component)
        {
            ExecutionContext<IComponent> exe = new ExecutionContext<IComponent>(component, new ComponentTimerCreator());

            exe.addNodeListener("root", new NodeListener<IComponent>()
            {
                @Override
                public void onSucceeded(Node<IComponent> node, ExecutionContext<IComponent> context) 
                {
                    //System.out.println("succeeded");
                    finalstate = true;
                }
                
                public void onFailed(Node<IComponent> node, ExecutionContext<IComponent> context) 
                {
                    //System.out.println("failed");
                    finalstate = false;
                }
            });

            return exe;
        }

        @Override
        public Node<IComponent> createBehaviorTree()
        {
            ActionNode<IComponent> action1 = new ActionNode<>(new TerminableUserAction<>((event, agent) -> 
            {
                System.out.println("Action 1 starts ...");
                TerminableFuture<NodeState> ret = new TerminableFuture<NodeState>();
                
                agent.getFeature(IExecutionFeature.class).waitForDelay(actiondur).then(Void -> 
                {
                    System.out.println("Action 1 success ...");
                    ret.setResultIfUndone(NodeState.SUCCEEDED);
                });

                return ret;
            }));

            ActionNode<IComponent> action2 = new ActionNode<>(new TerminableUserAction<>((event, agent) -> 
            {
                System.out.println("Action 2 starts ...");
                TerminableFuture<NodeState> ret = new TerminableFuture<NodeState>();
                
                agent.getFeature(IExecutionFeature.class).waitForDelay(actiondur).then(Void -> 
                {
                    System.out.println("Action 2 success ...");
                    ret.setResultIfUndone(NodeState.SUCCEEDED);
                });

                return ret;
            }));

            try
            {
                CompositeNode<IComponent> root = (CompositeNode<IComponent>)nodetype.getDeclaredConstructor().newInstance();
                root.setName("root");
                root.addChild(action1).addChild(action2);

                if(success)
                {
                    root.addDecorator(new SuccessDecorator<IComponent>()
                        .setCondition((node, state, context) -> 
                        {
                            //System.out.println("Checking success condition: "+(curtime.get() - starttime > abortdur));
                            return curtime.get() - starttime > abortdur;
                        })
                        .setEvents(new ChangeEvent(Type.CHANGED, "curtime")));
                }
                else
                {
                    root.addDecorator(new FailureDecorator<IComponent>()
                        .setCondition((node, state, context) -> 
                        {
                            //System.out.println("Checking failure condition: "+(curtime.get() - starttime > abortdur));
                            return curtime.get() - starttime > abortdur;
                        })
                        .setEvents(new ChangeEvent(Type.CHANGED, "curtime")));
                }

                return root;
            }
            catch(Exception e)
            {
                e.printStackTrace();
                SUtil.rethrowAsUnchecked(e);
            }
            return null;
        }
    }

    @Test
    public void testAbortSuccessSeq() 
    {
        //System.out.println("1");
        IComponentHandle comp = IComponentManager.get().create(new AbortAgent(true, SequenceNode.class)).get();

        //System.out.println("2");
        IComponentManager.get().waitForLastComponentTerminated();
       
        Boolean finalstate = (Boolean)comp.getResults().get().get("finalstate");

        assertNotNull(finalstate);
        assertTrue(finalstate);
    }

    @Test
    public void testAbortFailureSeq() 
    {
        //System.out.println("1");
        IComponentHandle comp = IComponentManager.get().create(new AbortAgent(false, SequenceNode.class)).get();

        //System.out.println("2");
        IComponentManager.get().waitForLastComponentTerminated();

        Boolean finalstate = (Boolean)comp.getResults().get().get("finalstate");
        System.out.println("finalstate: "+finalstate);

        assertNotNull(finalstate);
        assertFalse(finalstate);
    }

    @Test
    public void testAbortSuccessSel() 
    {
        //System.out.println("1");
        IComponentHandle comp = IComponentManager.get().create(new AbortAgent(true, SelectorNode.class)).get();

        //System.out.println("2");
        IComponentManager.get().waitForLastComponentTerminated();
       
        Boolean finalstate = (Boolean)comp.getResults().get().get("finalstate");

        assertNotNull(finalstate);
        assertTrue(finalstate);
    }

    @Test
    public void testAbortFailureSel1() 
    {
        //System.out.println("1");
        IComponentHandle comp = IComponentManager.get().create(new AbortAgent(false, SelectorNode.class)).get();

        //System.out.println("2");
        IComponentManager.get().waitForLastComponentTerminated();

        Boolean finalstate = (Boolean)comp.getResults().get().get("finalstate");
        System.out.println("finalstate: "+finalstate);

        // should succeed as condition triggers after first action ends
        assertNotNull(finalstate);
        assertTrue(finalstate);
    }

    @Test
    public void testAbortFailureSel2() 
    {
        IComponentHandle comp = IComponentManager.get().create(new AbortAgent(false, SelectorNode.class, 2000, 1000)).get();

        //System.out.println("2");
        IComponentManager.get().waitForLastComponentTerminated();

        Boolean finalstate = (Boolean)comp.getResults().get().get("finalstate");
        System.out.println("finalstate: "+finalstate);

        // should fail as condition triggers before first action ends
        assertNotNull(finalstate);
        assertFalse(finalstate);
    }

    @Test
    public void testAbortSuccessPar() 
    {
        //System.out.println("1");
        IComponentHandle comp = IComponentManager.get().create(new AbortAgent(true, ParallelNode.class)).get();

        //System.out.println("2");
        IComponentManager.get().waitForLastComponentTerminated();
       
        Boolean finalstate = (Boolean)comp.getResults().get().get("finalstate");

        assertNotNull(finalstate);
        assertTrue(finalstate);
    }

    @Test
    public void testAbortFailurePar1() 
    {
        //System.out.println("1");
        IComponentHandle comp = IComponentManager.get().create(new AbortAgent(false, ParallelNode.class)).get();

        //System.out.println("2");
        IComponentManager.get().waitForLastComponentTerminated();

        Boolean finalstate = (Boolean)comp.getResults().get().get("finalstate");
        System.out.println("finalstate: "+finalstate);

        assertNotNull(finalstate);
        assertTrue(finalstate);
    }

    @Test
    public void testAbortFailurePar2() 
    {
        //System.out.println("1");
        IComponentHandle comp = IComponentManager.get().create(new AbortAgent(false, ParallelNode.class, 3000, 2000)).get();

        //System.out.println("2");
        IComponentManager.get().waitForLastComponentTerminated();

        Boolean finalstate = (Boolean)comp.getResults().get().get("finalstate");
        System.out.println("finalstate: "+finalstate);

        assertNotNull(finalstate);
        assertFalse(finalstate);  
    }

    public static void main(String[] args) 
    {
        TestConditionAbort test = new TestConditionAbort();

        //for(int i=0; i<100; i++)
        //{
            System.out.println("...........................");
            //System.out.println("Test run "+i+":");
            test.testAbortFailureSel2();
        //}
       

        System.out.println("TestConditionAbort finished.");
    }

}
