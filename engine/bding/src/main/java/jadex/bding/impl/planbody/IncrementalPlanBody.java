package jadex.bding.impl.planbody;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jadex.bding.Belief;
import jadex.bding.IBDINGAgentFeature;
import jadex.bding.IPlanBody;
import jadex.bding.IPlanStep;
import jadex.bding.IReasoner;
import jadex.bding.StrategicPlan;
import jadex.bding.StrategicStep;
import jadex.bding.impl.RGoal;
import jadex.bding.impl.RIdElement;
import jadex.bding.impl.RPlan;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;

public class IncrementalPlanBody extends RIdElement implements IPlanBody
{
    protected StrategicPlan strategicPlan;

    protected List<IPlanStep> steps = new ArrayList<>();

    public IncrementalPlanBody(StrategicPlan strategicPlan)
    {
        super("incrementalplanbody");
        this.strategicPlan = strategicPlan;
    }

    public StrategicPlan getStrategicPlan()
    {
        return strategicPlan;
    }

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
        if(index >= strategicPlan.getSteps().size())
        {
            ret.setResult(parameters);
            return;
        }

        StrategicStep sstep = strategicPlan.getSteps().get(index);

        try
        {
            IReasoner reasoner = component.getFeature(IBDINGAgentFeature.class).getReasoner();

            reasoner.generatePlanStep(plan, sstep, parameters).then(step ->
            {
                if(step == null)
                {
                    ret.setException(new RuntimeException("Reasoner generated no plan step for strategic step '"+ sstep.getName() + "'"));
                    return;
                }

                steps.add(step);

                step.execute(component, parameters, plan).then(result ->
                {
                    writeBackContext(result, plan.getIntention().getGoal(), component);

                    executeSteps(component, plan, result, index + 1, ret);
                })
                .catchEx(ret);
            }).catchEx(ret);
        }
        catch(Exception e)
        {
            ret.setException(e);
        }
    }

    public static void writeBackContext(Map<String, Object> context, RGoal goal, IComponent agent)
    {
        for(Entry<String, Object> e : context.entrySet())
        {
            String key = e.getKey();

            if(key.startsWith("belief."))
            {
                String name = key.substring("belief.".length());

                Belief bel = goal.getGoal().getModel().getBeliefs().get(name);

                if(bel==null)
                {
                    //ret.setException(new RuntimeException("belief not found: "+name));
                    System.out.println("belief not found: "+name);
                }
                else
                {
                    bel.setValue(agent.getPojo(), e.getValue());
                    System.out.println("Updated belief: "+name+" "+e.getValue());
                }
            }
            else if(key.startsWith("goal."))
            {
                String name = key.substring("goal.".length());
            
                goal.getParameters().put(name, e.getValue());
                System.out.println("Updated goal parameter: "+name+" "+e.getValue());
            }
        }
    }
}