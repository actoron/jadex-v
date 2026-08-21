package jadex.bding;

public class Intention 
{
    protected String name;
    protected String description;

    protected AgentModel model;

    //protected Goal goal; //applicable for goal
    
    public Intention(String name, String description, AgentModel model)
    {
        this.name = name;
        this.description = description;
        this.model = model;
        model.addIntention(this);
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

    public AgentModel getModel() 
    {
        return model;
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
    
    @Override
    public String toString() 
    {
        return "Intention [name=" + name + ", description=" + description + "]";
    }
    
}
