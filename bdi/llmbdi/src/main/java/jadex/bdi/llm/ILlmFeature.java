package jadex.bdi.llm;

public interface ILlmFeature
{
    public void generateAndExecutePlanStep(Goal goal, Plan plan, Object... context);

}
