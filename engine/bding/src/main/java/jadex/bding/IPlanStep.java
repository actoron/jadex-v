package jadex.bding;

import java.util.Map;

import jadex.core.IComponent;
import jadex.future.IFuture;

public interface IPlanStep
{
    public IFuture<Map<String, Object>> execute(IComponent component, Map<String, Object> parameters);
}