package jadex.bding.impl;

import jadex.bding.Plan;
import jadex.core.IComponent;
import jadex.future.IFuture;

public class RPlan 
{
    protected Plan plan;

    protected IComponent component;

    public RPlan(Plan plan, IComponent component) 
    {
        this.plan = plan;
        this.component = component;
    }

    public IFuture<Void> execute()
    {
        // todo: execute plan body with parameters
        getPlan().getBody().execute(getComponent(), null);

        return null;
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
