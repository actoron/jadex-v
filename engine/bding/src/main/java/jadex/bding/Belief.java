package jadex.bding;

import java.lang.reflect.Field;

public class Belief 
{
    protected String name;

    protected String description;

    protected Field field;

    protected AgentModel model;

    public Belief(String name, String description, Field field, AgentModel model)
    {
        this.name = name;
        this.description = description;
        this.field = field;
        this.model = model;
        model.addBelief(this);
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

    public Field getField() 
    {
        return field;
    }

    public void setField(Field field) 
    {
        this.field = field;
    }

    public AgentModel getModel() 
    {
        return model;
    }

    public void setModel(AgentModel model) 
    {
        this.model = model;
    }

    public Class<?> getJavaType()
    {
        return field.getType();
    }

    public ElementType getType()
    {
        return ElementType.fromJavaClass(field.getType());
    }
}
