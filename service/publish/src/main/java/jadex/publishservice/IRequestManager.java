package jadex.publishservice;

import jadex.providedservice.IService;
import jadex.publishservice.impl.PublishInfo;
import jadex.publishservice.impl.v2.Request;
import jadex.publishservice.impl.v2.Response;

public interface IRequestManager 
{
    public static record PublishContext(PublishInfo info, IService service, Object mapping)
    {
    }

    public void handleRequest(Request req, Response resp, PublishContext context) throws Exception;
}
