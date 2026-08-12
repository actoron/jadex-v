package jadex.bding;

import jadex.core.IComponent;
import java.util.Map;

import jadex.future.IFuture;

public interface IPlanBody 
{
    public IFuture<Map<String, Object>> execute(IComponent component, Map<String, Object> parameters);
}
