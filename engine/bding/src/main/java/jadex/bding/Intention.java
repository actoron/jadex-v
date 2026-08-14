package jadex.bding;

public class Intention 
{
    protected String name;
    protected String description;

    //protected Goal goal; //applicable for goal
    
    public Intention(String name, String description)
    {
        this.name = name;
        this.description = description;
    }

    public String getName() 
    {
        return name;
    }

    public Intention setName(String name) 
    {
        this.name = name;
        return this;
    }

    public String getDescription() 
    {
        return description;
    }

    public Intention setDescription(String description) 
    {
        this.description = description;
        return this;
    }

    @Override
    public String toString() 
    {
        return "Intention [name=" + name + ", description=" + description + "]";
    }

    /*public Goal getGoal() 
    {
        return goal;
    }

    public Intention setGoal(Goal goal) 
    {
        this.goal = goal;
        return this;
    }*/
    
    
}
