package jadex.bding.impl.planbody;


import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;

public class StateStep extends PlanStep
{
    protected String exp;

    protected String resultmapping;

    public StateStep(String id, String exp, String resultmapping)
    {
        super(id, null, null);
        this.exp = exp;
        this.resultmapping = resultmapping;

        if(exp == null)
            throw new RuntimeException("Expression for state action must not be null");

        if(resultmapping==null)
            throw new RuntimeException("Resultmapping for state action must not null");
    }

    @Override
    public IFuture<PlanStepExecution> execute(IComponent component, PlanExecutionContext context)
    {
        Future<PlanStepExecution> ret = new Future<>();

        try
        {
            Object value = PlanStep.evaluateExpression(exp, context.getParameters());

            context.set(resultmapping, value);

            PlanStepExecution exe = new PlanStepExecution(this, context.getParameters());
            exe.setOutputs(context.getParameters());

            ret.setResult(exe);
        }
        catch(Exception e)
        {
            ret.setException(e);
        }

        return ret;
    }

   
}