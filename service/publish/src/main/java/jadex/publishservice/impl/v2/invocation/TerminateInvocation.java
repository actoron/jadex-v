package jadex.publishservice.impl.v2.invocation;

import jadex.future.*;
import jadex.publishservice.IRequestManager.PublishContext;
import jadex.publishservice.impl.v2.Conversation;
import jadex.publishservice.impl.v2.Request;
import jadex.publishservice.impl.v2.Session;

public class TerminateInvocation extends Invocation 
{
    protected String exmessage;

    public TerminateInvocation(Request request, PublishContext context, String exmessage) 
    {
        super(request, context);
        this.exmessage = exmessage;
    }

    @Override
    public ISubscriptionIntermediateFuture<InvocationResult> invoke(Session session) 
    {
        SubscriptionIntermediateFuture<InvocationResult> fut = new SubscriptionIntermediateFuture<>();

        final String callid = request.getCallId();
        final String sessionid = request.getSessionId();

        Conversation conv = session.getConversation(callid);

        if(conv!=null)
        {
            conv.terminate(exmessage != null ? new RuntimeException(exmessage) : null);
        }
        else
        {
            System.out.println("Conversation not found " + callid + "/" + sessionid + " storing terminate");
        
            conv = session.getOrCreateConversation(callid, null);
            conv.setMustTerminate(true);
        }

        // no response ok?
        //InvocationResult res = new InvocationResult().setPayload(true);
        //fut.addIntermediateResult(res);
        fut.setFinished();

        return fut;
    }   
}