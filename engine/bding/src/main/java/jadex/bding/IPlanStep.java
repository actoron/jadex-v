package jadex.bding;

import java.util.Map;

import jadex.bding.impl.planbody.PlanExecutionContext;
import jadex.bding.impl.planbody.PlanStepExecution;
import jadex.core.IComponent;
import jadex.future.IFuture;

public interface IPlanStep
{
    public enum PlanStepState
    {
        RUNNING,
        SUCCEEDED,
        FAILED
    }

    public IFuture<PlanStepExecution> execute(IComponent agent, PlanExecutionContext context);

    public Map<String, String> getParameterMapping();

    public String getResultMapping();
}