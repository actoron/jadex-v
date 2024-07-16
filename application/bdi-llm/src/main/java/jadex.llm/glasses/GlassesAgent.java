package jadex.llm.glasses;

import jadex.bdi.annotation.*;
import jadex.bdi.llm.ILlmFeature;
import jadex.bdi.model.BDIModelLoader;
import jadex.bdi.runtime.IBDIAgentFeature;
import jadex.bdi.runtime.IPlan;
import jadex.core.IComponent;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;

import java.util.ArrayList;
import java.util.List;


@Agent(type = "bdi")
public class GlassesAgent
{
    /** The bdi agent. */
    @Agent
    protected IComponent agent;

    static
    {
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

    @Belief
    public ILlmFeature llmFeature;

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
        System.out.println("executeGeneratedPlan is called");
        if (llmFeature == null)
        {
            System.out.println("No LLM feature found");
            return;
        }

        List<Glasses> glassesList = getGlassesList();
        if (glassesList == null)
        {
            System.out.println("GlassesList is empty");
            return;
        }

        try
        {
            System.out.println("Execute plan step: " + glassesList.size() + " glasses");
            //llmFeature.generatePlanStep(new SortGlassesListGoal(), context, glassesList);
        }
        catch (Exception e)
        {
            System.out.println("Failed to generate plan step: " + e.getMessage());
        }
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
        System.out.println("HelloFeature");
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
                llmFeature.readClassStructure(Glasses.class);
                llmFeature.readClassStructure(GlassesAgent.class);

                //llmFeature.connectToLLM(cls, obj);

            } else
            {
                System.out.println("Nope, Feature not found");
            }
        } catch (Exception e) {
            System.out.println("Failed to initialize LLM feature: " + e.getMessage());
        }
        agent.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(new SortGlassesListGoal()).get();
        System.out.println("GlassesAgent finished");
    }
}
