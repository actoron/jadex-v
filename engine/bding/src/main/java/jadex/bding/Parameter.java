package jadex.bding;


public class Parameter 
{
    protected String name;
    protected String description;
    protected ElementType type;

    public Parameter(String name, String description, ElementType type)
    {
        this.name = name;
        this.description = description;
        this.type = type;
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
