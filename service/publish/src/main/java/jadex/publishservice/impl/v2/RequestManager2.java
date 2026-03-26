package jadex.publishservice.impl.v2;


import java.util.ArrayList;
import java.util.List;

import jadex.publishservice.IRequestManager;
import jadex.publishservice.impl.v2.http.HttpConnectionFactory;
import jadex.publishservice.impl.v2.invocation.Invocation;
import jadex.publishservice.impl.v2.invocation.InvocationResult;

public class RequestManager2 implements IRequestManager
{
    protected SessionManager sesman;

    protected HttpConnectionFactory confac;
    
    protected List<IRequestMapper> reqmappers;

    public RequestManager2()
    {
        sesman = new SessionManager();
        confac = new HttpConnectionFactory();
        reqmappers = new ArrayList<>();
    }
    
    public void handleRequest(Request req, Response resp, PublishContext context) throws Exception
    {
        // Find suitable request mapper
        IRequestMapper reqmap = reqmappers.stream().filter(m -> m.canHandle(req)).findFirst()
            .orElseThrow(() -> new RuntimeException("No request mapper found for request of type " + req.getRawRequest().getClass()));

        // Create an invocation from the request
        Invocation call = reqmap.map(req, context);

        Session session = sesman.getOrCreateSession(req.getSessionId());

        // Extract/create connection
        Connection con = confac.create(req);
        if(con != null)
        {
            if(con.supports(TransportMode.STREAM))
                session.addConnection(con);
            else 
                req.setConnection(con); // only request reply
        }

        // Create conversation
        //Conversation conv = sesman.startConversation(req.getSessionId(), req.getCallId());
        //conv.setResponseStrategy(strategy);

        // Invoke the invocation
        call.invoke(sesman).next(res ->
        {
            // Create a result message
            Message message = new Message(req, resp).setResult(res);

            // Send the result
            sesman.dispatch(message, context);
        })
        .catchEx(ex -> 
        {
            // Create a result message
            Message message = new Message(req, resp).setResult(new InvocationResult(ex)).setError(true);
            
            // Send the result
            sesman.dispatch(message, context);
        });
        // add also .then?!
    }

    public void addRequestMapper(IRequestMapper reqmapper)
    {
        reqmappers.add(reqmapper);
    }
}

