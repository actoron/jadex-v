package jadex.bding.impl;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jadex.collection.PathMap;
import jadex.core.IComponent;

public class RIdElement 
{
    public static AtomicInteger cnt = new AtomicInteger(); 

    protected String id;

    // Bean constructor for cloning
    public RIdElement()
    {
    }

    public RIdElement(String prefix)
    {
        this.id = createId(prefix);
    }

    public String createId(String prefix)
    {
        return prefix+" "+cnt.getAndIncrement();
    }

    public String getId() 
    {
        return id;
    }

    public void setId(String id) 
    {
        this.id = id;
    }

    @Override
    public int hashCode() 
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) 
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RIdElement other = (RIdElement) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    public static Map<String, Object> createContext(IComponent agent, RGoal goal)
    {
        Map<String, Object> params = new PathMap();

        for(String name: goal.getParameters().keySet())
        {
            params.put("goal."+name, goal.getParameters().get(name));
        }

        Map<String, Object> bels = BeliefExtractor.extract(agent);
        for(String name: bels.keySet())
        {
            params.put("belief."+name, bels.get(name));
        }

        return params;
    }
 
}
