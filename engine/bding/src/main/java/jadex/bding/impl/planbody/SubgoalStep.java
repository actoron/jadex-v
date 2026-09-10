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
        super("subgoalstep_"+goal, null, resultmapping);
        this.goal = goal;
        this.resultmapping = resultmapping;
    }

   @Override
    public IFuture<PlanStepExecution> execute(IComponent component, PlanExecutionContext context)
    {
        IBDINGAgentFeature feature = component.getFeature(IBDINGAgentFeature.class);

        Future<PlanStepExecution> ret = new Future<>();
        PlanStepExecution exe = new PlanStepExecution(this);

        try
        {
            exe.setInputs(context.getParameters());

            feature.dispatchSubgoal(goal, context.getPlan()).then(subgoal ->
            {
                subgoal.getFinished().then(result ->
                {
                    try
                    {
                        if(resultmapping != null)
                            context.set(resultmapping, result);

                        exe.setOutputs(context.getParameters());
                        exe.setState(IPlanStep.PlanStepState.SUCCEEDED);

                        ret.setResult(exe);
                    }
                    catch(Exception e)
                    {
                        exe.setException(e);
                        exe.setState(IPlanStep.PlanStepState.FAILED);
                        ret.setResult(exe);
                    }
                }).catchEx(ex ->
                {
                    exe.setException(ex);
                    exe.setState(IPlanStep.PlanStepState.FAILED);
                    ret.setResult(exe);
                });
            }).catchEx(ex ->
            {
                exe.setException(ex);
                exe.setState(IPlanStep.PlanStepState.FAILED);
                ret.setResult(exe);
            });
        }
        catch(Exception e)
        {
            exe.setException(e);
            exe.setState(IPlanStep.PlanStepState.FAILED);
            ret.setResult(exe);
        }

        return ret;
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
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) 
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SubgoalStep other = (SubgoalStep) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

}