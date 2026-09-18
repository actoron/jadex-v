package jadex.bding.impl.planbody;


import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;

public class FailStep extends PlanStep
{
    public FailStep(String id)
    {
        super(id, null, null);
    }

    @Override
    public IFuture<PlanStepExecution> execute(IComponent component, PlanExecutionContext context)
    {
        Future<PlanStepExecution> ret = new Future<>();

        ret.setException(new RuntimeException("Flan failed"));

        return ret;
    }
}