package jadex.bding.impl.planbody;

import java.util.List;
import java.util.Map;

import jadex.bding.IBDINGAgentFeature;
import jadex.bding.IPlanStep;
import jadex.bding.IPlanStepContainer;
import jadex.bding.impl.RPlan;
import jadex.bding.impl.planbody.strategic.StrategicContainer;
import jadex.bding.impl.planbody.strategic.StrategicStep;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;

public class SequentialPlanStepContainer implements IPlanStepContainer, IPlanStep
{
    protected StrategicContainer container;

    public SequentialPlanStepContainer(StrategicContainer container)
    {
        this.container = container;
    }

    @Override
    public IFuture<PlanStepExecution> execute(IComponent component, PlanExecutionContext context)
    {
        Future<PlanStepExecution> ret = new Future<>();

        executeStep(component, context, 0, ret);

        return ret;
    }

    protected void executeStep(IComponent component, PlanExecutionContext context, int index, Future<PlanStepExecution> ret)
    {
        List<StrategicStep> steps = container.getSteps();

        if(index >= steps.size())
        {
            ret.setResult(null);
            return;
        }

        StrategicStep sstep = steps.get(index);

        sstep.getExecutableStep(component.getFeature(IBDINGAgentFeature.class).getReasoner(), context.getPlan(), 
            context.getParameters()).then(step ->
        {
            PlanStepExecution failexe = new PlanStepExecution(this, context.getParameters());

            step.execute(component, context).then(exe ->
            {
                context.getPlan().addExecutedStep(exe);

                if(exe.getState() == IPlanStep.PlanStepState.FAILED)
                {
                    ret.setException(exe.getException());
                    return;
                }

                RPlan.writeBackContext(context, context.getPlan().getIntention().getGoal(), component);

                executeStep(component, context, index + 1, ret);
            })
            .catchEx(ex ->
            {
                context.getPlan().addExecutedStep(failexe.setOutputs(context.getParameters()).setState(PlanStepState.FAILED).setException(ex));

                ret.setException(ex);
            });
        })
        .catchEx(ret);
    }

    @Override
    public Map<String, String> getParameterMapping() 
    {
        return null;
    }

    @Override
    public String getResultMapping() 
    {
        return null;
    }
}