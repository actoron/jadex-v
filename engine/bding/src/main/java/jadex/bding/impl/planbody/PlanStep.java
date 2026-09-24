package jadex.bding.impl.planbody;

import java.util.Map;

import jadex.bding.AgentModel;
import jadex.bding.ICondition;
import jadex.bding.IPlanStep;
import jadex.bding.IReasoner;
import jadex.bding.impl.ExpressionCondition;
import jadex.bding.impl.RIdElement;
import jadex.bding.impl.ReasonerCondition;
import jadex.common.IValueFetcher;
import jadex.javaparser.SJavaParser;

public abstract class PlanStep extends RIdElement implements IPlanStep
{
    protected Map<String, String> parammapping;
    
    protected String resultmapping;

    public PlanStep(String id, Map<String, String> parammapping, String resultmapping)
    {
        super(id);
        this.parammapping = parammapping;
        this.resultmapping = resultmapping;
    }

    public Map<String, String> getParameterMapping() 
    {
        return parammapping;
    }

    public void setParameterMapping(Map<String, String> parammapping) 
    {
        this.parammapping = parammapping;
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
        return SJavaParser.evaluateExpression(exp, new IValueFetcher() 
        {
            @Override
            public Object fetchValue(String name) 
            {
                return context.get(name);
            }        
        });
    }
    
}
