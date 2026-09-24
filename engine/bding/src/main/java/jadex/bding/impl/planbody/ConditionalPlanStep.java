package jadex.bding.impl.planbody;

import java.util.Map;

import jadex.bding.AgentModel;
import jadex.bding.IBDINGAgentFeature;
import jadex.bding.ICondition;
import jadex.bding.IPlanStep;
import jadex.bding.IPlanStepContainer;
import jadex.bding.IReasoner;
import jadex.bding.impl.ExpressionCondition;
import jadex.bding.impl.ReasonerCondition;
import jadex.bding.impl.planbody.strategic.StrategicConditionContainer;
import jadex.bding.impl.planbody.strategic.StrategicContainer;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.javaparser.SJavaParser;

public class ConditionalPlanStep implements IPlanStepContainer, IPlanStep
{
    protected ICondition condition;

    protected StrategicConditionContainer container;

    public ConditionalPlanStep(StrategicConditionContainer container)
    {
        this.container = container;
    }

    @Override
    public IFuture<PlanStepExecution> execute(IComponent component, PlanExecutionContext context)
    {
        Future<PlanStepExecution> ret = new Future<>();

        PlanStepExecution condexe = new PlanStepExecution(this, context.getParameters());

        this.condition = PlanStep.createCondition(container.getCondition(), component.getFeature(IBDINGAgentFeature.class).getModel(), 
            component.getFeature(IBDINGAgentFeature.class).getReasoner());

        executeCondition(component, context, condexe, ret);

        return ret;
    }

    protected void executeCondition(IComponent component, PlanExecutionContext context,
        PlanStepExecution condexe, Future<PlanStepExecution> ret)
    {
        condition.evaluate(context.getParameters()).then(result ->
        {
            StrategicContainer branch = result.booleanValue() ? container.getTrueContainer() : container.getFalseContainer();

            if(branch == null)
            {
                finishCondition(condexe, ret, context);
                return;
            }

            branch.getExecutableStep(component.getFeature(IBDINGAgentFeature.class).getReasoner(),
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
                            failCondition(condexe, ret, context, exe.getException());
                            return;
                        }
                    }

                    finishCondition(condexe, ret, context);
                })
                .catchEx(ex ->
                {
                    failCondition(condexe, ret, context, ex);
                });
            })
            .catchEx(ex ->
            {
                failCondition(condexe, ret, context, ex);
            });
        })
        .catchEx(ex ->
        {
            failCondition(condexe, ret, context, ex);
        });
    }

    protected void finishCondition(PlanStepExecution condexe, Future<PlanStepExecution> ret, PlanExecutionContext context)
    {
        condexe.setOutputs(context.getParameters());
        ret.setResult(condexe);
    }

    protected void failCondition(PlanStepExecution condexe, Future<PlanStepExecution> ret, PlanExecutionContext context, Exception ex)
    {
        condexe.setState(IPlanStep.PlanStepState.FAILED).setException(ex);
        finishCondition(condexe, ret, context);
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