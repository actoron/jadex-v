package jadex.bding;

import jadex.bding.impl.planbody.strategic.StrategicContainer;

public class Plan extends ModelElement
{
    protected Intention intention;

    protected StrategicContainer strategicplan;

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

    public StrategicContainer getStrategicPlan() 
    {
        return strategicplan;
    }

    public void setStrategicPlan(StrategicContainer strategicplan) 
    {
        this.strategicplan = strategicplan;
    }

    @Override
    public String toString() 
    {
        return "Plan [name=" + name + ", description=" + description + "]";
    }

}
