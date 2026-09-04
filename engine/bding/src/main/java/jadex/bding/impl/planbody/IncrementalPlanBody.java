package jadex.bding.impl.planbody;

import java.util.ArrayList;
import java.util.List;

import jadex.bding.IBDINGAgentFeature;
import jadex.bding.IPlanBody;
import jadex.bding.IPlanStep;
import jadex.bding.IReasoner;
import jadex.bding.ModelElement;
import jadex.bding.StrategicPlan;
import jadex.bding.StrategicStep;
import jadex.bding.impl.RPlan;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;

public class IncrementalPlanBody extends ModelElement implements IPlanBody
{
    protected StrategicPlan splan;

    protected List<IPlanStep> steps = new ArrayList<>();

    public IncrementalPlanBody(StrategicPlan splan)
    {
        super("incrementalplanbody", null);
        this.splan = splan;
    }

    public StrategicPlan getStrategicPlan()
    {
        return splan;
    }

    public List<IPlanStep> getSteps()
    {
        return steps;
    }

    public void setSteps(List<IPlanStep> steps) 
    {
        this.steps = steps;
    }

    @Override
    public IFuture<Void> execute(IComponent component, PlanExecutionContext context)
    {
        Future<Void> ret = new Future<>();

        executeSteps(component, context, 0, ret);

        return ret;
    }

    protected void executeSteps(IComponent component, PlanExecutionContext context, int index, Future<Void> ret)
    {
        if(index >= splan.getSteps().size())
        {
            ret.setResult(null);
            return;
        }

        StrategicStep sstep = splan.getSteps().get(index);

        // Use already generated step if available.
        if(index < steps.size())
        {
            executeStep(component, context, index, steps.get(index), ret);
            return;
        }

        try
        {
            IReasoner reasoner = component.getFeature(IBDINGAgentFeature.class).getReasoner();

            reasoner.generatePlanStep(context.getPlan(),sstep, context.getParameters()).then(step ->
            {
                if(step == null)
                {
                    PlanStepExecution exe = new PlanStepExecution(null);
                    exe.setException(new RuntimeException("Reasoner generated no plan step for strategic step '"+ sstep.getName() + "'"));

                    context.getPlan().addExecutedStep(exe);
                    ret.setException(exe.getException());
                    return;
                }

                steps.add(step);

                executeStep(component, context, index, step, ret);
            })
            .catchEx(ret);
        }
        catch(Exception e)
        {
            ret.setException(e);
        }
    }

    protected void executeStep(IComponent component, PlanExecutionContext context, int index, IPlanStep step, Future<Void> ret)
    {
        step.execute(component, context).then(exe ->
        {
            context.getPlan().addExecutedStep(exe);

            if(exe.getState() == IPlanStep.PlanStepState.FAILED)
            {
                ret.setException(exe.getException());
                return;
            }

            RPlan.writeBackContext(context, context.getPlan().getIntention().getGoal(), component);

            executeSteps(component, context, index + 1, ret);
        })
        .catchEx(ex ->
        {
            // Should not happen: step failures are represented by
            // PlanStepExecution.
            System.out.println("Step return exception, should not happen: " + ex);

            PlanStepExecution exe = new PlanStepExecution(step);
            exe.setException(ex);

            context.getPlan().addExecutedStep(exe);
            ret.setException(ex);
        });
    }


}