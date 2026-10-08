package jadex.bding.impl.planbody;

import java.util.LinkedHashMap;
import java.util.Map;

import jadex.bding.AgentModel;
import jadex.bding.ICondition;
import jadex.bding.IPlanStep;
import jadex.bding.IReasoner;
import jadex.bding.impl.ExpressionCondition;
import jadex.bding.impl.RIdElement;
import jadex.bding.impl.ReasonerCondition;
import jadex.collection.PathMap;
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

    public static boolean isExpression(String condition)
    {
        return SJavaParser.isExpressionString(condition);
    }

    public static Object evaluateExpression(String exp, Map<String, Object> context)
    {
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
                value = SJavaParser.evaluateExpression(source.substring(1), name -> context.get(name));
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
    
}
