package jadex.bding.impl.planbody;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jadex.bding.IPlanBody;
import jadex.bding.IPlanStep;
import jadex.bding.impl.RPlan;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;

public class SequentialPlanBody implements IPlanBody
{
    protected List<IPlanStep> steps = new ArrayList<>();

    public SequentialPlanBody()
    {
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
    public IFuture<Map<String, Object>> execute(IComponent component, RPlan plan, Map<String, Object> parameters)
    {
        Future<Map<String, Object>> ret = new Future<>();

        executeSteps(component, plan, parameters, 0, ret);

        return ret;
    }

    protected void executeSteps(IComponent component, RPlan plan, Map<String, Object> parameters, int index, Future<Map<String, Object>> ret)
    {
        if(index >= steps.size())
        {
            ret.setResult(parameters);
            return;
        }

        IPlanStep step = steps.get(index);

        try
        {
            // Execute before transformation
            //step.getBefore().apply(parameters);

            step.execute(component, parameters, plan).then(result ->
            {
                if(result != null)
                    parameters.putAll(result);

                // Execute after transformation
                //step.getAfter().apply(parameters);

                executeSteps(component, plan, parameters, index+1, ret);
            }).catchEx(ex ->
            {
                ret.setException(ex);
            });
        }
        catch(Exception e)
        {
            ret.setException(e);
        }
    }
}
