package jadex.llm.glasses;

import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.Plan;
import jadex.bdi.llm.impl.LlmFeature;
import jadex.bdi.runtime.IPlan;
import jadex.core.IComponent;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Description;
import jadex.model.annotation.OnEnd;
import jadex.model.annotation.OnStart;


@Agent(type="bdip")
@Description("This agent uses ChatGPT to create the plan step.")
public class GlassesAgent
{
    /** The Glasses agent class. */
    @Agent
    protected IComponent agent;

    private final String chatUrl;
    private final String apiKey;
    private final String agentClassName;
    private final String featureClassName;

    /** Constructor */
    public GlassesAgent(String chatUrl, String apiKey, String agentClassName, String featureClassName)
    {
        this.chatUrl = chatUrl;
        this.apiKey = apiKey;
        this.agentClassName = agentClassName;
        this.featureClassName = featureClassName;

        System.out.println("A: " + chatUrl);
        System.out.println("A: " + apiKey);
        System.out.println("A: " + agentClassName);
        System.out.println("A: " + featureClassName);

        System.out.println("A: GlassesAgent class loaded");
        //Annotation

        //read Dateset jsonarray im constructor laden und befüllen
    }
//    @Belief
//    public JsonArray = null

//    @Goal

    @Plan

    @OnStart
    public void body()
    {
        System.out.println("A: Agent " +agent.getId()+ " active");

        /** Initialize the LlmFeature */
        LlmFeature llmFeature = new LlmFeature(
                chatUrl,
                apiKey,
                agentClassName,
                featureClassName);

//        String javacode = llmFeature.connectToLLM("Hello, World!");
//        llmFeature.generateAndInterpretPlanStep(javacode);
        //Ausgabe SclassReader
        llmFeature.readClassStructure(agentClassName, featureClassName);

//        String javacode = "class Plan { static doPlanStep() { print('Hello World JavaScript'); } };";
//        llmFeature.generateAndInterpretPlanStep(javacode);


//        try {
//            Method doPlanStep = PlanStep.getMethod("doPlanStep");
//            try {
//                doPlanStep.invoke(PlanStep.getDeclaredConstructor().newInstance());
//            } catch (IllegalAccessException e) {
//                throw new RuntimeException(e);
//            } catch (InvocationTargetException e) {
//                throw new RuntimeException(e);
//            } catch (InstantiationException e) {
//                throw new RuntimeException(e);
//            }
//        } catch (NoSuchMethodException e) {
//            throw new RuntimeException(e);
//        }



        agent.terminate();
    }

    @OnEnd
    public void end()
    {
        System.out.println("A: Agent "+agent.getId()+ " terminated");
    }

//    @Plan(trigger = @Trigger(goals = SortGlassesListGoal.class))
    public void executeGeneratedPlan(IPlan context)
    {
        // java code = llmFeature.generatePlanStep()
        System.out.println("A: executeGeneratedPlan");
    }

    /**
     *  Start Glasses Agent.
     * @throws InterruptedException
     */
    public static void main(String[] args)
    {
        System.out.println("A: GlassesAgent started");

        IComponent.create(new GlassesAgent(
                "https://api.openai.com/v1/chat/completions",
                System.getenv("OPENAI_API_KEY"),
                "jadex.llm.glasses.GlassesAgent",
                "jadex.llm.glasses.Glasses")
        );
        IComponent.waitForLastComponentTerminated();
    }
}
