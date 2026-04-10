package jadex.publishservice;

import jadex.providedservice.IService;
import jadex.publishservice.impl.PublishInfo;
import jadex.publishservice.impl.v2.Request;
import jadex.publishservice.impl.v2.Response;

public interface IRequestManager 
{
    public static record PublishContext(PublishInfo info, IService service, Object mapping)
    {
        public PublishContext 
        {
            System.out.println("PublishContext created: info=" + info + ", service=" + service + ", mapping=" + mapping);
        }
    }

    public void handleRequest(Request req, Response resp, PublishContext context) throws Exception;

    public default boolean isSupported(PublishType pt)
    {
        return PublishType.isKnown(pt);
    }
}
