package jadex.publishservice.impl.v2.sse;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletResponse;
import jadex.common.SUtil;
import jadex.publishservice.impl.v2.Connection;
import jadex.publishservice.impl.v2.Message;
import jadex.publishservice.impl.v2.Request;
import jadex.publishservice.impl.v2.TransportType;
import jadex.publishservice.impl.v2.http.HttpRequest;

public class SseConnection extends Connection
{
    protected PrintWriter writer;

    protected AsyncContext asyncContext = null;

    public SseConnection(String id, Request request)
    {
        super(id, TransportType.SSE);

        HttpRequest req = (HttpRequest)request;
        this.asyncContext = req.getAsyncContextInfo().getAsyncContext();
        asyncContext.setTimeout(0);

        HttpServletResponse resp = (HttpServletResponse)asyncContext.getResponse();

        resp.setContentType("text/event-stream");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader("Connection", "keep-alive");

        try
        {
            this.writer = resp.getWriter();
        }
        catch(IOException e)
        {
            SUtil.throwUnchecked(e);
        }
    }

    @Override
    public boolean send(Message message) throws Exception
    {
        // todo: metainfos?
        //SendData details = getSendDetails(message);
        Object payload = message.getResult().getPayload();

        try
        {
            writer.write("data: ");
            writer.write(String.valueOf(payload));
            writer.write("\n\n");
            writer.flush();

            markAlive();
            return true;
        }
        catch(Exception e)
        {
            return false;
        }
    }

    @Override
    public void terminate()
    {
        try
        {
            asyncContext.complete();
        }
        catch(Exception e)
        {
        }
    }

    public AsyncContext getAsyncContext()
    {
        return asyncContext;
    }
}