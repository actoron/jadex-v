package jadex.publishservice.impl.v2;


import java.util.ArrayList;
import java.util.List;

import jadex.execution.IExecutionFeature;
import jadex.publishservice.IRequestManager;
import jadex.publishservice.impl.v2.http.HttpConnectionFactory;
import jadex.publishservice.impl.v2.http.HttpRequestMapper;
import jadex.publishservice.impl.v2.invocation.Invocation;
import jadex.publishservice.impl.v2.invocation.InvocationResult;
import jadex.publishservice.impl.v2.ws.WsConnectionFactory;
import jadex.publishservice.impl.v2.ws.WsRequestMapper;

public class RequestManager2 implements IRequestManager
{
    protected SessionManager sesman;

    protected List<IConnectionFactory> confacs;

    protected List<IRequestMapper> reqmappers;

    public RequestManager2()
    {
        sesman = new SessionManager();
        confacs = new ArrayList<>();
        reqmappers = new ArrayList<>();

        // todo: Hack!
        reqmappers.add(new HttpRequestMapper());
        reqmappers.add(new WsRequestMapper());
        confacs.add(new HttpConnectionFactory());
        confacs.add(new WsConnectionFactory());
    }

    protected static IRequestManager instance;
	
	protected static synchronized void createInstance()
	{
		if(instance==null)
			instance = new RequestManager2();
	}
	
	protected static synchronized IRequestManager getInstance()
	{
		return getInstance(false);
	}

	protected static synchronized IRequestManager getInstance(boolean create)
	{
		if(instance==null)
		{
			if(create)
				createInstance();
			else
				throw new RuntimeException("request manager was not created");
		}
		return instance; 
	}
    
    public void handleRequest(Request req, Response resp, PublishContext context) throws Exception
    {
        // Find suitable request mapper
        IRequestMapper reqmap = reqmappers.stream().filter(m -> m.canHandle(req)).findFirst()
            .orElseThrow(() -> new RuntimeException("No request mapper found for request of type " + req.getRawRequest().getClass()));

        // Create an invocation from the request
        Invocation call = reqmap.map(req, context);

        System.out.println("Request manager received: "+call);

        Session session = sesman.getOrCreateSession(req.getSessionId());

        // Extract/create connection
        IConnectionFactory confac = confacs.stream().filter(f -> f.canHandle(req)).findFirst()
            .orElseThrow(() -> new RuntimeException("No connection factory found for request of type " + req.getRawRequest().getClass()));
        Connection con = confac.create(req);

        session.scheduleOnSession(agent ->
        {
            System.out.println("scheduled: "+agent.getFeature(IExecutionFeature.class).isComponentThread());
            
            if(con != null)
            {
                if(con.supports(TransportMode.STREAM))
                    session.addConnection(con); // thread-safe
                else 
                    req.setConnection(con); // only request reply
            }

            System.out.println("before call invoke: "+call);

            // Invoke the invocation
            call.invoke(session).next(res ->
            {
                System.out.println("after call invoke: "+res);
                session.scheduleOnSession(ag ->
                {
                    System.out.println("called invocation: "+res+" "+ag.getFeature(IExecutionFeature.class).isComponentThread());

                    // Create a result message
                    Message message = new Message(req, resp).setResult(res);

                    // Send the result
                    session.dispatch(message, context); 
                });
            })
            .catchEx(ex -> 
            {
                session.scheduleOnSession(ag ->
                {
                    //System.out.println("exception on invocation: "+ex+" "+ag.getFeature(IExecutionFeature.class).isComponentThread());

                    // Create a result message
                    Message message = new Message(req, resp).setResult(new InvocationResult(ex)).setError(true);
                    
                    // Send the result
                    session.dispatch(message, context); 
                });
            });
            // add also .then?!
        }).printOnEx();
    }

    public void addRequestMapper(IRequestMapper reqmapper)
    {
        reqmappers.add(reqmapper);
    }
}

