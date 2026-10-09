package jadex.bding.impl.planbody;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import jadex.bding.AgentModel;
import jadex.bding.ICondition;
import jadex.bding.IPlanStep;
import jadex.bding.IReasoner;
import jadex.bding.impl.BeliefExtractor;
import jadex.bding.impl.ExpressionCondition;
import jadex.bding.impl.RGoal;
import jadex.bding.impl.RIdElement;
import jadex.bding.impl.ReasonerCondition;
import jadex.collection.PathMap;
import jadex.core.IComponent;
import jadex.javaparser.SJavaParser;

public abstract class PlanStep extends RIdElement implements IPlanStep
{
    protected Map<String, String> inputmapping;
    
    protected String resultmapping;

    public PlanStep(String id, Map<String, String> inputmapping, String resultmapping)
    {
        super(id);
        this.inputmapping = inputmapping;
        this.resultmapping = resultmapping;
    }

    public Map<String, String> getInputMapping() 
    {
        return inputmapping;
    }

    public void setInputMapping(Map<String, String> parammapping) 
    {
        this.inputmapping = parammapping;
    }

    public String getResultMapping() 
    {
        return resultmapping;
    }

    public void setResultMapping(String resultmapping) 
    {
        this.resultmapping = resultmapping;
    }

    public static ICondition createCondition(String condition, AgentModel model, IReasoner reasoner)
    {
        if(isExpression(condition))
            return new ExpressionCondition(condition);
        else
            return new ReasonerCondition(reasoner, condition, model);
    }

    public static boolean isExpression(String exp)
    {
        return exp.startsWith("=");
    }

    public static Object evaluateExpression(String exp, Map<String, Object> context)
    {
        if(isExpression(exp))
            exp = exp.substring(1);
        else
            System.out.println("Warning: found expression without expression marker: "+exp);
        return SJavaParser.evaluateExpression(exp, name -> context.get(name));
    }

    protected Map<String, Object> resolveInputMapping(PlanExecutionContext context)
    {
        Map<String, Object> args = new PathMap();

        if(getInputMapping() == null)
            return args;

        for(Map.Entry<String, String> entry: getInputMapping().entrySet())
        {
            String source = entry.getKey();
            String target = entry.getValue();

            Object value;

            if(source.startsWith("="))
            {
                value = PlanStep.evaluateExpression(source, context.getParameters());
            }
            else if(context.has(source))
            {
                value = context.get(source);
            }
            else
            {
                throw new RuntimeException("Parameter not found: " + source);
            }

            args.put(target, value);
        }

        return args;
    }

     // todo: put in parameters of all goals in hierachy (plans?!)
    /*public static Map<String, Object> createContext(RGoal goal, IComponent agent)
    {
        Map<String, Object> context = new HashMap<>();

        


        Map<String, Object> bels = BeliefExtractor.extract(agent);
        bels.keySet().forEach(name -> context.put("belief." + name, bels.get(name)));

        goal.getParameters().keySet().forEach(name -> context.put("goal." + name, goal.getParameters().get(name)));
        
        return context;
    }*/
    
}
