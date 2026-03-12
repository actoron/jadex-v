package jadex.publishservice.impl.v2.http;

import java.io.IOException;

import jadex.publishservice.impl.v2.Connection;
import jadex.publishservice.impl.v2.Request;
import jadex.publishservice.impl.v2.TransportType;
import jadex.publishservice.publish.IAsyncContextInfo;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.http.HttpServletRequest;

public class HttpRequest extends Request
{
    public HttpRequest(HttpServletRequest request) 
    {
        super(request);
    }

    public HttpServletRequest getHttpServletRequest() 
    {
        return (HttpServletRequest)getRawRequest();
    }

    public String extractCallId()
    {
        HttpServletRequest request = getHttpServletRequest();

        String id = null;
        
        // 1) Query-Param
        id = request.getParameter(HttpConnection.HEADER_JADEX_CALLID);
        if (id != null && !id.isEmpty()) 
            return id;
        
        // 2) Header
        id = request.getHeader(HttpConnection.HEADER_JADEX_CALLID);
        if (id != null && !id.isEmpty()) 
            return id;
        
        return null;
    }

    public String extractSessionId() 
    {
        HttpServletRequest request = getHttpServletRequest();

        String id = null;
        
        // 1) Query-Param
        id = request.getParameter(HttpConnection.HEADER_JADEX_SESSIONID);
        if (id != null && !id.isEmpty()) 
            return id;
        
        // 2) Header
        id = request.getHeader(HttpConnection.HEADER_JADEX_SESSIONID);
        if (id != null && !id.isEmpty()) 
            return id;
        
        // 2) Cookie (cookies are not used currently)
        /*String cookie = request.getHeader("cookie");
        if (cookie != null) 
        {
            StringTokenizer stok = new StringTokenizer(cookie, ";");
            while (stok.hasMoreTokens()) 
            {
                String c = stok.nextToken();
                int del = c.indexOf("=");
                String name = c.substring(0, del).trim();
                if ("jadex".equals(name)) 
                {
                    id = c.substring(del + 1).trim();
                    break;
                }
            }
        }*/
        return id;
    }

    /*public Connection extractRequestConnection()
    {
        HttpServletRequest request = getHttpServletRequest();
        HttpConnection con = new HttpConnection(getCallId(), getAsyncContextInfo(request).getAsyncContext());
        return con;
    }*/

    public TransportType extractTransportType()
    {
        HttpServletRequest req = getHttpServletRequest();

        if(req == null)
            return TransportType.REST;

        // SSE via Accept-Header 
        String accept = req.getHeader("Accept");
        if(accept != null && accept.contains("text/event-stream")) 
        {
            return TransportType.SSE;
        }

        // todo: use convid!
        // Longpoll not standardized via Query-Parameter / Header
        String lpParam = req.getParameter("longpoll");
        if(lpParam != null && !lpParam.isEmpty())
            return TransportType.LONGPOLL;

        String lpHeader = req.getHeader("X-Longpoll");
        if(lpHeader != null && !lpHeader.isEmpty())
            return TransportType.LONGPOLL;

        if(req.getCookies() != null) 
        {
            for(var c : req.getCookies()) 
            {
                if("longpoll".equalsIgnoreCase(c.getName()))
                    return TransportType.LONGPOLL;
            }
        }

        return TransportType.REST;
    }

    /**
	 * Get the async
	 */
	public IAsyncContextInfo getAsyncContextInfo()
	{
        HttpServletRequest request = getHttpServletRequest();
		IAsyncContextInfo ret = (IAsyncContextInfo)request.getAttribute(IAsyncContextInfo.ASYNC_CONTEXT_INFO);
		
		// In case the call comes from an internally started server async is not already set
		// In case the call comes from an external web server it has to set the async in order
		// to let the call wait for async processing
		if(ret == null)
		{
			final AsyncContext rctx = request.startAsync();
			//System.out.println("ctx created: "+rctx+" "+request);
			final boolean[] complete = new boolean[1];
			AsyncListener alis = new AsyncListener()
			{
				public void onTimeout(AsyncEvent arg0) throws IOException
				{
				}

				public void onStartAsync(AsyncEvent arg0) throws IOException
				{
				}

				public void onError(AsyncEvent arg0) throws IOException
				{
				}

				public void onComplete(AsyncEvent arg0) throws IOException
				{
					//if(request.getRequestURI().indexOf("subscribe")!=-1)
					//	System.out.println("ctx complete: "+((HttpServletRequest)rctx.getRequest()).getRequestURI());
					complete[0] = true;
				}
			};
			rctx.addListener(alis);

			// Must be async because Jadex runs on other thread
			// tomcat async bug? http://jira.icesoft.org/browse/PUSH-116
			request.setAttribute(IAsyncContextInfo.ASYNC_CONTEXT_INFO, new IAsyncContextInfo()
			{
				public boolean isComplete()
				{
					return complete[0];
				}
				
				public AsyncContext getAsyncContext() 
				{
					return rctx;
				}
			});
		}
		
		return ret;
	}

}