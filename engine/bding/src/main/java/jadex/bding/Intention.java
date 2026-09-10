package jadex.bding;

public class Intention extends ModelElement
{
    //protected Goal goal; //applicable for goal
    
    public Intention(String name, String description, AgentModel model)
    {
        super(name, description, model);
        model.addIntention(this);
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
