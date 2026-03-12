package jadex.publishservice.impl.v2.invocation;

import java.lang.reflect.Method;
import java.util.Collection;

import jadex.core.IComponentHandle;
import jadex.core.impl.ComponentManager;
import jadex.future.*;
import jadex.publishservice.IRequestManager.PublishContext;
import jadex.publishservice.impl.v2.Conversation;
import jadex.publishservice.impl.v2.Request;
import jadex.publishservice.impl.v2.Session;
import jadex.publishservice.impl.v2.SessionManager;

public class ServiceInvocation extends Invocation 
{
    /** Finished result marker. */
    public static final String FINISHED = "__finished__";

    protected Method method;
    protected Object[] parameters;

    public ServiceInvocation(Request request, PublishContext context, Method method, Object[] parameters) 
    {
        super(request, context);
        this.method = method;
        this.parameters = parameters;
    }

	// todo: remove finished marker

    @Override
    public ISubscriptionIntermediateFuture<InvocationResult> invoke(SessionManager sesman) 
    {
        SubscriptionIntermediateFuture<InvocationResult> fut = new SubscriptionIntermediateFuture<>();

        final String callid = request.getCallId();
        final String sessionid = request.getSessionId();
        final Session session = sesman.getOrCreateSession(sessionid);

        IComponentHandle comp = ComponentManager.get()
            .getComponentHandle(context.service().getServiceId().getProviderId());

        comp.scheduleStep(agent -> 
        {
            Object result;

            try 
            {
                result = method.invoke(context.service(), parameters);

                Conversation conver = sesman.getConversation(sessionid, callid);
                if(conver != null)
                    throw new RuntimeException("Conversation for new call must not exist");
                conver = sesman.startConversation(sessionid, callid, result instanceof IFuture ? (IFuture<?>) result : null);
				final Conversation conv = conver;

                if(result instanceof IIntermediateFuture) 
                {
                    ((IIntermediateFuture<Object>) result).addResultListener(new IIntermediateFutureCommandResultListener<Object>() 
                    {
                        @Override
                        public void intermediateResultAvailable(Object r) 
                        {
                            InvocationResult ir = handleResult(conv, r, null, null, null);
                            fut.addIntermediateResult(ir);
                        }

                        @Override
                        public void resultAvailable(Collection<Object> results) 
                        {
                            for(Object r : results)
                                fut.addIntermediateResult(handleResult(conv, r, null, null, null));
                            finished();
                        }

                        @Override
                        public void exceptionOccurred(Exception e) 
                        {
                            fut.addIntermediateResult(handleResult(conv, null, e, null, null));
    						fut.addIntermediateResult(handleResult(conv, FINISHED, null, null, null));
                        	fut.setFinished();
	                    }

                        @Override
                        public void finished() 
                        {
							fut.addIntermediateResult(handleResult(conv, FINISHED, null, null, null));
                            fut.setFinished();
                        }

                        @Override
                        public void commandAvailable(Object command) 
                        {
                            fut.addIntermediateResult(handleResult(conv, null, null, command, null));
                        }

                        @Override
                        public void maxResultCountAvailable(int max) 
                        {
                            // Optional: Header oder Meta im InvocationResult
							fut.addIntermediateResult(handleResult(conv, null, null, null, max));
                        }
                    });
                }
                else if(result instanceof IFuture)
                {
                    ((IFuture<Object>) result).addResultListener(new IResultListener<Object>() 
                    {
                        public void resultAvailable(Object r) 
                        {
                            fut.addIntermediateResult(handleResult(conv, r, null, null, null));
                            fut.setFinished();
                        }

                        public void exceptionOccurred(Exception e) 
                        {
                            fut.addIntermediateResult(handleResult(conv, null, e, null, null));
                            fut.setFinished();
                        }
                    });
                }
                else
                {
                    fut.addIntermediateResult(handleResult(conv, result, null, null, null));
                    fut.setFinished();
                }

            } 
            catch(Exception e) 
            {
                Conversation conv = sesman.startConversation(sessionid, callid, null);
                fut.addIntermediateResult(handleResult(conv, null, e, null, null));
                fut.setFinished();
            }

        })
        .catchEx(e -> 
		{
            Conversation conv = sesman.startConversation(sessionid, callid, null);
            fut.addIntermediateResult(handleResult(conv, null, e, null, null));
            fut.setFinished();
        });

        return fut;
    }

    protected InvocationResult handleResult(Conversation conv, Object result, Exception ex, Object command, Integer max) 
    {
        InvocationResult res = new InvocationResult();

        if (FINISHED.equals(result)) 
        {
            res.setFinished(true);
        } 
        else if (ex != null) 
        {
            res.setPayload(ex);
        } 
        else if (command != null) 
        {
            res.setPayload(command);
        } 
        else 
        {
            res.setPayload(result);
        }

        if (max != null) 
            res.setMax(max);

        return res;
    }
}
