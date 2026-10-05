package jadex.bding;

import java.util.Map;

import jadex.future.IFuture;

public interface ICondition
{
    public IFuture<Boolean> evaluate(Map<String, Object> context);
}