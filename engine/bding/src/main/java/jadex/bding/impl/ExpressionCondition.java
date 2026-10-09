package jadex.bding.impl;

import java.util.Map;

import jadex.bding.ICondition;
import jadex.bding.impl.planbody.PlanStep;
import jadex.future.Future;
import jadex.future.IFuture;

public class ExpressionCondition implements ICondition
{
    protected String expression;

    public ExpressionCondition(String expression)
    {
        this.expression = expression;
    }

    @Override
    public IFuture<Boolean> evaluate(Map<String, Object> context)
    {
        Future<Boolean> ret = new Future<>();

        try
        {
            Object result = PlanStep.evaluateExpression(expression, context);

            ret.setResult(((Boolean)result).booleanValue());
        }
        catch(Exception e)
        {
            ret.setException(e);
        }

        return ret;
    }

    @Override
    public String toString() 
    {
        return "ExpressionCondition [expression=" + expression + "]";
    }
}