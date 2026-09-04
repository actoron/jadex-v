package jadex.bding;

import jadex.common.SReflect;

public class ModelElement 
{
    protected String name;

    protected String description;

    protected AgentModel model;

    public ModelElement(String name, String description)
    {
        this(name, description, null);
    }

    public ModelElement(String name, String description, AgentModel model)
    {
        this.name = name;
        this.description = description;
        this.model = model;
    }

    public String getName() 
    {
        return name;
    }

    public void setName(String name) 
    {
        this.name = name;
    }

    public String getDescription() 
    {
        return description;
    }

    public void setDescription(String description) 
    {
        this.description = description;
    }

    public AgentModel getModel() 
    {
        return model;
    }

    public void setModel(AgentModel model) 
    {
        this.model = model;
    }

    @Override
    public int hashCode() 
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        ModelElement other = (ModelElement) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    @Override
    public String toString() 
    {
        return SReflect.getUnqualifiedClassName(getClass())
            +" [name=" + name + ", description=" + description + ", model=" + model + "]";
    }
}
