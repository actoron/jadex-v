package jadex.llm.glasses;

import jadex.bdi.annotation.*;
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
    static
    {
        System.out.println("GlassesAgent class loaded");
    }

    @Belief
    private final SortBy characteristic = SortBy.SHAPE;

    public enum SortBy
    {
        SHAPE, MATERIAL, COLOUR, BRAND, MODEL
    }

    @Belief
    private List<Glasses> glassesList = new ArrayList<>();
    private GlassesSortingGoal sortingGoal;
    private ILlmFeature llmFeature;

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
        System.out.println("executeGeneratedPlan is called");

        SortGlassesListGoal sortingGoal = new SortGlassesListGoal();
        List<Glasses> glassesList = getGlassesList();

        llmFeature.generateAndExecutePlanStep(this, sortingGoal, glassesList);
    }

    private List<Glasses> getGlassesList()
    {
        return null;
    }


    @OnStart
    public void body(IComponent agent)
    {
        System.out.println("GlassesAgent active");

        // call method from Glasses
        glassesList = Glasses.generateRandomGlassesList();
        sortingGoal = new GlassesSortingGoal(characteristic);
        // call method from class reader
        BDIModelLoader loader = new BDIModelLoader();
        ClassReader classReader = new ClassReader(loader);
        classReader.readAllClassStructure();

        agent.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(new SortGlassesListGoal()).get();
        System.out.println("GlassesAgent finished");
    }
}
