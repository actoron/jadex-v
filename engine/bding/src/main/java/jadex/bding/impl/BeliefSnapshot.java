package jadex.bding.impl;

import java.util.Map;

import com.eclipsesource.json.JsonObject;

import jadex.core.IComponent;

public class BeliefSnapshot
{
    protected JsonObject beliefs;

    protected BeliefSnapshot(JsonObject beliefs)
    {
        this.beliefs = beliefs;
    }

    public static BeliefSnapshot extract(IComponent component)
    {
        JsonObject values = BeliefExtractor.extract(component);
        return new BeliefSnapshot(values);
    }

    public JsonObject getJson()
    {
        return beliefs;
    }

    public void inject(IComponent component)
    {
        BeliefExtractor.inject(component, beliefs);
    }

    @Override
    public String toString()
    {
        return beliefs.toString();
    }
}
