package jadex.bding.impl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.eclipsesource.json.JsonObject;

import jadex.core.IComponent;

public class BeliefSnapshot
{
    protected Map<String, Object> beliefs = new LinkedHashMap<>();

    public BeliefSnapshot()
    {
    }

    public BeliefSnapshot(Map<String, Object> beliefs)
    {
        this.beliefs.putAll(beliefs);
    }

    public Object get(String name)
    {
        return beliefs.get(name);
    }

    public Map<String, Object> getBeliefs()
    {
        return Collections.unmodifiableMap(beliefs);
    }

    public static BeliefSnapshot extract(IComponent component)
    {
        return BeliefExtractor.extract(component);
    }

    public void inject(IComponent component)
    {
        BeliefExtractor.inject(component, this);
    }

}

