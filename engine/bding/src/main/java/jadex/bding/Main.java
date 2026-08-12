package jadex.bding;

import jadex.core.IComponent;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;

import jadex.bding.annotation.BDINGAgent;
import jadex.core.impl.ComponentManager;

/** 

    Goal      = desired world state ("what")
    Intention = strategic approach ("how at a conceptual level")
    Plan      = concrete operationalization ("how exactly")
    PlanBody  = executable realization

    Goal:
        Deliver package

    Intentions:
        Use drone

        Use truck

        Ask another agent

    Plans:
        Fly with drone XYZ from A to B
        
        Request drone service from provider X
        
        Use autonomous drone fleet

    Goal
    |
    | "Was soll erreicht werden?"
    v
    Intention
    |
    | "Welche Strategie verfolge ich?"
    v
    Plan
    |
    | "Wie setze ich diese Strategie konkret um?"
    v
    PlanBody

    Goal
    |
    RGoal
    |
    adopt()
    |
    Brain.generateIntentions()
    |
    Brain.selectIntention()
    |
    RIntention
    |
    adopt()
    |
    Brain.generatePlans()
    |
    Brain.selectPlan()
    |
    RPlan
    |
    execute()

*/    
public class Main 
{
    @BDINGAgent
    public static class HelloAgent
    {
        @Inject
        protected IComponent agent;
        
        @OnStart
        protected void onStart()
        {
            System.out.println("Hello from agent " + agent.getId()+" "+agent.getClass().getName());

            agent.getFeature(IBDINGAgentFeature.class).dispatchTopLevelGoal(new Goal("Deliver package"));

            agent.terminate();
        }
    }

    public static void main(String[] args) 
    {
        System.out.println("Starting test...");

        ComponentManager.get().create(new HelloAgent()).get();

        ComponentManager.get().waitForLastComponentTerminated();
    }
}
