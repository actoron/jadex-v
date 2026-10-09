package jadex.bding.impl.planbody;

import java.util.List;
import java.util.Map;

import jadex.bding.IBDINGAgentFeature;
import jadex.bding.ICondition;
import jadex.bding.IPlanStep;
import jadex.bding.IPlanStepContainer;
import jadex.bding.impl.RPlan;
import jadex.bding.impl.planbody.strategic.StrategicLoopContainer;
import jadex.bding.impl.planbody.strategic.StrategicStep;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.core.IComponent;

public class LoopPlanStepContainer implements IPlanStepContainer, IPlanStep
{
    protected ICondition condition;

    protected StrategicLoopContainer container;

    public LoopPlanStepContainer(StrategicLoopContainer container)
    {
        this.container = container;
    }

    protected void setLoopParameters(PlanExecutionContext context, int count, int max)
    {
        String prefix = "loop." + container.getName() + ".";

        context.set(prefix + "counter", count);

        if(max >= 0)
        {
            context.set(prefix + "max", max);
        }
    }

    @Override
    public IFuture<PlanStepExecution> execute(IComponent component, PlanExecutionContext context)
    {
        Future<PlanStepExecution> ret = new Future<>();

        // todo: change init names in compiler to: plan.
        Map<String, Object> inits = container.getInits();
        if(inits!=null)
        {
            for(String name: inits.keySet())
            {
                String iname = "plan."+name;
                if(!context.getParameters().containsKey(iname))
                {
                    context.set(iname, inits.get(name));
                }
            }
        }

        PlanStepExecution loopexe = new PlanStepExecution(this, context.getParameters());

        String conexp = container.getCondition();

        if(conexp != null && !conexp.isBlank())
        {
            this.condition = PlanStep.createCondition(conexp, component.getFeature(IBDINGAgentFeature.class).getModel(),
                component.getFeature(IBDINGAgentFeature.class).getReasoner());
        }
        else
        {
            this.condition = null;
        }

        int count = 0;
        int max = -1;

        if(container.getMax() != null)
        {
            max = (Integer)PlanStep.evaluateExpression(container.getMax(), context.getParameters());
        }

        executeLoop(component, context, loopexe, ret, count, max);

        return ret;
    }

    protected void executeLoop(IComponent component, PlanExecutionContext context, PlanStepExecution loopexe,
        Future<PlanStepExecution> ret, int count, int max)
    {
        setLoopParameters(context, count, max);

        // Maximum number of iterations reached.
        if(max >= 0 && count >= max)
        {
            System.out.println("Loop count exit: count="+count+", max="+max);
            finishLoop(loopexe, ret, context);
            return;
        }

        // No condition means: continue the loop.
        if(condition == null)
        {
            executeBody(component, context, loopexe, ret, count, max);
            return;
        }

        // Evaluate loop condition.
        condition.evaluate(context.getParameters()).then(result ->
        {
            if(!result.booleanValue())
            {
                System.out.println("Loop condition exit: "+condition+" "+context.getParameters());
                finishLoop(loopexe, ret, context);
                return;
            }

            executeBody(component, context, loopexe, ret, count, max);
        })
        .catchEx(ex ->
        {
            System.out.println("Loop exception exit: "+ex.getMessage());
            ex.printStackTrace();
            failLoop(loopexe, ret, context, ex);
        });
    }

    protected void executeBody(IComponent component, PlanExecutionContext context, PlanStepExecution loopexe,
        Future<PlanStepExecution> ret, int count, int max)
    {
        executeBodyStep(component, context, loopexe, ret, count, max, 0);
    }

    protected void executeBodyStep(IComponent component, PlanExecutionContext context,
        PlanStepExecution loopexe, Future<PlanStepExecution> ret, int count, int max, int index)
    {
        List<StrategicStep> steps = container.getSteps();

        if(index >= steps.size())
        {
            RPlan.writeBackContext(context, context.getPlan().getIntention().getGoal(), component);

            executeLoop(component, context, loopexe, ret, count + 1, max);

            return;
        }

        StrategicStep sstep = steps.get(index);

        sstep.getExecutableStep(component.getFeature(IBDINGAgentFeature.class).getReasoner(),
            context.getPlan(), context.getParameters())
        .then(step ->
        {
            step.execute(component, context).then(exe ->
            {
                if(exe != null)
                {
                    context.getPlan().addExecutedStep(exe);

                    if(exe.getState() == IPlanStep.PlanStepState.FAILED)
                    {
                        failLoop(loopexe, ret, context, exe.getException());
                        return;
                    }
                }

                executeBodyStep(component, context, loopexe, ret, count, max, index + 1);
            })
            .catchEx(ex ->
            {
                failLoop(loopexe, ret, context, ex);
            });
        })
        .catchEx(ex ->
        {
            failLoop(loopexe, ret, context, ex);
        });
    }
  
    protected void finishLoop(PlanStepExecution loopexe, Future<PlanStepExecution> ret, PlanExecutionContext context)
    {
        loopexe.setOutputs(context.getParameters());
        ret.setResult(loopexe);
    }

    protected void failLoop(PlanStepExecution loopexe, Future<PlanStepExecution> ret,
        PlanExecutionContext context, Exception ex)
    {
        loopexe.setState(IPlanStep.PlanStepState.FAILED);
        loopexe.setException(ex);
        finishLoop(loopexe, ret, context);
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