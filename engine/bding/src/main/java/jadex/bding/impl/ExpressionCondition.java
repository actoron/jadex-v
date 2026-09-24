package jadex.bding.impl;

import java.util.Map;

import jadex.bding.AgentModel;
import jadex.bding.ICondition;
import jadex.common.IValueFetcher;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.javaparser.SJavaParser;

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
            Object result = SJavaParser.evaluateExpression(expression, new IValueFetcher() 
            {
                @Override
                public Object fetchValue(String name) 
                {
                    return context.get(name);
                }
            });

            ret.setResult(((Boolean)result).booleanValue());
        }
        catch(Exception e)
        {
            ret.setException(e);
        }

        return ret;
    }
}