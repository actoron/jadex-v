package jadex.publishservice.impl.v2.http;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jadex.collection.MultiCollection;
import jadex.common.transformation.IObjectStringConverter;
import jadex.common.transformation.STransformation;
import jadex.common.transformation.traverser.Traverser;
import jadex.core.IComponentManager;
import jadex.publishservice.impl.ContentNegotiator;
import jadex.publishservice.impl.MappingEvaluator.MappingInfo;
import jadex.publishservice.impl.v2.Connection;
import jadex.publishservice.impl.v2.Message;
import jadex.publishservice.impl.v2.Request;
import jadex.publishservice.impl.v2.TransportMode;
import jadex.publishservice.impl.v2.TransportType;
import jadex.publishservice.publish.IAsyncContextInfo;
import jadex.publishservice.publish.clone.CloneResponseProcessor;
import jadex.publishservice.publish.json.PublishJsonSerializer;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class HttpConnection extends Connection
{

	/** Http header for the Jadex version. */
	public static final String HEADER_JADEX_VERSION = "x-jadex-version";

	/** Client identifier. */
	public static final String HEADER_JADEX_SESSIONID = "x-jadex-sessionid";
	
	/** Http header for the call id (req and resp). */
	public static final String HEADER_JADEX_CALLID = "x-jadex-callid";

	/** Http header for the call id siganlling that this is the last response (resp). */
	public static final String HEADER_JADEX_CALLFINISHED = "x-jadex-callidfin";
	
	/** Http header for max value of intermediate future. */
	public static final String HEADER_JADEX_MAX = "x-jadex-max";
	
	/** Http header for the client side timeout of calls (req). */
	public static final String HEADER_JADEX_CLIENTTIMEOUT = "x-jadex-clienttimeout";
	
	/** Http header for the client side to indicate that conversation is still alive/ongoing. */
	public static final String HEADER_JADEX_ALIVE = "x-jadex-alive";
	public static final String HEADER_JADEX_SSEALIVE = "x-jadex-ssealive";
	
	/** Http header to terminate the call (req). */
	public static final String HEADER_JADEX_TERMINATE = "x-jadex-terminate";

	/** Http header to login to and gain admin access (req). */
	public static final String HEADER_JADEX_LOGIN = "x-jadex-login";
	public static final String HEADER_JADEX_LOGOUT = "x-jadex-logout";
	public static final String HEADER_JADEX_ISLOGGEDIN = "x-jadex-isloggedin";

    public static final String RESULT_TYPES = "result-types";
    public static final String FINISHED = "finished";
    public static final String MAX = "max";
    public static final String MAPPING_INFO = "mapping-info";
    
    public static MultiCollection<String, IObjectStringConverter> converters;

    static
    {
        converters = new MultiCollection<String, IObjectStringConverter>();
 
        PublishJsonSerializer jsonser = PublishJsonSerializer.get();
        
        //JsonResponseProcessor jrp = new JsonResponseProcessor();
        //jsonser.addProcessor(jrp, jrp);
        
        //serser.getCloneProcessors().add(0, new CloneResponseProcessor());
        //TODO: HACK! This is most definitely WRONG to do, make your own cloner, don't modify the global one.
        Traverser.getDefaultProcessors().add(0, new CloneResponseProcessor());
        
        //binser = (JadexBinarySerializer)serser.getSerializer(JadexBinarySerializer.SERIALIZER_ID);
        //BinaryResponseProcessor brp = new BinaryResponseProcessor();
        //binser.addProcessor(brp, brp);
        
        // System.out.println("added response processors for:
        // "+component.getId().getRoot());

        //sessions = new HashMap<String, Map<String,Object>>();// new HashMap<String, Map<String,Object>>();

        // todo: move this code out
        IObjectStringConverter jsonc = new IObjectStringConverter()
        {
            // todo: HACK use other configuration
            Map<String, Object> conv;
            {
                conv = new HashMap<>();
                conv.put("writeclass", false);
                conv.put("writeid", false);
            }
            
            public String convertObject(Object val, Object context)
            {
                // System.out.println("write response in json");
                
                byte[] data = jsonser.encode(val, IComponentManager.get().getClassLoader(), conv);
                //byte[] data = JsonTraverser.objectToByteArray(val, component.getClassLoader(), null, false, false, null, null, null);
                return new String(data, StandardCharsets.UTF_8);
            }
        };
        converters.add(MediaType.APPLICATION_JSON, jsonc);
        converters.add("*/*", jsonc);
        converters.add(MediaType.SERVER_SENT_EVENTS, jsonc);

        IObjectStringConverter jjsonc = new IObjectStringConverter()
        {
            public String convertObject(Object val, Object context)
            {
                byte[] data = jsonser.encode(val, IComponentManager.get().getClassLoader(), null);
                //byte[] data = JsonTraverser.objectToByteArray(val, component.getClassLoader(), null, true, true, null, null, null);
                String ret = new String(data, StandardCharsets.UTF_8);
                //System.out.println("rest json: "+ret);
                return ret;
            }
        };
        converters.add(STransformation.MediaType.APPLICATION_JSON_JADEX, jjsonc);
        converters.add("*/*", jjsonc);

        /*IObjectStringConverter xmlc = new IObjectStringConverter()
        {
            public String convertObject(Object val, Object context)
            {
                // System.out.println("write response in xml");

                byte[] data = JavaWriter.objectToByteArray(val, getClassLoader());
                return new String(data, StandardCharsets.UTF_8);
            }
        };
        converters.add(MediaType.APPLICATION_XML, xmlc);*/
        //converters.add("*/*", xmlc);
        

        IObjectStringConverter tostrc = new IObjectStringConverter()
        {
            public String convertObject(Object val, Object context)
            {
                //System.out.println("write response in plain text (toString)");
                return val.toString();
            }
        };
        converters.add(MediaType.TEXT_PLAIN, tostrc);
        converters.add("*/*", tostrc);

        //final Long to = (Long)Starter.getPlatformValue(component.getId(), Starter.DATA_DEFAULT_TIMEOUT);
        //System.out.println("Using default client timeout: " + to);

        /*requestspercall = new MultiCollection<String, AsyncContext>()
        {
            public java.util.Collection<AsyncContext> createCollection(final String callid)
            {
                return LeaseTimeSet.createLeaseTimeCollection(to, new ICommand<Tuple2<AsyncContext, Long>>()
                {
                    public void execute(Tuple2<AsyncContext, Long> tup)
                    {
                        System.out.println("rqcs: "+requestspercall.size()+" "+requestspercall);
                        System.out.println("cleaner remove: "+tup.getFirstEntity().hashCode());
                        System.out.println("rqcs: "+requestspercall.size()+" "+requestspercall);
                        // Client timeout (nearly) occurred for the request
                        System.out.println("sending timeout to client " + tup.getFirstEntity().getRequest());
                        writeResponse(null, Response.Status.REQUEST_TIMEOUT.getStatusCode(), callid, null, (HttpServletRequest)tup.getFirstEntity().getRequest(),
                            (HttpServletResponse)tup.getFirstEntity().getResponse(), false);
                        // ctx.complete();
                    }
                });
            }
        };*/
        // Problem with multicollection and leasetimeset is:
        // a) passive lease time set: does not check all sets when sth. changes in multicoll
        // b) active lease time set: does not work when multicol.remove(key) is used. The set does not know that it was removed
        
        /*requestspercall = new LeaseTimeMap(to, true, new ICommand<Tuple2<Entry<String, Collection<IAsyncContextInfo>>, Long>>()
        {
            public void execute(Tuple2<Entry<String, Collection<IAsyncContextInfo>>, Long> tup)
            {
                //System.out.println("rqcs: "+requestspercall.size()+" "+requestspercall);
                //System.out.println("cleaner remove: "+tup.getFirstEntity().hashCode());
                // Client timeout (nearly) occurred for the request
                
                String callid = tup.getFirstEntity().getKey();
                Collection<IAsyncContextInfo> ctxs = tup.getFirstEntity().getValue();
                if(ctxs!=null)
                {
                    for(IAsyncContextInfo ctx: ctxs)
                    {
                        //System.out.println("sending timeout to client " + ctx.getRequest());
                        //writeResponse(null, Response.Status.REQUEST_TIMEOUT.getStatusCode(), callid, null, (HttpServletRequest)ctx.getRequest(),
                        //	(HttpServletResponse)ctx.getResponse(), false, null);
                        //((HttpServletRequest)ctx.getRequest()).getAttribute(IAsyncContextInfo.ASYNC_CONTEXT_INFO).is
                        
                        // Only send timeout when context is still valid (not complete)
                        if(!ctx.isComplete())
                        {
                            System.out.println("cleaner remove: "+tup.getFirstEntity().hashCode());
                            writeResponse(new ResponseInfo().setStatus(Response.Status.REQUEST_TIMEOUT.getStatusCode())
                                .setCallid(callid).setRequest((HttpServletRequest)ctx.getAsyncContext().getRequest()).setResponse((HttpServletResponse)ctx.getAsyncContext().getResponse()).setFinished(false));
                        }
                        /*else
                        {
                            System.out.println("time outed ctx is already complete");
                        }* /
                    }
                }
                // ctx.complete();
            }
        });*/
    }

    /**
	 *  Get the converters.
	 *  
	 *  Needs not to be synchronized as long as only gets are performed.
	 */
	protected static Collection<IObjectStringConverter> getConverters(String mediatype)
	{
		return converters.get(mediatype);
	}
	

    public static class SendData
    {
        protected int status;
        
        protected Map<String,String> headers;
        
        protected String contentType;

        protected Object payload;

        public String getContentType() 
        {
            return contentType;
        }

        public Map<String, String> getHeaders() 
        {
            return headers;
        }

        public int getStatus() 
        {
            return status;
        }

        public void setContentType(String contentType) 
        {
            if(!contentType.contains("charset"))
                contentType = contentType + "; charset=utf-8";

            this.contentType = contentType;
        }

        public void addHeader(String name, String value) 
        {
            if(headers == null)
                headers = new HashMap<>();
            headers.put(name, value);
        }

        public void setStatus(int status) 
        {
            this.status = status;
        }

        public Object getPayload() 
        {
            return payload;
        }

        public void setPayload(Object payload) 
        {
            this.payload = payload;
        }
    }

    private final AsyncContext asyncContext;

    public HttpConnection(String id, Request request)
    {
        super(id, TransportType.REST);
        HttpRequest req = (HttpRequest)request;
        this.asyncContext = req.getAsyncContextInfo().getAsyncContext();

        supportedModes = Set.of(TransportMode.REQUEST_RESPONSE);
    }

    public AsyncContext getAsyncContext()
    {
        return asyncContext;
    }

    @Override
    public boolean send(Message message) throws Exception
    {
        boolean ret = false;
        SendData details = getSendDetails(message);

        try
        {
            HttpServletResponse resp =
                (HttpServletResponse) getAsyncContext().getResponse();

            writeResponse(resp, details);
            ret = true;
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            getAsyncContext().complete();
        }

        return ret;
    }

    protected void writeResponse(HttpServletResponse resp, SendData details) throws IOException
    {
        // Status
        if(details.getStatus() > 0)
            resp.setStatus(details.getStatus());

        // Headers
        if(details.getHeaders() != null)
        {
            for(Map.Entry<String,String> e : details.getHeaders().entrySet())
            {
                resp.setHeader(e.getKey(), e.getValue());
            }
        }

        // Content-Type
        if(details.getContentType() != null)
            resp.setContentType(details.getContentType());

        // Write payload
        if(details.getPayload() != null)
        {
            String output;
            if(details.getPayload() instanceof byte[])
            {
                output = new String((byte[])details.getPayload(), StandardCharsets.UTF_8);
            }
            else if(details.getPayload() instanceof String)
            {
                output = (String)details.getPayload();
            }
            else
            {
                System.out.println("cannot convert result, writing as string: " + details.getPayload());
                output = details.getPayload().toString();
            }

            try(PrintWriter w = resp.getWriter())
            {
                w.write(output);
                w.flush();
            }
        }
    }

    protected SendData getSendDetails(Message message) throws Exception
    {
        SendData details = new SendData();

        Request req = message.getRequest();
        HttpServletRequest request = (HttpServletRequest) req.getRawRequest();
        Object payload = message.getResult().getPayload();

        // JAX-RS Response handling
        if(payload instanceof jakarta.ws.rs.core.Response)
        {
            jakarta.ws.rs.core.Response resp = (jakarta.ws.rs.core.Response)payload;

            // copy headers
            for(String name : resp.getStringHeaders().keySet())
            {
                details.addHeader(name, resp.getHeaderString(name));
            }

            // status
            details.setStatus(resp.getStatus());

            // payload entity
            details.setPayload(resp.getEntity());

            if(resp.getMediaType() != null)
                details.addHeader(HttpHeaders.CONTENT_TYPE, resp.getMediaType().toString());
        }
        else
        {
            // Default status
            int status = message.isError() ? 500 : 200;
            details.setStatus(status);
            
            // Content negotiation
            String accept = request.getHeader(HttpHeaders.ACCEPT);
			//if("*/*".equals(accept)) // ignore wildcard only
			//	accept = null;

	        // Mapping produced types (explicit capabilities)
	        List<String> produced = message.getMeta().get(MAPPING_INFO) == null
	            ? Collections.emptyList()
	            : ((MappingInfo)message.getMeta().get(MAPPING_INFO)).getProducedMediaTypes();

            // Start with server preference / existing result types (ordered fallback)
	        List<String> serverpref = message.getMeta().get(RESULT_TYPES) != null
	            ? new ArrayList<>((List<String>)message.getMeta().get(RESULT_TYPES))
	            : new ArrayList<>();

            //List<String> produced = (List<String>)message.getMeta().getOrDefault(
            //    "producedMediaTypes", Collections.emptyList());

            //List<String> serverPref = (List<String>)message.getMeta().getOrDefault(
            //    "resultTypes", new ArrayList<>());

            Optional<String> chosen = ContentNegotiator.negotiate(accept, produced, serverpref);

            if(chosen.isPresent() && !"*/*".equals(chosen.get()))
            {
                details.setContentType(chosen.get());
            }
            else
            {
                if(payload instanceof String)
                    details.setContentType(MediaType.TEXT_PLAIN);
                else
                    details.setContentType(MediaType.APPLICATION_JSON);
            }
        }

        // Jadex call metadata
        String callId = req.getCallId();
        Boolean finished = (Boolean)message.getMeta().get(FINISHED); 
        Integer max = (Integer)message.getMeta().get(MAX);

        if(callId != null)
        {
            if(Boolean.TRUE.equals(finished))
                details.addHeader(HEADER_JADEX_CALLFINISHED, callId);
            else
                details.addHeader(HEADER_JADEX_CALLID, callId);

            if(max != null)
                details.addHeader(HEADER_JADEX_MAX, String.valueOf(max));
        }

        // CORS / caching
        details.addHeader("Access-Control-Allow-Origin", "*");
        details.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");

        // store headers in message meta
        //message.getMeta().put("headers", headers);

        if(payload instanceof byte[])
        {
            if(message.getMeta().get(HttpHeaders.CONTENT_TYPE) == null 
                && message.getMeta().get(RESULT_TYPES)!=null && ((Collection<String>)message.getMeta().get(RESULT_TYPES)).size()>0)
            {
                details.setContentType(((Collection<String>)message.getMeta().get(RESULT_TYPES)).iterator().next());
                details.setPayload(payload);
            }
        }
        else
        {
            if(payload != null)
            {
                String ret = null;
                String mt = null;
                //if(ri.getResultTypes() != null)
                
                if(details.getContentType() != null)
                {
                    // try to find converter for acceptable mime types
                    String mediatype = details.getContentType();
                    mediatype = mediatype.trim(); // e.g. sent with leading space from edge, grrr
                    Collection<IObjectStringConverter> convs = getConverters(mediatype);
                    if(convs != null && convs.size() > 0)
                    {
                        mt = mediatype;
                        Object input = payload instanceof Response ? ((Response)payload).getEntity() : payload;
                        ret = convs.iterator().next().convertObject(input, null);
                    }

                    details.setPayload(ret);
                }
                else
                {
                    //throw new RuntimeException("No content type specified for response and no default could be determined: "+details.getPayload());
                    details.setContentType(MediaType.TEXT_PLAIN);

                    if(!(payload instanceof String) && !(payload instanceof Response))
                        System.out.println("cannot convert result, writing as string: " + payload);

                    // Important: writer access must be deferred to happen after setting charset!
                    ret = payload instanceof Response ? "" + ((Response)payload).getEntity() : payload.toString();

                    details.setPayload(ret);
                }
            }  
        }

        return details;
    }

}