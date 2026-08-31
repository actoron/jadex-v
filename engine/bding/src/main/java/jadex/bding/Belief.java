package jadex.bding;

import java.lang.reflect.Field;

public class Belief extends ModelElement
{
    protected Field field;

    protected AgentModel model;

    public Belief(String name, String description, Field field, AgentModel model)
    {
        super(name, description, model);
        this.field = field;
        model.addBelief(this);
    }

    public Field getField() 
    {
        return field;
    }

    public void setField(Field field) 
    {
        this.field = field;
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
