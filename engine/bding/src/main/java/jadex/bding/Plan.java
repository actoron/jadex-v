package jadex.bding;

public class Plan 
{
    protected String name;
    protected String description;

    protected Intention intention;

    protected IPlanBody body;

    protected AgentModel model;

    public Plan(String name, String desciption, Intention intention, AgentModel model)
    {
        this.name = name;
        this.description = desciption;
        this.intention = intention;
        this.model = model;
        model.addPlan(this);
    }

    public String getName() 
    {
        return name;
    }

    public Plan setName(String name) 
    {
        this.name = name;
        return this;
    }

    public String getDescription() 
    {
        return description;
    }

    public Plan setDescription(String description) 
    {
        this.description = description;
        return this;
    }

    public Intention getIntention() 
    {
        return intention;
    }

    public Plan setIntention(Intention intention) 
    {
        this.intention = intention;
        return this;
    }

    public IPlanBody getBody() 
    {
        return body;
    }

    public Plan setBody(IPlanBody body) 
    {
        this.body = body;
        return this;
    }

    public AgentModel getModel() 
    {
        return model;
    }

    @Override
    public String toString() 
    {
        return "Plan [name=" + name + ", description=" + description + "]";
    }

}
