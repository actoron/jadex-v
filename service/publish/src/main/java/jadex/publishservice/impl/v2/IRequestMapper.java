package jadex.publishservice.impl.v2;

import jadex.publishservice.IRequestManager.PublishContext;
import jadex.publishservice.impl.v2.invocation.Invocation;

public interface IRequestMapper 
{
    public boolean canHandle(Request req);

    public Invocation map(Request req, PublishContext context);
}
