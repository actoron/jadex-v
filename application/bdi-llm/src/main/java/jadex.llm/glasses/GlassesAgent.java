package jadex.llm.glasses;


import jadex.bdi.llm.impl.LlmFeature;
import jadex.bdi.runtime.IPlan;
import jadex.core.IComponent;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Description;
import jadex.model.annotation.OnEnd;
import jadex.model.annotation.OnStart;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;

@Agent(type="bdip")
@Description("This agent uses ChatGPT to create the plan step.")
public class GlassesAgent
{
    /** The Glasses agent class. */
    @Agent
    protected IComponent agent;

    private final String chatgpt_url;
    private final String api_key;
    private final String agent_class_name;
    private final String feature_class_name;

    /** Constructor */
    public GlassesAgent(String chatgpt_url, String api_key, String agent_class_name, String feature_class_name)
    {
        this.chatgpt_url = chatgpt_url;
        this.api_key = api_key;
        this.agent_class_name = agent_class_name;
        this.feature_class_name = feature_class_name;

        System.out.println("A: " + chatgpt_url);
        System.out.println("A: " + api_key);
        System.out.println("A: " + agent_class_name);
        System.out.println("A: " + feature_class_name);

        System.out.println("A: GlassesAgent class loaded");
    }

    @OnStart
    public void body()
    {
        System.out.println("A: Agent " +agent.getId()+ " active");

        /** Initialize the LlmFeature */
        LlmFeature llmFeature = new LlmFeature(
                chatgpt_url,
                api_key,
                agent_class_name,
                feature_class_name);

        String javacode = llmFeature.connectToLLM("Hello, World!");
        Class<?> PlanStep = llmFeature.generatePlanStep(javacode);

        try {
            Method doPlanStep = PlanStep.getMethod("doPlanStep");
            try {
                doPlanStep.invoke(PlanStep.getDeclaredConstructor().newInstance());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }



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
