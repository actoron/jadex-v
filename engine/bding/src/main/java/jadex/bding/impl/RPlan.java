package jadex.bding.impl;

import jadex.bding.Plan;
import jadex.core.IComponent;

public class RPlan 
{
    protected Plan plan;

    protected IComponent component;

    public RPlan(Plan plan, IComponent component) 
    {
        this.plan = plan;
        this.component = component;
    }

    public void execute()
    {
        // todo: execute plan body with parameters
        getPlan().getBody().execute(getComponent(), null);
    }

    public Plan getPlan() 
    {
        return plan;
    }

    public IComponent getComponent() 
    {
        return component;
    }


}
