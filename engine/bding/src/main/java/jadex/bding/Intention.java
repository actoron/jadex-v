package jadex.bding;

public class Intention 
{
    protected String name;
    protected String description;

    protected Goal goal; //applicable for goal
    
    public Intention(String name)
    {
        this.name = name;
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

    public Goal getGoal() 
    {
        return goal;
    }

    public Intention setGoal(Goal goal) 
    {
        this.goal = goal;
        return this;
    }
    
}
