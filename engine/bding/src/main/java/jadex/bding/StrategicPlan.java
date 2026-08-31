package jadex.bding;

import java.util.List;

public class StrategicPlan 
{
    public enum StepType
    {
        TOOL,
        REASONING,
        SUBGOAL
    }

    protected List<StrategicStep> steps;

    public StrategicPlan()
    {
    }

    public StrategicPlan(List<StrategicStep> steps)
    {
        this.steps = steps;
    }

    public List<StrategicStep> getSteps() 
    {
        return steps;
    }

    public void setSteps(List<StrategicStep> steps) 
    {
        this.steps = steps;
    }
}