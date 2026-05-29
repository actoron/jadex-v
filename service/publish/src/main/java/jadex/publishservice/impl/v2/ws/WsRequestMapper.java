package jadex.publishservice.impl.v2.ws;

import java.lang.reflect.Method;

import jadex.publishservice.IRequestManager.PublishContext;
import jadex.publishservice.impl.v2.IRequestMapper;
import jadex.publishservice.impl.v2.Request;
import jadex.publishservice.impl.v2.invocation.Invocation;
import jadex.publishservice.impl.v2.invocation.ResourceInvocation;
import jadex.publishservice.impl.v2.invocation.ServiceInfoInvocation;
import jadex.publishservice.impl.v2.invocation.ServiceInvocation;
import jadex.publishservice.impl.v2.invocation.TerminateInvocation;

public class WsRequestMapper implements IRequestMapper
{
    public enum InvocationType 
    {
        SERVICE("service"),
        RESOURCE("resource"),
        INFO("info"),
        TERMINATE("terminate");

        private final String value;

        InvocationType(String value) 
        {
            this.value = value;
        }

        public String getValue() 
        {
            return value;
        }

        public static InvocationType fromString(String s) 
        {
            if(s == null) return null;
            for(InvocationType t : values()) 
            {
                if(t.value.equalsIgnoreCase(s)) 
                    return t;
            }
            return null; // oder throw new IllegalArgumentException("Unknown type: "+s);
        }
    }

    @Override
    public boolean canHandle(Request req) 
    {
        return req instanceof WsRequest;
    }

    @Override
    public Invocation map(Request req, PublishContext context) 
    {
        WsRequest wsreq = (WsRequest) req;
        InvocationType type = wsreq.getInvocationType(); // optional
        
        if(type != null) 
        {
            switch(type) 
            {
                case RESOURCE:
                    return new ResourceInvocation(req, wsreq.getResourcePath());
                case SERVICE:
                {
                    Method m = wsreq.getServiceMethod();
                    return new ServiceInvocation(req, context, m, wsreq.getServiceParameters(m));
                }
                case INFO:
                    return new ServiceInfoInvocation(req, context, wsreq.getInfoUrl());
                case TERMINATE:
                    return new TerminateInvocation(req, context, wsreq.getException());
            }
        }

        if(wsreq.getTerminate() != null)
        {
            return new TerminateInvocation(req, context, wsreq.getException());
        }
        else if(wsreq.getResourcePath() != null) 
        {
            return new ResourceInvocation(req, wsreq.getResourcePath());
        } 
        else if(wsreq.getServiceMethod() != null) 
        {
            Method m = wsreq.getServiceMethod();
            return new ServiceInvocation(req, context, m, wsreq.getServiceParameters(m));
        } 
        else 
        {
            return new ServiceInfoInvocation(req, context, wsreq.getInfoUrl());
        }
    }
}

