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
    public IFuture<PlanStepExecution> execute(
        IComponent component, PlanExecutionContext context)
    {
        Future<PlanStepExecution> ret = new Future<>();

        // An absent container represents an empty sequence.
        if(container == null)
        {
            ret.setResult(null);
            return ret;
        }

        // Initialize plan variables before executing any steps.
        Map<String, Object> inits = container.getInits();
        if(inits != null && !inits.isEmpty())
        {
            for(Map.Entry<String, Object> entry : inits.entrySet())
            {
                String name = entry.getKey();

                // Ignore invalid initialization entries defensively.
                if(name == null || name.isBlank())
                    continue;

                String iname = name.startsWith("plan.")
                    ? name
                    : "plan." + name;

                if(!context.getParameters().containsKey(iname))
                    context.set(iname, entry.getValue());
            }

            System.out.println("context with inits is: "
                + context.getParameters());
        }

        executeStep(component, context, 0, ret);

        return ret;
    }

    protected void executeStep(
        IComponent component,
        PlanExecutionContext context,
        int index,
        Future<PlanStepExecution> ret)
    {
        // A missing container or empty sequence is a no-op.
        if(container == null)
        {
            ret.setResult(null);
            return;
        }

        List<StrategicStep> steps = container.getSteps();

        if(steps == null || index >= steps.size())
        {
            ret.setResult(null);
            return;
        }

        StrategicStep sstep = steps.get(index);

        // A null step is treated as an empty entry and skipped.
        if(sstep == null)
        {
            executeStep(component, context, index + 1, ret);
            return;
        }

        sstep.getExecutableStep(
            component.getFeature(IBDINGAgentFeature.class).getReasoner(),
            context.getPlan(),
            context.getParameters())
        .then(step ->
        {
            if(step == null)
            {
                NullPointerException ex = new NullPointerException(
                    "Strategic step returned null executable step: " + sstep);

                PlanStepExecution failexe =
                    new PlanStepExecution(this, context.getParameters());

                context.getPlan().addExecutedStep(
                    failexe.setOutputs(context.getParameters())
                        .setState(PlanStepState.FAILED)
                        .setException(ex));

                ret.setException(ex);
                return;
            }

            PlanStepExecution failexe =
                new PlanStepExecution(this, context.getParameters());

            step.execute(component, context)
            .then(exe ->
            {
                if(exe == null)
                {
                    NullPointerException ex = new NullPointerException(
                        "Plan step returned null execution: " + sstep);

                    context.getPlan().addExecutedStep(
                        failexe.setOutputs(context.getParameters())
                            .setState(PlanStepState.FAILED)
                            .setException(ex));

                    ret.setException(ex);
                    return;
                }

                context.getPlan().addExecutedStep(exe);

                if(exe.getState() == IPlanStep.PlanStepState.FAILED)
                {
                    ret.setException(exe.getException());
                    return;
                }

                RPlan.writeBackContext(
                    context, context.getPlan().getIntention().getGoal(),
                    component);

                executeStep(component, context, index + 1, ret);
            })
            .catchEx(ex ->
            {
                context.getPlan().addExecutedStep(
                    failexe.setOutputs(context.getParameters())
                        .setState(PlanStepState.FAILED)
                        .setException(ex));

                ret.setException(ex);
            });
        })
        .catchEx(ret);
    }

    @Override
    public Map<String, String> getInputMapping()
    {
        return null;
    }

    @Override
    public String getResultMapping()
    {
        return null;
    }
}

