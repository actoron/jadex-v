package jadex.bding.impl.planbody;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jadex.bding.impl.RPlan;

public class PlanExecutionContext
{
    protected RPlan plan;

    protected Map<String, Object> parameters;
    
    protected Set<String> dirty = new HashSet<>();

    public PlanExecutionContext(RPlan plan, Map<String, Object> parameters)
    {
        this.plan = plan;
        this.parameters = parameters;
    }

    public RPlan getPlan()
    {
        return plan;
    }

    public Object get(String name)
    {
        return parameters.get(name);
    }

    public void set(String name, Object value)
    {
        parameters.put(name, value);
        dirty.add(name);
    }

    public boolean has(String name)
    {
        return parameters.containsKey(name);
    }

    public boolean isDirty(String name)
    {
        return dirty.contains(name);
    }

    public Set<String> getDirty()
    {
        return Collections.unmodifiableSet(dirty);
    }

    public Map<String, Object> getParameters()
    {
        return Collections.unmodifiableMap(parameters);
    }
}