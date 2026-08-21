package jadex.bding;

import jadex.core.IComponent;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;
import jadex.micro.llmcall2.LlmChatAgent;
import jadex.micro.llmcall2.LlmChatAgent2;
import jadex.micro.llmcall2.LlmHelper;
import dev.langchain4j.model.chat.StreamingChatModel;
import jadex.bding.annotation.BDINGAgent;
import jadex.bding.annotation.Model;
import jadex.bding.annotation.Belief;
import jadex.bding.annotation.Reasoner;
import jadex.bding.impl.LlmReasoner;
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

    Goal:      Wach werden
   │
   ├── Intention: Kaffee trinken
   │       │
   │       ├── Plan: Kaffeeautomat benutzen
   │       │
   │       └── Plan: Kaffee im Café kaufen
   │
   └── Intention: kalt duschen
           │
           └── Plan: Dusche einschalten

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
    public static class UniversityAgent
    {
        @Inject
        protected IComponent agent;

        //@Model
        //protected AgentModel model;

        //@Reasoner
        //protected IReasoner reasoner;

        @Belief(description = "Describes if it is currently raining.")
        protected boolean raining;

        @Belief(description = "The amount of money the agent has in euro.") 
        protected double money;

        @Belief(description = "The current location of the agent.")
        protected String location;

        public UniversityAgent()
        {
            this("Hamburg", 20);//, null);
        }

        public UniversityAgent(String location, double money)//, AgentModel model)
        {
            this.location = location;
            this.money = money;
            //this.model = model;
        }
        
        @OnStart
        protected void onStart()
        {
            System.out.println("Hello from agent " + agent.getId()+" "+agent.getClass().getName());

            agent.getFeature(IBDINGAgentFeature.class).dispatchTopLevelGoal(
                "Go now to City University of Applied Sciences in Bremen.");

            agent.terminate();
        }
    }


   public static void main(String[] args) 
    {
        System.out.println("Starting test...");

        StreamingChatModel llm = LlmHelper.createChatModel(LlmHelper.Provider.OLLAMA_REMOTE, "gemma4:31b", false);

        ComponentManager.get().create(new LlmChatAgent2(llm)).get();

        ComponentManager.get().create(new UniversityAgent()).get();

        ComponentManager.get().waitForLastComponentTerminated();
    }
}
