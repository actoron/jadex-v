package jadex.llm.glasses;

import jadex.bdi.annotation.*;
import jadex.bdi.llm.ILlmFeature;
import jadex.bdi.llm.impl.LlmFeature;
import jadex.bdi.runtime.IBDIAgentFeature;
import jadex.bdi.runtime.IPlan;
import jadex.core.IComponent;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Description;
import jadex.model.annotation.OnStart;

import java.util.ArrayList;
import java.util.List;


@Agent(type = "bdi")
@Description("This agent uses ChatGPT to create the plan step.")
public class GlassesAgent
{
    /** The Glasses agent class. */
    // @Agent
    // protected IComponent agent;

    @Belief
    protected ILlmFeature agent_llmfeature;

    //    protected String api_key            = (String)agent.getFeature(IBDIAgentFeature.class).getArgument("api_key");
    //    protected String chatgpt_url        = (String)agent.getFeature(IBDIAgentFeature.class).getArgument("chatgpt_url");
    //    protected String agent_class_name   = (String)agent.getFeature(IBDIAgentFeature.class).getArgument("agent_class_name");
    //    protected String feature_class_name = (String)agent.getFeature(IBDIAgentFeature.class).getArgument("feature_class_name");

    /** Constructor */
    public GlassesAgent(String chatgpt_url, String api_key, String agent_class_name, String feature_class_name) {
        System.out.println(chatgpt_url);
        System.out.println(api_key);
        System.out.println(agent_class_name);
        System.out.println(feature_class_name);

        this.agent_llmfeature = new LlmFeature(
                chatgpt_url,
                api_key,
                agent_class_name,
                feature_class_name);

        System.out.println("GlassesAgent class loaded");
    }

    @Belief
    private List<Glasses> glassesList = new ArrayList<>();

    @Belief
    private SortBy characteristic = SortBy.SHAPE;

    public enum SortBy
    {
        SHAPE, MATERIAL, COLOUR, BRAND, MODEL
    }

    private GlassesSortingGoal sortingGoal;

    public class GlassesSortingGoal
    {
        private SortBy sortBy;

        public GlassesSortingGoal(SortBy sortBy) {
            this.sortBy = sortBy;
        }

        public SortBy getSortBy() {
            return sortBy;
        }
    }

    @Goal
    class SortGlassesListGoal
    {
        @GoalResult
        public List<Glasses> getSortedGlassesList() {
            return glassesList;
        }
    }

    @Plan(trigger = @Trigger(goals = SortGlassesListGoal.class))
    public void executeGeneratedPlan(IPlan context)
    {
//        System.out.println("executeGeneratedPlan is called");
//        if (llmFeature == null)
//        {
//            System.out.println("No LLM feature found");
//            return;
//        }
//
//        List<Glasses> glassesList = getGlassesList();
//        if (glassesList == null)
//        {
//            System.out.println("GlassesList is empty");
//            return;
//        }
//
//        try
//        {
//            System.out.println("Execute plan step: " + glassesList.size() + " glasses");
//            llmFeature.generatePlanStep(new SortGlassesListGoal(), context, glassesList);
//        }
//        catch (Exception e)
//        {
//            System.out.println("Failed to generate plan step: " + e.getMessage());
//        }
    }

    public List<Glasses> getGlassesList()
    {
        if (glassesList == null)
        {
            glassesList = Glasses.generateRandomGlassesList();
        }
        return null;
    }


    @OnStart
    public void body(IComponent agent)
    {
        System.out.println("GlassesAgent active");
        // call method from Glasses
        glassesList = Glasses.generateRandomGlassesList();
        sortingGoal = new GlassesSortingGoal(characteristic);

        // Initialize LLM feature
        System.out.println("Feature initialization started");
        try
        {
            ILlmFeature llmFeature = agent.getFeature(ILlmFeature.class);

            if (llmFeature != null)
            {
                System.out.println("LLM Feature found and initializing");

                List<Class<?>> cls = new ArrayList<>();
                cls.add(GlassesAgent.class);
                cls.add(Glasses.class);

                List<Object> obj = new ArrayList<>();
                obj.add(glassesList);
                obj.add(sortingGoal);

                //Read class structure - Idee: Auslesen Klassen aufgrund gegebenen Pfad
                //SClassReader.ClassInfo classInfo = SClassReader.getClassInfo("", ClassLoader.loadClass());
                //llmFeature.readClassStructure(Glasses.class);
                //llmFeature.readClassStructure(GlassesAgent.class);

                llmFeature.connectToLLM(cls, obj);

            } else
            {
                System.out.println("Nope, Feature not found");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        agent.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(new SortGlassesListGoal()).get();
        System.out.println("GlassesAgent finished");
    }

    /**
     *  Start Glasses Agent.
     * @throws InterruptedException
     */
    public static void main(String[] args)
    {
        System.out.println("GlassesAgent started");

        IComponent.create(new GlassesAgent(
                "https://api.openai.com/v1/chat/completions",
                System.getenv("OPENAI_API_KEY"),
                "jadex.llm.glasses.GlassesAgent",
                "jadex.llm.glasses.Glasses")
        );
        IComponent.waitForLastComponentTerminated();
    }
}
