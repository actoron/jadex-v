package jadex.bding.impl.planbody;

import java.util.Map;

import jadex.bding.IBDINGAgentFeature;
import jadex.bding.ICondition;
import jadex.bding.IPlanStep;
import jadex.bding.IPlanStepContainer;
import jadex.bding.impl.RPlan;
import jadex.bding.impl.planbody.strategic.StrategicLoopContainer;
import jadex.common.IValueFetcher;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.javaparser.SJavaParser;

public class LoopPlanStepContainer implements IPlanStepContainer, IPlanStep
{
    protected ICondition condition;

    protected StrategicLoopContainer container;

    public LoopPlanStepContainer(StrategicLoopContainer container)
    {
        this.container = container;
    }

    @Override
    public IFuture<PlanStepExecution> execute(IComponent component, PlanExecutionContext context)
    {
        Future<PlanStepExecution> ret = new Future<>();

        PlanStepExecution loopexe = new PlanStepExecution(this, context.getParameters());

        this.condition = PlanStep.createCondition(container.getCondition(), component.getFeature(IBDINGAgentFeature.class).getModel(), 
            component.getFeature(IBDINGAgentFeature.class).getReasoner());

        int count = 0;
        int max = -1;
        if(container.getMax()!=null)
            max = (Integer)PlanStep.evaluateExpression(container.getMax(), context.getParameters());

        executeLoop(component, context, loopexe, ret, count, max);

        return ret;
    }

    protected void executeLoop(IComponent component, PlanExecutionContext context, PlanStepExecution loopexe, Future<PlanStepExecution> ret, int count, int max)
    {
        if(max >= 0 && count >= max)
        {
            finishLoop(loopexe, ret, context);
            return;
        }

        condition.evaluate(context.getParameters()).then(result ->
        {
            if(!result.booleanValue())
            {
                finishLoop(loopexe, ret, context);
                return;
            }

            container.getExecutableStep(component.getFeature(IBDINGAgentFeature.class).getReasoner(),
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

                    RPlan.writeBackContext(context, context.getPlan().getIntention().getGoal(), component);

                    executeLoop(component, context, loopexe, ret, count+1, max);
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

    protected void failLoop(PlanStepExecution loopexe, Future<PlanStepExecution> ret, PlanExecutionContext context, Exception ex)
    {
        loopexe.setState(IPlanStep.PlanStepState.FAILED);
        loopexe.setException(ex);
        finishLoop(loopexe, ret, context);
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