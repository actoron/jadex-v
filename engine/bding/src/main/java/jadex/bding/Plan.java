package jadex.bding;

public class Plan extends ModelElement
{
    protected Intention intention;

    protected StrategicPlan strategicplan;

    protected IPlanBody body;

    public Plan(String name, String desciption, Intention intention, AgentModel model)
    {
        super(name, desciption, model);
        this.intention = intention;
        model.addPlan(this);
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

    public StrategicPlan getStrategicPlan() 
    {
        return strategicplan;
    }

    public void setStrategicPlan(StrategicPlan strategicplan) 
    {
        this.strategicplan = strategicplan;
    }

    @Override
    public String toString() 
    {
        return "Plan [name=" + name + ", description=" + description + "]";
    }

}
