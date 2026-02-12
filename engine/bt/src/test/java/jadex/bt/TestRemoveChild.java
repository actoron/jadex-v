package jadex.bt;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadex.bt.actions.TerminableUserAction;
import jadex.bt.decorators.ChildCreationDecorator;
import jadex.bt.nodes.ActionNode;
import jadex.bt.nodes.CompositeNode;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.nodes.ParallelNode;
import jadex.common.SUtil;
import jadex.core.ChangeEvent;
import jadex.core.ChangeEvent.Type;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.future.TerminableFuture;
import jadex.injection.annotation.OnEnd;
import jadex.injection.annotation.OnStart;
import jadex.injection.annotation.ProvideResult;

public class TestRemoveChild 
{
    public static class RemoveChildAgent implements IBTProvider
    {     
        protected List<String> items = new ArrayList<>();

        @ProvideResult
        protected Boolean finalstate;

        @OnStart
        public void start(IComponent agent)
        {
            /*SwingUtilities.invokeLater(()->
            {
                new BTViewer(agent.getComponentHandle()).setVisible(true);
            });*/

            //int cnt = 0;
            //while(true)
            for(int i=0; i<5; i++)
            {
                agent.getFeature(IExecutionFeature.class).waitForDelay(100).get();
                items.add("item_"+i);
                System.out.println("Items: "+items);
            }

            agent.getFeature(IExecutionFeature.class).waitForDelay(10000).get();

            finalstate = items.isEmpty();
            System.out.println("Final state: "+finalstate);

            agent.terminate();
        }

        public RemoveChildAgent()
        {    
        }

        @OnEnd
        public void end(IComponent agent)
        {
           System.out.println("AbortAgent ended: "+agent.getId());
        }

        @Override
        public Node<IComponent> createBehaviorTree()
        {
            try
            {
                CompositeNode<IComponent> root = new ParallelNode<>();

                root.addDecorator(new ChildCreationDecorator<IComponent>()
                    .setCondition((node, state, context) -> true)
                    .setEvents(new ChangeEvent(Type.ADDED, "items"))
                    .setChildCreator((event) -> 
                    {   
                        String item = (String)event.value();
                        System.out.println("Creating child for: "+item);
                        ActionNode<IComponent> an = new ActionNode<>("action_"+item);
                        an.setAction(new TerminableUserAction<IComponent>((e, agent) ->
                        {
                            TerminableFuture<NodeState> ret = new TerminableFuture<>();

                            System.out.println("Action starts ...: "+item);
                
                            agent.getFeature(IExecutionFeature.class).waitForDelay(300).then(Void -> 
                            {
                                System.out.println("Action 1 success ...");
                                
                                ret.setResultIfUndone(NodeState.SUCCEEDED);

                                items.remove(item);
                            });

                            return ret;
                        }));
                        return an;
                    }));


                /*root.addNodeListener(new NodeListener<IComponent>()
                {
                    @Override
                    public void onSucceeded(Node<IComponent> node, ExecutionContext<IComponent> context) 
                    {
                        System.out.println("succeeded");
                        finalstate = true;
                    }
                    
                    public void onFailed(Node<IComponent> node, ExecutionContext<IComponent> context) 
                    {
                        System.out.println("failed");
                        finalstate = false;
                    }
                });*/

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
    public void testCreate() 
    {
        //System.out.println("1");
        IComponentHandle comp = IComponentManager.get().create(new RemoveChildAgent()).get();

        //System.out.println("2");
        IComponentManager.get().waitForLastComponentTerminated();
       
        Boolean finalstate = (Boolean)comp.getResults().get().get("finalstate");

        assertTrue(finalstate);
    }

    
    public static void main(String[] args) 
    {
        TestRemoveChild test = new TestRemoveChild();

        //for(int i=0; i<100; i++)
        {
            System.out.println("...........................");
            //System.out.println("Test run "+i+":");
            test.testCreate();
        }

        System.out.println("TestCreate finished.");
    }

}
