package jadex.bding.impl;

import jadex.bding.Plan;
import jadex.core.IComponent;
import jadex.future.Future;
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
        Future<Void> ret = new Future<>();

        // todo: execute plan body with parameters
        getPlan().getBody().execute(getComponent(), null).then(res ->
        {
            System.out.println("plan execution led to: "+res);

            // todo: update beliefs with results

            ret.setResult(null);

        }).catchEx(ret);

        return ret;
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
