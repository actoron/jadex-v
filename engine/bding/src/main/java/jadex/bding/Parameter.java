package jadex.bding;


public class Parameter extends ModelElement
{
    protected ElementType type;

    public Parameter(String name, String description, ElementType type)
    {
        super(name, description, null);
        this.type = type;
    }
 
    public ElementType getType() 
    {
        return type;
    }
    
    public void setType(ElementType type) 
    {
        this.type = type;
    }

    @Override
    public String toString() 
    {
        return "Parameter [name=" + name + ", description=" + description + ", type=" + type + "]";
    }
}
