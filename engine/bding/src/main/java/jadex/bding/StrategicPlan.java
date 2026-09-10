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

    //protected boolean retryAllowed;

    //protected int maxRetries;

    public StrategicPlan()
    {
    }

    public StrategicPlan(List<StrategicStep> steps)//, boolean retryAllowed, int maxRetries)
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

    /*public boolean isRetryAllowed() 
    {
        return retryAllowed;
    }

    public void setRetryAllowed(boolean retryAllowed) 
    {
        this.retryAllowed = retryAllowed;
    }

    public int getMaxRetries() 
    {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) 
    {
        this.maxRetries = maxRetries;
    }*/

    
}