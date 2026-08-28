package jadex.bding.impl;

import java.util.concurrent.atomic.AtomicInteger;

public class RIdElement 
{
    public static AtomicInteger cnt = new AtomicInteger(); 

    protected String id;

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
    
}
