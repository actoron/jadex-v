package jadex.bding.impl.planbody;

import java.util.ArrayList;
import java.util.List;

import jadex.bding.IPlanBody;
import jadex.bding.IPlanStep;
import jadex.bding.impl.RIdElement;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;

public class SequentialPlanBody extends RIdElement implements IPlanBody
{
    protected List<IPlanStep> steps = new ArrayList<>();

    public SequentialPlanBody()
    {
        super("planbody");
    }

    /*public SequentialPlanBody(String description)
    {
        addSteps(description);
    }*/

    public SequentialPlanBody addStep(IPlanStep step)
    {
        steps.add(step);
        return this;
    }

    /*public SequentialPlanBody addStep(String description)
    {
        steps.add(PlanStepParser.parse(description));
        return this;
    }

    public SequentialPlanBody addSteps(String description)
    {
        steps.addAll(PlanStepParser.parseAll(description));
        return this;
    }*/

    public List<IPlanStep> getSteps()
    {
        return steps;
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
        if(index >= steps.size())
        {
            ret.setResult(null);
            return;
        }

        IPlanStep step = steps.get(index);

        try
        {
            // Execute before transformation
            //step.getBefore().apply(parameters);

            step.execute(component, context).then(exe ->
            {
                context.getPlan().addExecutedStep(exe);
                // Execute after transformation
                //step.getAfter().apply(parameters);

                executeSteps(component, context, index+1, ret);
            }).catchEx(ex ->
            {
                 System.out.println("Step return exception, should not happen: "+ex);

                PlanStepExecution exe = new PlanStepExecution(step);
                exe.setException(ex);

                context.getPlan().addExecutedStep(exe);
                ret.setException(ex);
            });
        }
        catch(Exception e)
        {
            e.printStackTrace();
            ret.setException(e);
        }
    }
}
