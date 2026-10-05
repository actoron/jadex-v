package jadex.bding.impl.planbody;

import jadex.bding.IBDINGAgentFeature;
import jadex.bding.IPlanStep;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;


public class SubgoalStep extends PlanStep
{
    protected String goal;
    protected String resultmapping;

    public SubgoalStep(String goal, String resultmapping)
    {
        super("subgoalstep_" + goal, null, resultmapping);
        this.goal = goal;
        this.resultmapping = resultmapping;
    }

    @Override
    public IFuture<PlanStepExecution> execute(IComponent component, PlanExecutionContext context)
    {
        Future<PlanStepExecution> ret = new Future<>();

        PlanStepExecution exe = new PlanStepExecution(this, context.getParameters());

        try
        {
            IBDINGAgentFeature feature = component.getFeature(IBDINGAgentFeature.class);

            feature.dispatchSubgoal(goal, context.getPlan()).then(subgoal ->
            {
                subgoal.getFinished().then(result ->
                {
                    try
                    {
                        if(resultmapping != null)
                            context.set(resultmapping, result);

                        finish(exe, ret, context);
                    }
                    catch(Exception ex)
                    {
                        fail(exe, ret, context, ex);
                    }
                })
                .catchEx(ex ->
                {
                    fail(exe, ret, context, ex);
                });
            })
            .catchEx(ex ->
            {
                fail(exe, ret, context, ex);
            });
        }
        catch(Exception ex)
        {
            fail(exe, ret, context, ex);
        }

        return ret;
    }

    protected void finish(PlanStepExecution exe, Future<PlanStepExecution> ret, PlanExecutionContext context)
    {
        exe.setOutputs(context.getParameters()).setState(IPlanStep.PlanStepState.SUCCEEDED);
        ret.setResult(exe);
    }

    protected void fail(PlanStepExecution exe, Future<PlanStepExecution> ret, PlanExecutionContext context, Exception ex)
    {
        exe.setException(ex).setOutputs(context.getParameters()).setState(IPlanStep.PlanStepState.FAILED);
        ret.setResult(exe);
    }

    public String getGoal()
    {
        return goal;
    }

    public String getId()
    {
        return id;
    }

    @Override
    public int hashCode()
    {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
            return true;

        if(obj == null || getClass() != obj.getClass())
            return false;

        SubgoalStep other = (SubgoalStep)obj;

        return id == null ? other.id == null : id.equals(other.id);
    }
}

