package jadex.publishservice.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import jadex.collection.MultiCollection;
import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.common.Tuple2;
import jadex.common.transformation.BasicTypeConverter;
import jadex.common.transformation.IObjectStringConverter;
import jadex.common.transformation.IStringConverter;
import jadex.common.transformation.STransformation;
import jadex.common.transformation.traverser.ITraverseProcessor;
import jadex.common.transformation.traverser.Traverser;
import jadex.core.IComponentHandle;
import jadex.core.impl.ComponentManager;
import jadex.future.Future;
import jadex.future.FutureTerminatedException;
import jadex.future.IFuture;
import jadex.future.IIntermediateFuture;
import jadex.future.IIntermediateFutureCommandResultListener;
import jadex.future.IResultListener;
import jadex.future.ITerminableFuture;
import jadex.javaparser.SJavaParser;
import jadex.providedservice.IService;
import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.impl.service.ServiceCall;
import jadex.publishservice.impl.RequestManager.MappingInfo.HttpMethod;
import jadex.publishservice.impl.RequestManager.ResponseInfo;
import jadex.publishservice.publish.IAsyncContextInfo;
import jadex.publishservice.publish.PathManager;
import jadex.publishservice.publish.annotation.ParametersMapper;
import jadex.publishservice.publish.annotation.ResultMapper;
import jadex.publishservice.publish.clone.CloneResponseProcessor;
import jadex.publishservice.publish.json.PublishJsonSerializer;
import jadex.publishservice.publish.mapper.DefaultParameterMapper;
import jadex.publishservice.publish.mapper.IParameterMapper;
import jadex.publishservice.publish.mapper.IParameterMapper2;
import jadex.publishservice.publish.mapper.IValueMapper;
import jadex.transformation.jsonserializer.JsonTraverser;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 *  The request manager is used to handle REST web requests.
 *  It follows tha Jadex manager pattern, i.e. it is a singleton accessible
 *  also from static contexts. 
 *  
 *  - Requests with jadex.js contained will be delivered the jadex.js file.
 *  
 *  - Requests on the base url of a published service will be delivered an
 *    overview site for the service.
 *  
 *  The request manager has a sophisticated mapping mechanisms for request
 *  paramaters and tries to map naturally as much as possible.
 *  
 *  There is (nearly) a 1:1 relationship between sessions and sseinfos. Each jadex.js 
 *  uses the "jadex" cookie for session id. The cookie is path wide (from a path all subpaths)
 *  so that multiple jadex.js and thus applications can coexist using different sessions.
 *  Sessions do not need explicit timeouts as SSE pings are sent to the see clients.
 *  Each client responds via a call to baseurl/ssealive path, which must be routed to
 *  the request manager.
 *  
 */
public class RequestManager 
{
	/** The default host name. */
	public static final String DEFAULT_HOST = "DEFAULTHOST";
	
	/** The default port. */
	public static final int DEFAULT_PORT = 0;
	
	/** The default app name. */
	public static final String DEFAULT_APP = "DEFAULTAPP";
	
	/** The default hostportappcontext. */
	public static final String DEFAULT_COMPLETECONTEXT = "http://"+DEFAULT_HOST+":"+DEFAULT_PORT+"/"+DEFAULT_APP+"/";
	
	/** The conversation timeout. */
	public static long CONVERSATION_TIMEOUT = 30000;
	
	/** Async context info. */
	public static final String ASYNC_CONTEXT_INFO = "__cinfo";

	/** Http header for the Jadex version. */
	public static final String HEADER_JADEX_VERSION = "x-jadex-version";

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
	
	/** Finished result marker. */
	public static final String FINISHED	= "__finished__";
	
	/** URL parameter random request. */
	public static final String RANDOM = "__random";
	
	/** URL parameter type request. */
	public static final String CONTENTTYPE = "contenttype";
	
	/** URL parameter accept request. */
	public static final String ACCEPT = "__accept__";

	/** Some basic media types for service invocations. */
	public static final List<String> PARAMETER_MEDIATYPES = Arrays.asList(new String[]{MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML});

	
	/** The state is thread protected via synchronized */
	
	/** Info about an ongoing conversation, i.e. Jadex future, session etc. */
	protected Map<String, ConversationInfo> conversationinfos;

	/** SSE events that could not directly be sent. */
	protected List<SSEEvent> sseevents;
	
	/** The sessions. Cannot use built-in due to problem with concurrent inital requests (and also after platform restart). 
	    Two requests (sse, subscribeX) arrive at server with the same jsessionid -> session not known, server
	    creates a new one for both with different ids.
	    https://community.oracle.com/tech/developers/discussion/1455774/multiple-sessions-created. 
	 */
	protected Map<String, Map<String, Object>> sessions;
	
	/** Infos about the sse sources. */
	protected Map<String, SSEInfo> sseinfos;
			
	/** State end */

	
	/** The media type converters. */
	protected MultiCollection<String, IObjectStringConverter> converters;
	
	/** Login security of or off. */
	protected boolean loginsec;

	/** The json processor. */
	protected PublishJsonSerializer jsonser;
	
	/** The basic type converter. */
	protected BasicTypeConverter basicconverter = new BasicTypeConverter();
	
	///** The binary processor. */
	//protected JadexBinarySerializer binser;
	
	protected static RequestManager instance;
	
	public static synchronized void createInstance()
	{
		if(instance==null)
			instance = new RequestManager();
	}
	
	public static synchronized RequestManager getInstance()
	{
		if(instance==null)
			throw new RuntimeException("request manager was not created");
		return instance; 
	}
	
	public static class Resp
	{
		protected Map<String, Object> headers;
		protected Object entity;
		protected int status;
		
		public Resp()
		{
		}

		public Map<String, Object> getHeaders() 
		{
			return headers;
		}

		public Resp setHeaders(Map<String, Object> headers) 
		{
			this.headers = headers;
			return this;
		}

		public Object getEntity() 
		{
			return entity;
		}

		public Resp setEntity(Object entity) 
		{
			this.entity = entity;
			return this;
		}

		public int getStatus() 
		{
			return status;
		}

		public Resp setStatus(int status) 
		{
			this.status = status;
			return this;
		}
	}
	
	public RequestManager()
	{
		conversationinfos = new LinkedHashMap<String, ConversationInfo>();
		sseevents = new ArrayList<SSEEvent>();
		sseinfos = new HashMap<String, RequestManager.SSEInfo>();
		//sessions = new LeaseTimeMap<String, Map<String,Object>>(1000*60*20); // 20 min session timeout
		//sessions = new PassiveLeaseTimeMap<String, Map<String,Object>>(1000*60*1); // 20 min session timeout
		sessions = new HashMap<String, Map<String,Object>>();
		
		this.loginsec = false;
		
		// Add rs 'Response' converters
		// todo: support for xml ?!
		// todo: use preprocessors (would work for all serializers) ?!
		//ISerializationServices ss = getComponent().getFeature(IMjProvidedServiceFeature.class).getSerializationService();
		//SerializationServices.getSerializationServices(self.getId());
		
		jsonser = PublishJsonSerializer.get();
		
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

		converters = new MultiCollection<String, IObjectStringConverter>();
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
				
				byte[] data = jsonser.encode(val, getClassLoader(), conv);
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
				byte[] data = jsonser.encode(val, getClassLoader(), null);
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
				System.out.println("write response in plain text (toString)");
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
	 * Add a converter for one or multiple types.
	 * /
	public void addConverter(String[] mediatypes, IObjectStringConverter converter)
	{
		for(String mediatype : mediatypes)
		{
			converters.add(mediatype, converter);
		}
	}*/

	/**
	 * Remove a converter.
	 * 
	 * @param converter The converter.
	 * /
	public void removeConverter(String[] mediatypes, IObjectStringConverter converter)
	{
		for(String mediatype : mediatypes)
		{
			converters.removeObject(mediatype, converter);
		}
	}*/
	
	/**
	 *  Get the converters.
	 *  
	 *  Needs not to be synchronized as long as only gets are performed.
	 */
	protected Collection<IObjectStringConverter> getConverters(String mediatype)
	{
		return converters.get(mediatype);
	}
	
	/**
	 *  Turn on or off the login security.
	 *  If true one has to log in with platform secret before using published services.
	 *  @param sec On or off.
	 */
	public IFuture<Void> setLoginSecurity(boolean sec)
	{
		this.loginsec = sec;
		return IFuture.DONE;
	}
	
	/**
	 *  Get a session.
	 *  @param request The request
	 *  @param create Flag if shall be created.
	 *  @return The session.
	 */
	public Map<String, Object> getSession(HttpServletRequest request, boolean create)
	{
		String id = getSessionId(request);
		return getSession(id, create);
	}
	
	/**
	 *  Get a session.
	 *  @param request The request
	 *  @param create Flag if shall be created.
	 *  @return The session.
	 */
	public synchronized Map<String, Object> getSession(String sessionid, boolean create)
	{
		if(sessionid==null && !create)
			return null;
		else if(sessionid==null)
			throw new RuntimeException("Session id null, no jadex cookie for session provided by client");
		
		Map<String, Object> session = sessions.get(sessionid);
		
		if(session==null && create)
		{
			session = new HashMap<String, Object>();
			sessions.put(sessionid, session);
		}
	
		return session;
	}
	
	public synchronized AsyncContext getSSEContextFromSession(String sessionid)
	{
		if(sessionid==null)
			return null;
		
		Map<String, Object> session = sessions.get(sessionid);
		return session!=null? (AsyncContext)session.get("sse"): null;
	}
	
	public synchronized void putSSEContextInSession(String sessionid, AsyncContext context)
	{
		setInSession(sessionid, "sse", context);
	}
	
	public synchronized void setInSession(String sessionid, String name, Object value)
	{
		getSession(sessionid, true).put(name, value);
	}
	
	public synchronized Map<String, Object> getSession(String sessionid)
	{
		return sessions.get(sessionid);
	}
	
	public synchronized void putSession(String sessionid, Map<String, Object> session)
	{
		if(SUtil.DEBUG)
			System.out.println("adding session: "+sessionid+" "+session);
		sessions.put(sessionid, session);
	}
	
	public synchronized void removeSession(String sessionid)
	{
		Map<String, Object> session = sessions.remove(sessionid);
		if(SUtil.DEBUG)
			System.out.println("removing session: "+sessionid+" "+session);
	}
	
	/**
	 *  Get a session id.
	 *  @param request The request
	 *  @return The session id.
	 */
	public static String getSessionId(HttpServletRequest request)
	{
		String cookie = request.getHeader("cookie");
		String id = null;
		
		if(cookie!=null)
		{
			StringTokenizer stok = new StringTokenizer(cookie, ";");
			while(stok.hasMoreTokens())
			{
				String c = stok.nextToken();

				int del = c.indexOf("=");
				String name = c.substring(0, del).trim();
				
				if("jadex".equals(name))
				{
					id = c.substring(del+1, c.length()).trim();
					break;
				}
			}
		}
		
		return id;
	}
		
	/**
	 * Handle a web request.
	 * 
	 * @param service The service.
	 * @param mappings The collected mapping infos for the service.
	 * @param request The request.
	 * @param response The response.
	 */
	public void handleRequest(IService service, PathManager<MappingInfo> pm, final HttpServletRequest request, final HttpServletResponse response, Object[] others)
		throws IOException, ServletException
	{
		ResponseInfo ri = new ResponseInfo().setRequest(request).setResponse(response);
		
		/*if(!getComponent().getFeature(IMjExecutionFeature.class).isComponentThread())
		{
			getComponent().getFeature(IMjExecutionFeature.class).scheduleStep(IComponent ->
			{
				try
				{
					handleRequest(service, pm, request, response, others);
					return IFuture.DONE;
				}
				catch(Exception e)
				{
					return new Future<Void>(e);
				}
			
			}).get(); // Hack??? should not be other component thread
			return;
		}*/

		pruneObsoleteConversations();
		
		// https://stackoverflow.com/questions/14139753/httpservletrequest-getsessiontrue-thread-safe
		// solution: always create on container thread and remember
		//final Map<String, Object> session = getSession(request, true);
		final String sessionid = getSessionId(request);
		
		if(sessionid==null && request.getRequestURI().indexOf("jadex.js")==-1 && ri.isSSERequest())
			System.out.println("Call has no jadex session id, Jadex cookie missing: "+request.getRequestURL());
		
		// todo: if missing generate one?! as it is cookie it would be used by further requests
		
		//if(request.getRequestURI().indexOf("jadex.js")!=-1)
		//System.out.println("handleRequest: "+request.getRequestURI()+" session: "+request.getSession().getId()+" "+request.getHeader(HEADER_JADEX_SSEALIVE));
		//System.out.println("handleRequest: "+request.getRequestURI());
		
		getAsyncContextInfo(request); // ensure async request processing

		// System.out.println("handler is: "+uri.getPath());
		String callid = request.getHeader(HEADER_JADEX_CALLID);
		ri.setCallid(callid);
		
		// update on alive and all incoming requests with that callid
		ConversationInfo cinfo = getConversation(callid);
		if(cinfo!=null)
		{
			//System.out.println("timestamp updated: "+callid);
			cinfo.updateTimestamp();
		}
		
		String alive = request.getHeader(HEADER_JADEX_ALIVE);
		String ssealive = request.getHeader(HEADER_JADEX_SSEALIVE);

		//if(ssealive!=null)
		//	System.out.println("handleRequest ssealive: "+request.getRequestURI()+" session: "+request.getSession().getId());

		
		// check if it is a login request
		String secret = request.getHeader(HEADER_JADEX_LOGIN);
		String logout = request.getHeader(HEADER_JADEX_LOGOUT);
		String isloggedin = request.getHeader(HEADER_JADEX_ISLOGGEDIN);
		if(secret!=null)
		{
			login(request, secret).then((Boolean ok) ->
			{
				if(ok)
					//writeResponse(Boolean.TRUE, Response.Status.OK.getStatusCode(), callid, null, request, response, true, null);
					writeResponse(ri.setResult(Boolean.TRUE).setStatus(Response.Status.OK.getStatusCode()).setFinished(true));
				else
					//writeResponse(Boolean.FALSE, Response.Status.UNAUTHORIZED.getStatusCode(), callid, null, request, response, true, null);
					writeResponse(ri.setResult(Boolean.FALSE).setStatus(Response.Status.UNAUTHORIZED.getStatusCode()).setFinished(true));
			}).catchEx((Exception e) ->
			{
				//writeResponse(new SecurityException("Login failed"), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), null, null, request, response, true, null);
				writeResponse(ri.setResult(new SecurityException("Login failed")).setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).setFinished(true));
			});
		}
		else if(logout!=null)
		{
			logout(request).then((Boolean ok) ->
			{
				//writeResponse(ok, Response.Status.OK.getStatusCode(), callid, null, request, response, true, null);
				writeResponse(ri.setResult(ok).setStatus(Response.Status.OK.getStatusCode()).setFinished(true));

			}).catchEx((Exception e) ->
			{
				//writeResponse(new SecurityException("Logout failed"), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), callid, null, request, response, true, null);
				writeResponse(ri.setResult(new SecurityException("Logout failed")).setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).setFinished(true));
			});
		}
		else if(isloggedin!=null)
		{
			boolean ret = isLoggedIn(request);
			//writeResponse(ret, Response.Status.OK.getStatusCode(), callid, null, request, response, true, null);
			writeResponse(ri.setResult(ret).setStatus(Response.Status.OK.getStatusCode()).setFinished(true));
		}
		else if(ssealive!=null)
		{
			// update all timer values of session conversations and remove checking sseinfo if alive is received
			if(cinfo!=null)
			{
				//System.out.println("received ssealive: "+cinfo.getSessionId());
				updateTimestamps(cinfo.getSessionId());
				removeSSEInfo(cinfo.getSessionId());
				writeResponse(ri.setStatus(Response.Status.OK.getStatusCode()).setFinished(true));
			}
			else
			{
				System.out.println("callid not found for alive: "+callid);
				writeResponse(ri.setStatus(Response.Status.NOT_FOUND.getStatusCode()).setFinished(true));
			}
		}
		else if(alive!=null)
		{
			// update on alive and all incoming requests with that callid
			//ConversationInfo cinfo = conversationinfos.get(callid);
			if(cinfo!=null)
			{
				boolean aliveb = Boolean.parseBoolean(alive);
				if(!aliveb)
				{
					terminateConversation(cinfo, new RuntimeException("Terminated from client"), true);
				}
				// Already done above
				/*else
				{
					//System.out.println("timestamp updated: "+callid);
					cinfo.updateTimestamp();
				}*/
				writeResponse(ri.setStatus(Response.Status.OK.getStatusCode()).setFinished(true));
			}
			else
			{
				System.out.println("callid not found for alive: "+callid);
				writeResponse(ri.setStatus(Response.Status.NOT_FOUND.getStatusCode()).setFinished(true));
			}
		}
		else
		{
			// check if call is an intermediate result fetch
			String terminate = request.getHeader(HEADER_JADEX_TERMINATE);
	
			//System.out.println("handleRequest: "+callid+" "+terminate);
			
			// request info manages an ongoing conversation
			if(cinfo!=null)
			//if(conversationinfos.containsKey(callid))
			{
				//ConversationInfo rinfo = conversationinfos.get(callid);
	
				// Terminate the future if requested
				if(terminate!=null && cinfo.getFuture() instanceof ITerminableFuture)
				{
					//System.out.println("Terminating call on client request: "+callid);
					// hmm, immediate response (should normally not be necessary)
					// otherwise a (termination) exception is returned
					//writeResponse(FINISHED, callid, rinfo.getMappingInfo(), request, response, false);
					
					// save context to answer request after future is set
					
					//IAsyncContextInfo ctx = getAsyncContextInfo(request);
					//saveRequestContext(callid, ctx);
					
					//System.out.println("request terminate conversation: "+callid);
					
					terminateConversation(cinfo, null, true);
					
					/*if(!"true".equals(terminate))
						((ITerminableFuture)rinfo.getFuture()).terminate(new RuntimeException(terminate)); 
					else
						((ITerminableFuture)rinfo.getFuture()).terminate();
					
					conversationinfos.remove(callid);*/
					
					//if(callid.indexOf("subscribeToPlatforms")!=-1)
					//	System.out.println("Removed connection: "+callid);
					
					writeResponse(ri.setStatus(Response.Status.OK.getStatusCode()).setFinished(true));
				}
				else if(terminate!=null && cinfo.getFuture()==null)
				{
					cinfo.setTerminated(true);
					writeResponse(ri.setStatus(Response.Status.OK.getStatusCode()).setFinished(true));
				}
				else if(terminate!=null)
				{
					System.out.println("Future cannot be terminated: "+callid+" "+request);
					writeResponse(ri.setStatus(Response.Status.NOT_FOUND.getStatusCode()).setFinished(true));
				}
							
				// Result already available?
				/*else if(rinfo.checkForResult())
				{
					// Normal result (or FINISHED as handled in writeResponse())
					Object result = rinfo.getNextResult();
					result = FINISHED.equals(result) ? result : mapResult(rinfo.getMappingInfo().getMethod(), result);
					//writeResponse(result, callid, rinfo.getMappingInfo(), request, response, false, null);
					writeResponse(ri.setResult(result).setMappingInfo(rinfo.getMappingInfo()).setFinished(false));
				}
	
				// Exception in mean time?
				else if(rinfo.getException() != null)
				{
					Object result = mapResult(rinfo.getMappingInfo().getMethod(), rinfo.getException());
					//writeResponse(result, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), callid, rinfo.getMappingInfo(), request, response, true, null);
					writeResponse(ri.setResult(result).setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).setMappingInfo(rinfo.getMappingInfo()).setFinished(true));
				}*/
	
				// No result yet -> store current request context until next result available
				/*else
				{
					IAsyncContextInfo ctx = getAsyncContextInfo(request);
					saveRequestContext(callid, ctx);
				}*/
				
				//System.out.println("received existing call: "+request+" "+callid);
			}
			/*else if(callid != null) // callid is now generated by client for subscriptions
			{
				// System.out.println("callid not found: "+callid);
	
				//writeResponse(null, Response.Status.NOT_FOUND.getStatusCode(), callid, null, request, response, true, null);
				writeResponse(ri.setStatus(Response.Status.NOT_FOUND.getStatusCode()).setFinished(true));
	
				// if(request.isAsyncStarted())
				// request.getAsyncContext().complete();
			}*/
			// handle new call
			else
			{
				//System.out.println("received new call: "+request);
					
				String methodname = request.getPathInfo();
	
				if(methodname != null && methodname.startsWith("/"))
					methodname = methodname.substring(1);
				if(methodname != null && methodname.endsWith("()"))
					methodname = methodname.substring(0, methodname.length() - 2);
				final String fmn = methodname;

				//if(methodname!=null && request.toString().indexOf("efault")!=-1)
				//	System.out.println("INVOKE: " + methodname);
				
				Collection<MappingInfo> mis = pm!=null? pm.getElementsForPath(methodname): new ArrayList<MappingInfo>();
				List<Map<String, String>> bindings = mis.stream().map(x -> pm.getBindingsForPath(fmn)).collect(Collectors.toList());
				
				if(methodname.endsWith("jadex.js"))
				{
					loadJadexJS().then(js -> writeResponse(ri.setStatus(Response.Status.OK.getStatusCode()).setFinished(true).setResult(js)));
				}
				else if(mis!=null && mis.size()>0)
				{
					//if(request.toString().indexOf("gen")!=-1)
					//	System.out.println("call 4: "+request);
					
					// convert and map parameters
					Tuple2<MappingInfo, Object[]> tup = mapParameters(request, mis, bindings);
					final MappingInfo mi = tup.getFirstEntity();
					Object[] params = tup.getSecondEntity();
					
					//System.out.println("handleRequest: "+mi.getMethod());
					
					//if(mi.getMethod().toString().indexOf("Display")!=-1)
					//	System.out.println("heeereeee");
					
					//if(mi.getMethod().toString().indexOf("isAvailable")!=-1)
					//	System.out.println("params: "+Arrays.toString(params));
	
					//final ConversationInfo cinfo = new ConversationInfo(getSession(request, true));
					final String fcallid = callid==null? SUtil.createUniqueId(fmn): callid;
					final ConversationInfo mycinfo = new ConversationInfo(fcallid, sessionid);
					addConversation(fcallid, mycinfo);
					//conversationinfos.put(fcallid, mycinfo);
	
					// Check security
					//if(request.toString().indexOf("suspend")!=-1)
					//	System.out.println("call 2: "+request);

					// Get provider of service and check access
					IComponentHandle comp = ComponentManager.get().getComponentHandle(service.getServiceId().getProviderId());
					
					comp.scheduleStep(agent ->
					{
						// Inject caller meta info
						Map<String, String> callerinfos = extractCallerValues(request);
						ServiceCall.getOrCreateNextInvocation().setProperty("webcallerinfos", callerinfos);
						
						ServiceCall.getOrCreateNextInvocation().setProperty("callid", fcallid);
						ri.setCallid(fcallid);
					
//						BasicService.isUnrestricted(service.getServiceId(), comp, mi.getMethod())
//						.then((Boolean unres) ->
//						{
							//if(request.toString().indexOf("suspend")!=-1)
							//	System.out.println("call 3: "+request);
							try
							{
								if(loginsec /* && !unres */ && !isLoggedIn(request))
								{
									//writeResponse(new SecurityException("Access not allowed as not logged in"), Response.Status.UNAUTHORIZED.getStatusCode(), null, mi, request, response, true, null);
									writeResponse(ri.setResult(new SecurityException("Access not allowed as not logged in")).setStatus(Response.Status.UNAUTHORIZED.getStatusCode()).setMappingInfo(mi).setFinished(true));
								}
								else
								{
									// *****
									// invoke the service method
									// *****
	
									final Method method = mi.getMethod();
									
									//System.out.println("request: "+request.getRequestURL()+" "+fcallid+" "+method.getName()+" "+Arrays.toString(params));
									
									//if(request.toString().indexOf("generateArea")!=-1)
									//	System.out.println("call 4: "+request);
									final Object ret = method.invoke(service, params);
									ri.setMethod(method);
	
									if(ret instanceof IFuture)
										mycinfo.setFuture((IFuture<?>)ret);
	
									// Call can already be terminated from client
									if(mycinfo.isTerminated())
									{
										if(ret instanceof ITerminableFuture)
											((ITerminableFuture)ret).terminate();
										else
											System.out.println("Call cannot be terminated, future not terminable: "+method+" "+callid);
									}
									
									if(ret instanceof IIntermediateFuture)
									{
										writeResponse(ri.setResult("sse").setStatus(Response.Status.OK.getStatusCode()).setMappingInfo(mi).setFinished(true));
	
										/*if(session.getAttribute("sse")!=null)
										{
											writeResponse(ri.setResult("sse").setStatus(Response.Status.OK.getStatusCode()).setMappingInfo(mi).setFinished(true));
										}
										else
										{	
											//IAsyncContextInfo ctx = getAsyncContextInfo(request);
											//saveRequestContext(fcallid, ctx);
											rinfo = new ConversationInfo(mi, (IFuture)ret);
											requestinfos.put(fcallid, rinfo);
										}*/
										
										// System.out.println("added context: "+fcallid+""+ctx);
				
										((IIntermediateFuture<Object>)ret)
											.addResultListener(
											//createResultListener(
											new IIntermediateFutureCommandResultListener<Object>()
										{
											public void resultAvailable(Collection<Object> result)
											{
												// Shouldn't be called?
												for(Object res : result)
												{
													intermediateResultAvailable(res);
												}
												finished();
											}
			
											public void exceptionOccurred(Exception exception)
											{
												handleResult(null, exception, null, null);
											}
			
											public void intermediateResultAvailable(Object result)
											{
												//System.out.println("intermediate: "+result);
												
												handleResult(result, null, null, null);
											}
			
											@Override
											public void commandAvailable(Object command)
											{
												handleResult(null, null, command, null);
											}
			
											public void finished()
											{
												// maps will be cleared when processing fin
												// element in writeResponse
												handleResult(FINISHED, null, null, null);
											}
											
											public void maxResultCountAvailable(int max)
											{											
												handleResult(null, null, null, max);
											}
											
											/**
											 * Handle a final or intermediate
											 * result/exception/command of a service call.
											 */
											protected void handleResult(Object result, Throwable exception, Object command, Integer max)
											{
												//if(result!=null)
												//	System.out.println("handleResult: "+result);
												
												//if(max!=null)
												//if(command==null)
												//	System.out.println("handleResult:"+result+", "+exception+", "+command+", sse:"+(session.getAttribute("sse")!=null));
												
												//if(command!=null)
												//	return; // skipping commands (e.g. updatetimer)
												
												/*if(exception instanceof FutureTerminatedException)
												{
													System.out.println("suppressed future terminated exception");
													return;
												}*/
												
												ResponseInfo ri = new ResponseInfo().setCallid(fcallid).setMappingInfo(mi).setMethod(method);
												
												if(FINISHED.equals(result))
												{
													ri.setFinished(true);
												}
												else if(exception!=null)
												{
													ri.setException((Exception)exception);
													
													int rescode = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
													if(exception instanceof FutureTerminatedException)
														rescode = Response.Status.OK.getStatusCode();
													
													ri.setStatus(rescode);
												}
												else if(command!=null)
												{
													// Command is just a enum type for updatetimer in IForwardCommandFuture
													//System.out.println("received command: "+command);
													ri.setResult(command);
													ri.setStatus(202);
												}
												else
												{
													ri.setResult(result);
												}
												
												if(max!=null)
													ri.setMax(max);
												
												//final Map<String, Object> session = getSession(sessionid, true);
												//AsyncContext ctx = (AsyncContext)session.get("sse");
												AsyncContext ctx = getSSEContextFromSession(sessionid);
												if(ctx!=null)
												{
													ri.setRequest((HttpServletRequest)ctx.getRequest());
													ri.setResponse((HttpServletResponse)ctx.getResponse());
												}
												else
												{
													System.out.println("No sse connection, delay sending: "+result+" "+sessionid);
													addSSEEvent(createSSEEvent(ri));
													return;
												}
												
												if(mycinfo!=null && mycinfo.isTerminated())
												{
													// nop -> ignore late results (i.e. when terminated due to browser offline).
													//System.out.println("ignoring late result: "+result);
												}
												else
												{
													writeResponse(ri);
												}
												
												// Browser waiting for result -> send immediately
												/*else if(requestspercall.containsKey(fcallid) && requestspercall.get(fcallid).size() > 0)
												{
													Collection<IAsyncContextInfo> cls = requestspercall.get(fcallid);
													
													// System.out.println("direct answer to browser request, removed context:"+callid+" "+ctx);
													if(command != null)
													{
														// Timer update (or other command???)
														// HTTP 102 -> processing (not recognized by angular?)
														// HTTP 202 -> accepted
														IAsyncContextInfo ctx = cls.iterator().next();
														cls.remove(ctx);
														
														if(!ctx.isComplete())
														{
															//writeResponse(null, 202, fcallid, mi, (HttpServletRequest)ctx.getRequest(), (HttpServletResponse)ctx.getResponse(), false, null);
															writeResponse(new ResponseInfo().setStatus(202).setCallid(fcallid).setMappingInfo(mi)
																.setRequest((HttpServletRequest)ctx.getAsyncContext().getRequest()).setResponse((HttpServletResponse)ctx.getAsyncContext().getResponse()).setFinished(false));
														}
													}
													else if(exception != null)
													{
														// Service call (finally) failed.
														result = mapResult(method, exception);
														
														int rescode = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
														if(exception instanceof FutureTerminatedException)
															rescode = Response.Status.OK.getStatusCode();
														
														//writeResponse(result, rescode, fcallid, mi, (HttpServletRequest)ctx.getRequest(),
														//	(HttpServletResponse)ctx.getResponse(), true);
														
														// Answer ALL pending requests and so also remove waiting longpoll calls (e.g. when terminate arrives)
														AsyncContext[] acs = cls.toArray(new AsyncContext[cls.size()]);
														for(AsyncContext ac: acs)
														{
															//writeResponse(result, rescode, fcallid, mi, (HttpServletRequest)ac.getRequest(),
															//	(HttpServletResponse)ac.getResponse(), true, null);
															writeResponse(new ResponseInfo(result).setStatus(rescode).setCallid(fcallid).setMappingInfo(mi)
																.setRequest((HttpServletRequest)ac.getRequest()).setResponse((HttpServletResponse)ac.getResponse()).setFinished(true));
	
															cls.remove(ac);
														}
													}
													else
													{
														IAsyncContextInfo ctx = cls.iterator().next();
														cls.remove(ctx);
														// Normal result (or FINISHED as handled in writeResponse())
														result = FINISHED.equals(result) ? result : mapResult(method, result);
														//writeResponse(result, fcallid, mi, (HttpServletRequest)ctx.getRequest(), (HttpServletResponse)ctx.getResponse(), false, max);
														// status?
														writeResponse(new ResponseInfo(result).setStatus(202).setCallid(fcallid).setMappingInfo(mi)
															.setRequest((HttpServletRequest)ctx.getAsyncContext().getRequest()).setResponse((HttpServletResponse)ctx.getAsyncContext().getResponse()).setFinished(true));
													}
													// ctx.complete();
												}
			
												// Browser not waiting -> check for timeout
												// and store or terminate
												else
												{
													// Only check timeout when future not
													// yet finished.
													if(!FINISHED.equals(result) && exception == null)
													{
														// System.out.println("checking "+result);
														// if timeout -> cancel future.
														// TODO: which timeout? (client vs server).
														if(System.currentTimeMillis() - rinfo.getTimestamp() > Starter.getDefaultTimeout(component.getId()))
														{
															// System.out.println("terminating due to timeout: "+exception);
															rinfo.setTerminated();
															if(ret instanceof ITerminableFuture< ? >)
															{
																((ITerminableFuture< ? >)ret).terminate(new TimeoutException());
															}
															else
															{
																// TODO: better handling of
																// non-terminable futures?
																throw new TimeoutException();
															}
														}
													}
			
													// Exception -> store until requested.
													if(!rinfo.isTerminated() && exception != null)
													{
														// System.out.println("storing
														// exception till browser requests:
														// "+exception);
														rinfo.setException(exception);
													}
			
													// Normal result -> store until
													// requested. (check for command==null
													// to also store null values as
													// results).
													else if(!rinfo.isTerminated() && command == null)
													{
														//System.out.println("storing result till browser requests: "+result);
														rinfo.addResult(result);
													}
			
													// else nop (no need to store timer
													// updates). what about other commands?
												}
												//System.out.println("handleResult exit: "+callid+" "+rinfo.getResults());
										*/	}
										});//);
									}
									else if(ret instanceof IFuture)
									{
										//final IAsyncContextInfo ctx = getAsyncContextInfo(request);
										//saveRequestContext(fcallid, ctx); // Only for having access to the request via callid from Jadex processing, e.g. for performing security checks with session
				
										// todo: use timeout listener
										// TODO: allow also longcalls (requires intermediate
										// command responses -> use only when requested by
										// browser?)
										((IFuture)ret).addResultListener(
											//createResultListener(
											new IResultListener<Object>()
										{
											public void resultAvailable(Object ret)
											{
												// System.out.println("one-shot call:
												// "+method.getName()+" paramtypes:
												// "+SUtil.arrayToString(method.getParameterTypes())+"
												// on "+service+" "+Arrays.toString(params));
												//ret = mapResult(method, ret);
												//writeResponse(ret, fcallid, mi, request, response, true, null);
												writeResponse(ri.setResult(ret).setCallid(fcallid).setMappingInfo(mi).setFinished(true));
												// ctx.complete();
											}
				
											public void exceptionOccurred(Exception exception)
											{
												//Object result = mapResult(method, exception);
												//writeResponse(result, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), fcallid, mi, request, response, true, null);
												writeResponse(ri.setException(exception).setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).setCallid(fcallid).setMappingInfo(mi).setFinished(true));
												// ctx.complete();
											}
										});//);
										// ret =
										// ((IFuture<?>)ret).get(Starter.getDefaultTimeout(null));
									}
									else
									{
										// System.out.println("call finished:
										// "+method.getName()+" paramtypes:
										// "+SUtil.arrayToString(method.getParameterTypes())+"
										// on "+service+" "+Arrays.toString(params));
										// map the result by user defined mappers
										//Object res = mapResult(method, ret);
										// convert content and write result to servlet response
										//writeResponse(res, fcallid, mi, request, response, true, null);
										
										// status?
										writeResponse(ri.setResult(ret).setStatus(202).setCallid(fcallid).setMappingInfo(mi).setFinished(true));
									}
								}
							}
							catch(Exception e)
							{
								// System.out.println("call exception: "+e);
								//writeResponse(e, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), null, null, request, response, true, null);
								writeResponse(ri.setException(e).setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).setFinished(true));
							}
						});
//					});
				}
				else
				{
					response.setCharacterEncoding("utf-8");
					PrintWriter out = response.getWriter();
					setCORSHeader(response);
					setNoCachingHeader(response);
	
					// setup SSE if requested via header
					String ah = request.getHeader("Accept");
					if(ah!=null && ah.toLowerCase().indexOf(MediaType.SERVER_SENT_EVENTS)!=-1)
					{
						// todo: sessionid should be set by client! why created here?
						
						// Session can be null if no header is provided by client
						String fsessionid = sessionid;
						if(fsessionid==null)
							fsessionid = SUtil.createUniqueId();
						
						// SSE flag in session 
						//getSession(fsessionid, true).put("sse", request.getAsyncContext());
						putSSEContextInSession(fsessionid, request.getAsyncContext());
						//setInSession(fsessionid, "sse", request.getAsyncContext());
						response.addHeader(HEADER_JADEX_CALLID, ri.getCallid());
						response.setContentType(MediaType.SERVER_SENT_EVENTS+"; charset=utf-8");
					    response.setCharacterEncoding("UTF-8");
					    response.setStatus(HttpServletResponse.SC_OK);
						//out.write("data: {'a': 'a'}\n\n");
						response.flushBuffer();
					    //System.out.println("sse connection saved: "+request.getAsyncContext()+" "+fsessionid+" "+getSessionId(request));
					    
					    sendDelayedSSEEvents(getSession(fsessionid, true));
					}
					else
					{
						response.setContentType(MediaType.TEXT_HTML+"; charset=utf-8");
						String info = getServiceInfo(service, getServletUrl(request), pm);
						out.write(info);
						// Set to ok and send back response
						response.setStatus(HttpServletResponse.SC_OK);
						complete(request, response);
						
						//System.out.println("info site sent");
					}
				}
			}
		}
	}
	
	/**
	 *  Prune the timeouted conversations.
	 */
	protected synchronized void pruneObsoleteConversations()
	{
		long curtime = System.currentTimeMillis();
		
		// Check sse client pings and prune old sse source (conversations)
		
		new ArrayList<SSEInfo>(sseinfos.values()).forEach(sseinfo ->
		{
			String sessionid = sseinfo.getSessionId();
			if(curtime - sseinfo.getLastCheck() > CONVERSATION_TIMEOUT)  
			{ 
				System.out.println("sse source does not respond, removing: "+sessionid);
				List<ConversationInfo> cinfos = conversationinfos.values().stream().filter(info -> info.getSessionId()==sessionid).collect(Collectors.toList());
				cinfos.forEach(cinfo ->
				{
					System.out.println("Conversation timed out: "+cinfo.getCallId());
					//System.out.println("cur time: "+System.currentTimeMillis());
					//System.out.println("timestamp: "+entry.getValue().getTimestamp());
					//System.out.println("timeout: "+Starter.getDefaultTimeout(component.getId()));
					terminateConversation(cinfo, null, false);
				});
				
				removeSSEInfo(sessionid);
				removeSession(sessionid);
			}
		});
		
		// Create sse pings for sse sources that have conversations that are all timed out
		
		Map<String, List<ConversationInfo>> ginfos = conversationinfos.values().stream()
			.filter(cinfo -> !sseinfos.containsKey(cinfo.getSessionId()))
			.collect(Collectors.groupingBy(ConversationInfo::getSessionId));
		
		ginfos.forEach((sessionid, infos) -> 
		{
            // Find newest time value
            ConversationInfo newest = conversationinfos.values().stream()
            	.max(Comparator.comparingLong(ConversationInfo::getTimestamp)).get();
            
            if(System.currentTimeMillis() - newest.getTimestamp() > CONVERSATION_TIMEOUT) 
			{
            	 AsyncContext ctx = getSSEContextFromSession(sessionid);
                 
                 if(ctx!=null)
                 {
                	// todo: make sseping ping/alive send all active client callids to remove the obsolete ones individually?!
                	 
                 	String callid = SUtil.createUniqueId("sseping");
                 	final ConversationInfo mycinfo = new ConversationInfo(callid, sessionid);
					addConversation(callid, mycinfo);
                 	
                 	ResponseInfo ri = new ResponseInfo().setResult("sseping").setCallid(callid)
                 		.setRequest((HttpServletRequest)ctx.getRequest())
                 		.setResponse((HttpServletResponse)ctx.getResponse());
                 	writeResponse(ri);
                 	
                 	addSSEInfo(new SSEInfo(ctx, sessionid));
                 }
                 else
                 {
                	 System.out.println("sse context not found: "+sessionid);
                 }
			}
        });
	}
	
	/**
	 *  Update timestamps of all conversations that belong to a session / sse source.
	 *  @param sessionid The session id.
	 */
	protected synchronized void updateTimestamps(String sessionid)
	{
		//System.out.println("Update timestamps: "+sessionid);
		List<ConversationInfo> cinfos = conversationinfos.values().stream().filter(cinfo -> sessionid.equals(cinfo.getSessionId())).collect(Collectors.toList());
		cinfos.forEach(cinfo -> cinfo.updateTimestamp());
	}
	
	/**
	 * 
	 * @param cinfo
	 * @param ex
	 */
	protected synchronized void terminateConversation(ConversationInfo cinfo, Exception ex, boolean clientterm)
	{
		//System.out.println("terminate in rest: "+cinfo+" "+ex+" "+clientterm);
		
		// Terminate the future if requested
		cinfo.setTerminated(true);
		if(cinfo.getFuture() instanceof ITerminableFuture)
		{
			if(ex!=null)
				((ITerminableFuture)cinfo.getFuture()).terminate(ex); 
			else
				((ITerminableFuture)cinfo.getFuture()).terminate();
			
			//System.out.println("terminate on: "+cinfo.getFuture().hashCode());
		}
		else if(clientterm)
		{
			System.out.println("WARNING: future cannot be terminated: "+cinfo+" "+cinfo.getFuture());
		}
		
		conversationinfos.remove(cinfo.getCallId());
	}
	
	protected synchronized ConversationInfo getConversation(String callid)
	{
		return conversationinfos.get(callid);
	}
	
	protected synchronized void addConversation(String callid, ConversationInfo cinfo)
	{
		if(cinfo.getSessionId()==null)
			throw new IllegalArgumentException();
		conversationinfos.put(callid, cinfo);
	}
	
	protected synchronized boolean hasConversation(String callid)
	{
		return conversationinfos.containsKey(callid);
	}
	
	protected synchronized SSEInfo getSSEInfo(String sessionid)
	{
		return sseinfos.get(sessionid);
	}
	
	protected synchronized void addSSEInfo(SSEInfo info)
	{
		if(info.getSessionId()==null)
			throw new RuntimeException("session id must not null");
		sseinfos.put(info.getSessionId(), info);
		//System.out.println("added sseinfo: "+info.getSessionId());
	}
	
	protected synchronized void removeSSEInfo(String sessionid)
	{
		if(sessionid==null)
			throw new RuntimeException("session id must not null");
		sseinfos.remove(sessionid);
		//System.out.println("removed sseinfo: "+sessionid);
	}

	/**
	 *  Set the cors header in the response.
	 *  @param response The response.
	 */
	protected void setCORSHeader(HttpServletResponse response)
	{
		response.addHeader("Access-Control-Allow-Origin", "*");
		// http://stackoverflow.com/questions/3136140/cors-not-working-on-chrome
		response.addHeader("Access-Control-Allow-Credentials", "true ");
		response.addHeader("Access-Control-Allow-Methods", "OPTIONS, GET, POST");
		response.addHeader("Access-Control-Allow-Headers", "Content-Type, Depth, User-Agent, X-File-Size, X-Requested-With, If-Modified-Since, X-File-Name, Cache-Control");
	}
	
	/**
	 *  Set the cache header in the response.
	 *  @param response The response.
	 */
	protected void setNoCachingHeader(HttpServletResponse response)
	{
		// add header for non-caching
		response.addHeader("Cache-Control", "no-cache, no-store");
		response.addHeader("Expires", "-1");
	}
	
	/**
	 * Send the delayed events which have been collected during connection loss.
	 * Must check if callid belongs to still ongoing call (could be terminated).
	 */
	protected synchronized void sendDelayedSSEEvents(Map<String, Object> session)
	{
		int cnt = sseevents.size();
		AsyncContext ctx = (AsyncContext)session.get("sse");
		HttpServletResponse response;
		if(ctx!=null)
		{
			response = (HttpServletResponse)ctx.getResponse();
			response.setContentType(MediaType.SERVER_SENT_EVENTS+"; charset=utf-8");
		    response.setCharacterEncoding("UTF-8");
		    response.setStatus(HttpServletResponse.SC_OK);

			try
			{
				for(SSEEvent event: sseevents)
				{
					//if(conversationinfos.containsKey(event.getCallId()))
					if(hasConversation(event.getCallId()))
					{
						String ret = createSSEJson(event);
						response.getWriter().write(ret);	
					}
					else
					{
						System.out.println("trashing delayed but obsolete event: "+event);
					}
				}
			
				response.flushBuffer();
				sseevents.clear();
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
		
		if(cnt!=0 && sseevents.size()!=0)
			System.out.println("sent delayed events: "+cnt+" "+sseevents.size());
	}
	
	protected synchronized void addSSEEvent(SSEEvent event)
	{
		sseevents.add(event);
	}
	
	/**
	 *  Logout from the platform.
	 *  @param secret The platform secret.
	 *  @return True, if login was successful.
	 */
	public synchronized IFuture<Boolean> logout(HttpServletRequest request)
	{
		boolean ret = true;
		if(getSession(request, false)!=null)
			getSession(request, false).remove("loggedin");
		else
			ret = false;
		//return ret;
		return new Future<Boolean>(ret);
	}
	
	/**
	 *  Test if a the web user is logged in.
	 *  @param request The web request.
	 *  @return True, if is logged in.
	 */
	public synchronized boolean isLoggedIn(HttpServletRequest request)
	{
		Map<String, Object> sess = getSession(request, false);
		return sess!=null && sess.get("loggedin")==Boolean.TRUE;
	}
	
	/**
	 *  Test if a the web user is logged in.
	 *  @param callid The callid of the request.
	 *  @return True, if is logged in.
	 */
	public synchronized boolean isLoggedIn(String callid)
	{
		boolean ret = false;
		ConversationInfo cinfo = conversationinfos.get(callid);
		if(cinfo!=null)
			ret = getSession(cinfo.getSessionId(), false).get("loggedin")==Boolean.TRUE;
		return ret;
		//return new Future<>(ret? Boolean.TRUE: Boolean.FALSE);
	}
	
	/**
	 * Get the async
	 */
	protected IAsyncContextInfo getAsyncContextInfo(HttpServletRequest request)
	{
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
	
	public static void addMimeTypes(Object cs, List<String> types)
	{
		if(cs instanceof Collection)
		{
			for(String c : (Collection<String>)cs)
			{
				if(!types.contains(c))
					types.add(c);
			}
		}
		else if(cs instanceof String)
		{
			String c = (String)cs;
			if(!types.contains(c))
				types.add(c);
		}
	}

	/**
	 * Map the incoming uri/post/multipart parameters to the service target
	 * parameter types.
	 */
	protected Tuple2<MappingInfo, Object[]> mapParameters(HttpServletRequest request, Collection<MappingInfo> mis, List<Map<String, String>> bindings)
	{
		try
		{
			Object[] targetparams = null;

			Map<String, Object> inparamsmap = new LinkedHashMap<>();
			
			String ct = request.getHeader("Content-Type");
			if(ct == null)
				ct = request.getHeader("Accept");

			// parameters for query string (must be parsed to keep order) and
			// posted form data not for multi-part
			if(request.getQueryString() != null)
				inparamsmap.putAll(splitQueryString(request.getQueryString()));

			// Read multi-part form data
			if(request.getContentType() != null && request.getContentType().startsWith(MediaType.MULTIPART_FORM_DATA) && request.getParts().size() > 0)
			{
				for(Part part : request.getParts())
				{
					//System.out.println("content-type: "+part.getContentType());
					byte[] data = SUtil.readStream(part.getInputStream());
					String mime = SUtil.guessContentTypeByBytes(data);
					if(mime!=null && (mime.indexOf("application")!=-1 || mime.indexOf("image")!=-1 || mime.indexOf("audio")!=-1))
					{
						// add as raw byte[] if mimetype is binary 
						addEntry(inparamsmap, part.getName(), data);
					}
					else
					{
						// add as text if other mimetype
						addEntry(inparamsmap, part.getName(), new String(data, StandardCharsets.UTF_8));
					}
				}
			}
			
			// Hack, removes internal random id used to avoid browser cache
			inparamsmap.remove(RANDOM);
			// For GET requests attribute 'contenttype' are added
			Object cs = inparamsmap.remove(CONTENTTYPE); // why not accept
			
			//if(cs!=null)
			//	System.out.println("found contenttype: "+cs);
					
			// Find correct method using paramter count
			MappingInfo mi = null;
			Map<String, String> binding = null;
			if(mis.size() == 1)
			{
				mi = mis.iterator().next();
				binding = bindings.get(0);
			}
			else
			{
				List<Object[]> matches1 = new ArrayList<>(); // by parameter names
				List<Object[]> matches2 = new ArrayList<>(); // by parameter cnt
				
				int psize = inparamsmap.size();
				Iterator<Map<String, String>> bit = bindings.iterator();
				for(MappingInfo tst : mis)
				{
					Map<String, String> b = bit.next();
					List<String> declaredparamnames = tst.getParameterNames();
					Set<String> paramnames = inparamsmap.keySet();
					
					if(declaredparamnames.size()>0 && declaredparamnames.containsAll(paramnames))
					{
						matches1.add(new Object[]{tst, b, paramnames.size()});
					}
					
					if(psize+b.size() == tst.getMethod().getParameterTypes().length)
					{
						matches2.add(new Object[]{tst, b, psize+b.size()});
					}
				}
				
				if(matches1.size()>0)
				{
					matches1.sort((x,y) -> ((Integer)y[2]).intValue()-((Integer)x[2]).intValue());
					Object[] res = matches1.get(0);
					mi = (MappingInfo)res[0];
					binding = (Map<String, String>)res[1];
				}
				else if(matches2.size()>0)
				{
					matches2.sort((x,y) -> ((Integer)y[2]).intValue()-((Integer)x[2]).intValue());
					Object[] res = matches2.get(0);
					mi = (MappingInfo)res[0];
					binding = (Map<String, String>)res[1];
				}
			}

			if(mi == null)
				throw new RuntimeException("No method mapping found.");
			
			// Add path infos from binding to inparamsmap
			if(binding!=null && binding.size()>0)
				inparamsmap.putAll(binding);

			Method method = mi.getMethod();
			
			// target method types
			Class<?>[] types = method.getParameterTypes();
			
			// acceptable media types for input
			//String mts = request.getHeader("Content-Type");
			List<String> cl = parseMimetypes(ct);
			addMimeTypes(cs, cl);

			List<String> sr = new ArrayList<String>(mi.getConsumedMediaTypes());
			if(sr == null || sr.size() == 0)
			{
				sr = cl;
			}
			else
			{
				sr.retainAll(cl);
			}

			Tuple2<List<Tuple2<String, String>>, Map<String, Class<?>>> pinfos = getParameterInfos(method);
			
			// is a @FormParam parameter used by the user?
			boolean hasformparam = false;
			for(Tuple2<String, String> pinfo: pinfos.getFirstEntity())
			{
				hasformparam = "form".equals(pinfo.getFirstEntity());
				if(hasformparam)
					break;
			}
			
			// Read parameter from stream (message body)
			//if((inparams == null || inparams.length == 0) && types.length > 0 && ct != null && (ct.trim().startsWith("application/json") || ct.trim().startsWith("test/plain")))
			//{
			byte[] bytes = null;
			try
			{
				// Nano can throw exception here :-(
				InputStream is = request.getInputStream();
				if(is!=null)
					bytes = SUtil.readStream(is);
			}
			catch(Exception e)
			{
			}

			// TODO Hack for raw JSON to avoid converting later?
			boolean rawjson = false;
			if(bytes!=null && bytes.length>0)
			{
				String mime = SUtil.guessContentTypeByBytes(bytes);
				if(mime!=null && (mime.indexOf("application")!=-1 || mime.indexOf("image")!=-1 || mime.indexOf("audio")!=-1))
				{
					// add as raw byte[] if mimetype is binary 
					// add under what name?!
					addEntry(inparamsmap, "body", bytes);
				}
				else
				{
					String str = new String(bytes, SUtil.UTF8);
					
					if(ct!=null && ct.trim().startsWith("application/x-www-form-urlencoded"))
					{
	//					System.out.println(str);
						Map<String, Object> vals = splitQueryString(str);
						inparamsmap.putAll(vals);
					}
					else if(ct!=null && (ct.trim().startsWith("application/json") || ct.trim().startsWith("test/plain")))
					{
						// if only one target argument
						if(types.length == 1)
						{
							// if is json object
							if(str.trim().startsWith("{"))
							{
								if (String.class.equals(types[0]))
								{
									// Return raw JSON to parameter
									rawjson = true;
									inparamsmap.put("0", str);
								}
								else
								{
									// Map as first argument
									Object arg0 = convertJsonValue(str, types[0], getClassLoader(), true);
									inparamsmap.put("0", arg0);
								}
							}
							else if(str.trim().startsWith("\""))
							{
								// try to directly convert to target type
								Object arg0 = JsonTraverser.objectFromString(str, getClassLoader(), null, types[0], null);
								inparamsmap.put("0", arg0);
							}
						}
						// multiple arguments
						else
						{
							// Array of objects as arguments
							JsonValue args = Json.parse(str);
							
							if(args instanceof JsonArray)
							{
								JsonArray array = (JsonArray)args;
								for(int i = 0; i < array.size(); i++)
								{
									inparamsmap.put(""+i, convertJsonValue(array.get(i).toString(), types[i], getClassLoader(), false));
								}
							}
							else if(args instanceof JsonObject)
							{
								JsonObject jobj = (JsonObject)args;
								if(hasformparam)
								{
									Map<String, Class<?>> typesmap = pinfos.getSecondEntity();
									// put all contained objects in the params map
									int[] i = new int[1];
									final Map<String, Object> finparamsmap = inparamsmap;
									jobj.forEach((com.eclipsesource.json.JsonObject.Member x)->
									{
										i[0]++;
										Class<?> type = typesmap.get(x.getName());
										if(type!=null)
										{
											Object val = convertJsonValue(x.getValue().toString(), type, getClassLoader(), false);
											finparamsmap.put(x.getName(), val);
										}
										else
										{
											System.out.println("Ignoring argument with no type: "+x.getName());
											//throw new RuntimeException("Unable to determine argument type: "+x.getName());
										}
									});
								}
								else
								{
	//								if(type==null && i[0]<types.length)
	//									type = types[i[0]];
	//								else
								}
							}
						}
					}
					else
					{
						throw new RuntimeException("Content type not supported for body: "+ct);
					}
				}
			}
			
			// From here the parameter array 'inparams' is built using
			// a) the map with possibly named input values 'inparamsmap'
			// b) the parameter annotations describing where each parameter comes from 'pinfos'
			
			// if(sr.size()>0)
			// System.out.println("found acceptable in types: "+sr);
			// if(sr.size()==0)
			// System.out.println("found no acceptable in types.");

			if(method.isAnnotationPresent(ParametersMapper.class))
			{
				// System.out.println("foundmapper");
				ParametersMapper mm = method.getAnnotation(ParametersMapper.class);
				if(!mm.automapping())
				{
					Class<?> pclazz = mm.value().clazz();
					Object mapper;
					if(!Object.class.equals(pclazz))
					{
						mapper = pclazz.getDeclaredConstructor().newInstance();
					}
					else
					{
						mapper = SJavaParser.evaluateExpression(mm.value().value(), null);
					}
					if(mapper instanceof IValueMapper)
						mapper = new DefaultParameterMapper((IValueMapper)mapper);
					
					if(mapper instanceof IParameterMapper)
					{
						// The order of in parameters is corrected with respect to the
						// target parameter order
						Object[] inparams = generateInParameters(inparamsmap, pinfos, types);
						for(int i = 0; i < inparams.length; i++)
						{
							if(inparams[i] instanceof String)
								inparams[i] = convertParameter(sr, (String)inparams[i], types[i]);
						}
						targetparams = ((IParameterMapper)mapper).convertParameters(inparams, request);
					}
					else if(mapper instanceof IParameterMapper2)
					{
						targetparams = ((IParameterMapper2)mapper).convertParameters(inparamsmap, pinfos, request);
					}
					else
					{
						throw new RuntimeException("Mapper does not implement IParameterMapper/2");
					}
				}
				else
				{
					// System.out.println("automapping detected");
					Class<?>[] ts = method.getParameterTypes();
					targetparams = new Object[ts.length];
					if(ts.length == 1 && inparamsmap != null)
					{
						if(SReflect.isSupertype(ts[0], Map.class))
						{
							targetparams[0] = inparamsmap;
							((Map)targetparams[0]).putAll(extractCallerValues(request));
						}
					}
				}
			}
			
			// Natural auto map if there are in parameters
			// Mappers can return null to not handle the mapping and let default mapping being applied
            if(targetparams==null)
			{
				targetparams = new Object[types.length];

				Object[] inparams = generateInParameters(inparamsmap, pinfos, types);
				if (!rawjson)
				{
					for (int i = 0; i < inparams.length; i++)
					{
						inparams[i] = convertParameter(sr, inparams[i], types[i]);
					}
				}
				
				for(int i = 0; i < targetparams.length && i < inparams.length; i++)
				{
					targetparams[i] = inparams[i];
				}
			}
			
			// Type check parameters and convert
			for(int i = 0; i < targetparams.length; i++)
			{
				Object p = targetparams[i];
				Object v = null;

				if(p != null)
				{
					// Only convert if necessary to keep transformed values from mapper
					if(!SReflect.isSupertype(types[i], p.getClass()))
					{
						if(types[i].isArray())
						{
							// fill in collection
							if(p instanceof Collection)
							{
								Collection<Object> col = (Collection<Object>)p;
								Object ar = Array.newInstance(types[i].getComponentType(), col.size());
								v = ar;
								Iterator<Object> it = col.iterator();
								for(int j = 0; j < col.size(); j++)
								{
									Object a = convertParameter(sr, it.next(), types[i].getComponentType());
									if(a != null)
										Array.set(ar, j, a);
								}
							}
							// varargs support -> convert matching single value
							// to singleton array
							else if(SReflect.isSupertype(types[i].getComponentType(), p.getClass()))
							{
								v = Array.newInstance(types[i].getComponentType(), 1);
								Array.set(targetparams[i], 0, p);
							}
						}
						else
						{
							v = convertParameter(sr, p, types[i]);
						}
					}

					if(v != null)
					{
						targetparams[i] = v;
					}
				}
			}
			
			// Add default values for basic types
			for(int i = 0; i < targetparams.length; i++)
			{
				if(targetparams[i] == null)
				{
					if(types[i].equals(boolean.class))
					{
						targetparams[i] = Boolean.FALSE;
					}
					else if(types[i].equals(char.class))
					{
						targetparams[i] = Character.valueOf((char)0);
					}
					else if(SReflect.getWrappedType(types[i]) != types[i]) // Number type
					{
						targetparams[i] = Integer.valueOf(0);
					}
				}
			}

			return new Tuple2<MappingInfo, Object[]>(mi, targetparams);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	/**
	 *  Generate in parameters that are correct wrt order and number of targetparameter (must convert types possibly).
	 */
	public static Object[] generateInParameters(Map<String, Object> inparamsmap, Tuple2<List<Tuple2<String, String>>, Map<String, Class<?>>> pinfos, Class<?>[] types)
	{
		// The order of in parameters is corrected with respect to the
		// target parameter order
		Object[] inparams = new Object[types.length];
		
		// Iterate over given method parameter annotations in order
		//for(Tuple2<String, String> pinfo: pinfos.getFirstEntity())
		List<Integer> todo = new ArrayList<>();
		for(int i=0; i<pinfos.getFirstEntity().size(); i++)
		{
			Tuple2<String, String> pinfo = pinfos.getFirstEntity().get(i);
			
			if("name".equals(pinfo.getFirstEntity()))
			{
				inparams[i] = inparamsmap.remove(pinfo.getSecondEntity());
			}
			else if("path".equals(pinfo.getFirstEntity()))
			{
				inparams[i] = inparamsmap.remove(pinfo.getSecondEntity());
				//binding.get(pinfo.getSecondEntity());
			}
			else if("query".equals(pinfo.getFirstEntity()))
			{
				// query params are in normal parameter map
				inparams[i] = inparamsmap.remove(pinfo.getSecondEntity());
			}
			else if("form".equals(pinfo.getFirstEntity()))
			{
				// query params are in normal parameter map
				inparams[i] = inparamsmap.remove(pinfo.getSecondEntity());
			}
			else
			{
				todo.add(i);
			}
		}
		
		Iterator<String> innames = inparamsmap.keySet().iterator();
		for(int i: todo)
		{
			Tuple2<String, String> pinfo = pinfos.getFirstEntity().get(i);
			String inname = innames.hasNext()? innames.next(): null;
			
			if("no".equals(pinfo.getFirstEntity()) && inname!=null && inparamsmap.get(inname)!=null)
			{
				inparams[i] = inparamsmap.get(inname);
			}
		}
		
		/*for(int i = 0; i < inparams.length; i++)
		{
			if(inparams[i] instanceof String)
				inparams[i] = convertParameter(sr, (String)inparams[i], types[i]);
		}*/
		
		return inparams;
	}

	/**
	 *  Convert a json string to a java object.
	 *  @param val The json string.
	 *  @param type The target class.
	 *  @param cl The classloader.
	 *  @param tomap Flag, if a (nested) map should be read (only possible if type is map too).
	 */
	public static Object convertJsonValue(String val, Class<?> type, ClassLoader cl, boolean tomap)
	{
		List<ITraverseProcessor> procs = null;

		if(tomap && SReflect.isSupertype(Map.class, type))
			procs = JsonTraverser.nestedreadprocs;
		return JsonTraverser.objectFromString(val.toString(), cl, null, type, procs);
	}
	
	/**
	 * Convert a (string) parameter
	 * 
	 * @param val
	 * @param target
	 * @return
	 */
	public Object convertParameter(Object val, Class<?> target)
	{
		Object ret = null;

		IStringConverter conv = PublishJsonSerializer.getBasicTypeSerializer();

		if(val != null && SReflect.isSupertype(target, val.getClass()))
		{
			ret = val;
		}
		else if(val instanceof String && ((String)val).length() > 0 && conv!=null && conv.isSupportedType(target))
		{
			try
			{
				ret = conv.convertString((String)val, target, getClassLoader(), null);
			}
			catch(Exception e)
			{
			}
		}

		return ret;
	}

	/**
	 * Convert a parameter string to an object if is json or xml.
	 * 
	 * @param sr The media types.
	 * @param val The string value.
	 * @return The decoded object.
	 */
	protected Object convertParameter(List<String> sr, Object val, Class<?> targetclazz)
	{
		Object ret = val;
		boolean done = false;

		if(val instanceof String)
		{
			if(sr != null && (sr.contains(MediaType.APPLICATION_JSON) || sr.contains(MediaType.WILDCARD)))
			{
				try
				{
					ret = jsonser.convertString((String)val, targetclazz, getClassLoader(), null);
					//ret = JsonTraverser.objectFromByteArray(val.getBytes(SUtil.UTF8), component.getClassLoader(), (IErrorReporter)null, null, targetclazz);
					// ret = JsonTraverser.objectFromByteArray(val.getBytes(),
					// component.getClassLoader(), (IErrorReporter)null);
					done = true;
				}
				catch(Exception e)
				{
					//e.printStackTrace();
				}
			}
	
			
			// todo?
			
			/*if(!done && sr != null && (sr.contains(MediaType.APPLICATION_XML) || sr.contains(MediaType.WILDCARD)))
			{
				try
				{
					ret = binser.decode(((String)val).getBytes(StandardCharsets.UTF_8), getClassLoader(), null, null, null);
					//ret = JavaReader.objectFromByteArray(val.getBytes(), component.getClassLoader(), null);
					done = true;
				}
				catch(Exception e)
				{
				}
			}*/
		}
				
		if(!done)
		{
			ret = convertParameter(val, targetclazz);
		}

		return ret;
	}

	/**
	 * Map a result using the result mapper.
	 */
	protected Object mapResult(Method method, Object ret)
	{
		if(method!=null && method.isAnnotationPresent(ResultMapper.class))
		{
			try
			{
				ResultMapper mm = method.getAnnotation(ResultMapper.class);
				Class< ? > pclazz = mm.value().clazz();
				IValueMapper mapper;
				// System.out.println("res mapper: "+clazz);
				if(!Object.class.equals(pclazz))
				{
					mapper = (IValueMapper)pclazz.getDeclaredConstructor().newInstance();
				}
				else
				{
					mapper = (IValueMapper)SJavaParser.evaluateExpression(mm.value().value(), null);
				}

				ret = mapper.convertValue(ret);
			}
			catch(Exception e)
			{
				SUtil.throwUnchecked(e);
			}
		}
		// else
		// {
		// NativeResponseMapper mapper = new NativeResponseMapper();
		// ret = mapper.convertValue(ret);
		// }

		return ret;
	}

	/**
	 *  Write the response (header and content).
	 * /
	protected void writeResponse(Object result, String callid, MappingInfo mi, HttpServletRequest request, HttpServletResponse response, boolean fin, Integer max)
	{
		writeResponse(result, Response.Status.OK.getStatusCode(), callid, mi, request, response, fin, max);
	}*/

	/**
	 *  Write the response (header and content).
	 */
	protected synchronized void writeResponse(ResponseInfo ri)
//	protected void writeResponse(Object result, int status, String callid, MappingInfo mi, HttpServletRequest request, HttpServletResponse response, boolean fin, Integer max)
	{		
		//if(ri.getRequest().getQueryString()!=null && ri.getRequest().getQueryString().indexOf(ACCEPT)!=-1)
		//	System.out.println("writeResponse: "+ri.getResult()+", "+ri.getRequest().getRequestURI()+", "+ri.getCallid()+" "+ ri.getRequest().getQueryString());
		// Only write response on first exception
		if(isComplete(ri.getRequest(), ri.getResponse()))
		{
			if(ri.isSSERequest())
			{
				// SSE source not available 
				// should delete sse in session?!
				System.out.println("SSE source is offline"+ri.callid);
			}
			return;
		}
		
		List<String> sr = writeResponseHeader(ri);
		
		writeResponseContent(ri.setResultTypes(sr));
		
		// remove conversation only  (otherwise ongoing)
		if(ri.isFinished() && ri.getCallid()!=null && conversationinfos.get(ri.getCallid())!=null && !conversationinfos.get(ri.getCallid()).isIntermediateFuture())
		{
			conversationinfos.remove(ri.getCallid());
			//System.out.println("remove conversation: "+ri.getCallid());
		}
	}

	/**
	 *
	 */
	//protected List<String> writeResponseHeader(Object ret, int status, String callid, MappingInfo mi, 
	//	HttpServletRequest request, HttpServletResponse response, boolean fin, Integer max)
	protected List<String> writeResponseHeader(ResponseInfo ri)
	{
		List<String> sr = ri.getResultTypes();
		if(sr==null)
			sr = new ArrayList<String>();
		//List<String> sr = null;

		if(ri.getResult() instanceof Response)
		{
			Response resp = (Response)ri.getResult();

			ri.getResponse().setStatus(resp.getStatus());

			for(String name : resp.getStringHeaders().keySet())
			{
				ri.getResponse().addHeader(name, resp.getHeaderString(name));
			}

			ri.setResult(resp.getEntity());
			if(resp.getMediaType() != null)
			{
				//sr = new ArrayList<String>();
				sr.add(resp.getMediaType().toString());
			}
		}
		else
		{
			if(ri.getStatus() > 0)
				ri.getResponse().setStatus(ri.getStatus());

			// acceptable media types for response (HTTP is case insensitive!)
			List<String> cl = new ArrayList<String>();
			String acc = ri.getRequest().getParameter(ACCEPT);
			if(acc!=null)
				addMimeTypes(acc, cl);
			String mts = ri.getRequest().getHeader("accept");
			cl.addAll(parseMimetypes(mts));
			
			List<String> pmt = ri.getMappingInfo() == null ? null : ri.getMappingInfo().getProducedMediaTypes();
			if(pmt!=null)
				sr.addAll(pmt);
			if(sr == null || sr.size() == 0)
			{
				sr = cl;
			}
			else
			{
				sr.retainAll(cl);
			}

			/*
			 * if(sr.size()==0) {
			 * System.out.println("found no acceptable return types."); } else {
			 * System.out.println("acceptable return types: "+sr+" ("+cl+")"); }
			 */

			if(ri.getCallid() != null)
			{
				if(ri.isFinished())
				{
					ri.getResponse().addHeader(HEADER_JADEX_CALLFINISHED, ri.getCallid());
				}
				else
				{
					ri.getResponse().addHeader(HEADER_JADEX_CALLID, ri.getCallid());
				}
				
				if(ri.getMax()!=null)
					ri.getResponse().addHeader(HEADER_JADEX_MAX, ""+ri.getMax());
			}

			setCORSHeader(ri.getResponse());
			
			setNoCachingHeader(ri.getResponse());
			
			// Add Jadex version header, if enabled
			//if(Boolean.TRUE.equals(Starter.getPlatformArgument(component.getId(), "showversion")))
			//	ri.getResponse().addHeader(HEADER_JADEX_VERSION, VersionInfo.getInstance().toString());				
		}

		return sr;
	}

	/**
	 * Write the response content.
	 */
	//protected void writeResponseContent(Object result, HttpServletRequest request, HttpServletResponse response, List<String> sr)
	protected void writeResponseContent(ResponseInfo ri)
	{
		//if(result!=null)// && result.getClass().toString().toLowerCase().indexOf("byte")!=-1)
		//	System.out.println("jju: "+result.getClass());
		
		//if(result instanceof Exception)
		//	System.out.println("result is exception: "+result);
		
		// Set response content type to stream when requested so
		//if(ri.getResultTypes()!=null && ri.getResultTypes().size()>0)
		//{
			//Optional<String> f = ri.getResultTypes().stream().filter(rt -> rt.indexOf("text/event-stream")!=-1).findFirst();
			//if(f.isPresent())
		//}
				
		if(ri.getException()!=null)
		{
			if(SUtil.DEBUG)
				ri.getException().printStackTrace();
			else
				System.out.println("ex: "+ri.getException());
		}
		
		try
		{
			// If SSE is explicitly requested or the request is already finished MUST use SSE
			if(ri.isSSERequest() 
				|| ((IAsyncContextInfo)ri.getRequest().getAttribute(IAsyncContextInfo.ASYNC_CONTEXT_INFO))==null // can null after timeout
				|| ((IAsyncContextInfo)ri.getRequest().getAttribute(IAsyncContextInfo.ASYNC_CONTEXT_INFO)).isComplete())
			{
				//if(ri.getResult()!=null && ri.getResult().toString().indexOf("isTrusted")!=-1)
				//	System.out.println("sse result: "+ri.isSSERequest()+" "+ri.getRequest()+" "+ri.getResult());
				
				ri.getResponse().setHeader("Content-Type", MediaType.SERVER_SENT_EVENTS);
				ri.getResponse().setHeader("Connection", "keep-alive");
	
				// Wrap content in SSE event class to add Jadex meta info 
				SSEEvent event = createSSEEvent(ri);
	
				if(!hasConversation(event.getCallId()))
				{
					System.out.println("trashing obsolete event: "+event);
					return;
				}
				
				//if(ri.isSSEConnectionAvailable())
				//{
					PrintWriter out = ri.getResponse().getWriter();
					String ret = createSSEJson(event);
					out.write(ret);				
					//System.out.println(ri.getResponse().getHeader("Content-Type"));
					//System.out.println("used sse channel flush: "+ri.getRequest().getAsyncContext());
					ri.getResponse().getWriter().flush();
					ri.getResponse().flushBuffer();
					//System.out.println(ri.getResponse());
					
					//System.out.println("SSE response content:  "+ret);
				//}
				//else
				//{
				//	sseevents.add(event);
					
					// defer response
				//	System.out.println("no SSE connection - delaying event: "+event+" "+getSessionId(ri.getRequest()));
				//}
			}
			// Send normal http response
			else
			{
				//System.out.println("http result: "+ri.getRequest()+" "+ri.getResult());
				
				// result is byte[] send directly
				if(ri.getResult() instanceof byte[])
				{
					if(ri.getResponse().getHeader("Content-Type") == null && ri.getResultTypes()!=null && ri.getResultTypes().size()>0)
						ri.getResponse().setHeader("Content-Type", ri.getResultTypes().get(0));
					ri.getResponse().getOutputStream().write((byte[])ri.getResult());
					complete(ri.getRequest(), ri.getResponse());
				}
				// convert result according to mime type
				else
				{ 
					Object res = ri.getException()!=null? ri.getException(): ri.getResult();
					
					if(res != null)
					{
						String ret = null;
						String mt = null;
						if(ri.getResultTypes() != null)
						{
							// try to find converter for acceptable mime types
							for(String mediatype : ri.getResultTypes())
							{
								mediatype = mediatype.trim(); // e.g. sent with leading space from edge, grrr
								Collection<IObjectStringConverter> convs = getConverters(mediatype);
								if(convs != null && convs.size() > 0)
								{
									mt = mediatype;
									Object input = res instanceof Response ? ((Response)res).getEntity() : res;
									ret = convs.iterator().next().convertObject(input, null);
									break;
								}
							}
						}
		
						// if found converter 
						if(mt != null)
						{
							// If no charset is specified, default to UTF-8 instead
							// of HTTP default which is ISO-8859-1.
							//if(mt.startsWith("text") && !mt.contains("charset"))
							if(!mt.contains("charset"))
								mt = mt + "; charset=utf-8";
		
							if(ri.getResponse().getHeader("Content-Type") == null)
								ri.getResponse().setHeader("Content-Type", mt);
	
							// Important: writer access must be deferred to happen after setting charset! Will be ignored otherwise
							// https://stackoverflow.com/questions/51014481/setting-default-character-encoding-and-content-type-in-embedded-jetty
							ri.getResponse().getWriter().write(ret);
							//System.out.println("Response content1  res:"+ret+" ctx:"+ri.getRequest().getAsyncContext());
						}
						// else cannot convert result, write plain text
						else 
						{
							if(ri.getResponse().getHeader("Content-Type") == null)
								ri.getResponse().setHeader("Content-Type", MediaType.TEXT_PLAIN + "; charset=utf-8");
							if(!(res instanceof String) && !(res instanceof Response))
								System.out.println("cannot convert result, writing as string: " + res);
	
							// Important: writer access must be deferred to happen after setting charset!
							ret = res instanceof Response ? "" + ((Response)res).getEntity() : res.toString();
							
							//System.out.println("Response content2:  "+ret+" ctx:"+ri.getRequest().getAsyncContext());
							ri.getResponse().getWriter().write(ret);
							//out.write("data: {'a': 'a'}\n\n");
						}
						// for testing with browser
						// http://brockallen.com/2012/04/27/change-firefoxs-default-accept-header-to-prefer-json-over-xml/
					}
					complete(ri.getRequest(), ri.getResponse());
				}
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 *  Create an sse event.
	 *  @param result
	 *  @param finished
	 *  @param callid
	 *  @param max
	 *  @return
	 */
	//protected SSEEvent createSSEEvent(Object result, Exception exception, boolean finished, String callid, Integer max)
	protected SSEEvent createSSEEvent(ResponseInfo ri)
	{
		if(ri.getException()!=null)
		{
			Object ex = mapResult(ri.getMethod(), ri.getException());
			return createSSEEvent(ex, ri.isFinished(), ri.getCallid(), ri.getMax(), ri.getException().getClass().getName());
		}
		else
		{
			Object result = mapResult(ri.getMethod(), ri.getResult());
			result = result instanceof Response ? ((Response)result).getEntity() : result;
			return createSSEEvent(ri.getResult(), ri.isFinished(), ri.getCallid(), ri.getMax(), null);
		}
	}
	
	/**
	 *  Create an sse event.
	 */
	protected SSEEvent createSSEEvent(Object result, boolean finished, String callid, Integer max, String exceptiontype)
	{
		//if(result!=null && result.getClass().toString().indexOf("PartDataChunk")!=-1)
		//	System.out.println("fff");
		
		SSEEvent event = new SSEEvent();
		// Wrap content in SSE event class to add Jadex meta info 
		event.setData(result).setFinished(finished).setCallId(callid).setMax(max).setExecptionType(exceptiontype);
		Collection<IObjectStringConverter> convs = getConverters(MediaType.SERVER_SENT_EVENTS);
		String ret = convs.iterator().next().convertObject(event, null);
		// Transform json to fit json SSE standard event 
		ret = "id: "+callid+"\ndata: "+ret+"\n\n";

		return event;
	}
	
	/**
	 *  Create an sse event for a JavaSSE event.
	 */
	protected String createSSEJson(SSEEvent event)
	{
		Collection<IObjectStringConverter> convs = getConverters(MediaType.SERVER_SENT_EVENTS);
		String ret = convs.iterator().next().convertObject(event, null);
		
		// Transform json to fit json SSE standard event 
		ret = "id: "+event.getCallId()+"\ndata: "+ret+"\n\n";
		return ret;
	}

	/**
	 *  Save a request context per callid.
	 *  @param callid The callid.
	 *  @param ctx The context.
	 * /
	protected void saveRequestContext(String callid, IAsyncContextInfo ctx)
	{
		//System.out.println("add request: "+callid+" "+ctx.hashCode());
		Collection<IAsyncContextInfo> ctxs = requestspercall.get(callid);
		if(ctxs==null)
		{
			ctxs = new ArrayList<IAsyncContextInfo>();
			requestspercall.put(callid, ctxs);
		}
		ctxs.add(ctx);
		//requestspercall.add(callid, ctx);

		// Set individual time if is contained in request
		// todo: add support for individual lease times in map
		//long to = getRequestTimeout((HttpServletRequest)ctx.getRequest());
		//((LeaseTimeMap)requestspercall).touch(key);
		
		/*if(to > 0)
		{
			// System.out.println("req timeout is: "+to);
			((ILeaseTimeSet<AsyncContext>)requestspercall.getCollection(callid)).touch(ctx, to);
		}* /
		// else
		// {
		// System.out.println("no req timeout for call: "+callid);
		// }
	}*/

	/**
	 * Get the request timeout.
	 */
	public static long getRequestTimeout(HttpServletRequest request)
	{
		long ret = -1;
		String tostr = request.getHeader(HEADER_JADEX_CLIENTTIMEOUT);
		Long to = tostr != null ? Long.valueOf(tostr) : null;
		if(to != null)
		{
			// wakeup 10% before client timeout
			ret = (long)(to.longValue() * 0.9);
		}
		return ret;
	}

	/**
	 * todo: make statically accessible Copied from Jadex ForwardFilter
	 */
	public static List<String> parseMimetypes(String mts)
	{
		// List<String> mimetypes = null;
		List<String> mimetypes = new ArrayList<String>();
		if(mts != null)
		{
			// mimetypes = new ArrayList<String>();
			StringTokenizer stok = new StringTokenizer(mts, ",");
			while(stok.hasMoreTokens())
			{
				String tok = stok.nextToken();
				StringTokenizer substok = new StringTokenizer(tok, ";");
				String mt = substok.nextToken();
				String charset = null;
				while(substok.hasMoreTokens())
				{
					String subtok = substok.nextToken().trim();
					if(subtok.startsWith("charset"))
					{
						charset = "; " + subtok;
						break;
					}
				}
				if(mimetypes == null)
				{
					mimetypes = new ArrayList<String>();
				}
				mimetypes.add(mt + (charset != null ? charset : ""));
			}
		}
		return mimetypes;
	}
	
	/**
	 *  Struct for storing info about a request and the results.
	 */
	public static class ConversationInfo
	{
		//protected Queue<Object>	results;

		//protected MappingInfo mappingInfo;

		protected boolean terminated;

		//protected Throwable exception;

		// to check time gap between last request from browser and current result
		// if gap>timeout -> abort future as probably no browser listening any more
		protected long lastcheck;
		
		protected IFuture<?> future;
		
		protected String sessionid;
		
		protected String callid;
		
		/**
		 *  Create a request info.
		 */
		//public RequestInfo(MappingInfo mappingInfo, IFuture<?> future)
		public ConversationInfo(String callid, String sessionid)
		//public ConversationInfo(HttpSession session, IFuture<?> future)
		{
			this.callid = callid;
			this.sessionid = sessionid;
			//this.mappingInfo = mappingInfo;
			//this.future = future;
			this.lastcheck = updateTimestamp();
		}

		/**
		 *  Set it to terminated.
		 */
		public void setTerminated(boolean term)
		{
			terminated = term;
		}

		/**
		 *  Check if terminated
		 *  @return True if terminated.
		 */
		public boolean isTerminated()
		{
			return terminated;
		}

		/**
		 * Check, if there is a result that is not yet consumed. Also increases
		 * the check timer to detect timeout when browser is disconnected.
		 * 
		 * @return True if there is a result.
		 * /
		public boolean checkForResult()
		{
			this.lastcheck = System.currentTimeMillis();
			return results != null && !results.isEmpty();
		}*/

		/**
		 * Add a result.
		 * 
		 * @param result The result to add
		 * /
		public void addResult(Object result)
		{
			if(results == null)
				results = new ArrayDeque<>();
			results.add(result);
		}*/

		/**
		 * Get the mappingInfo.
		 * 
		 * @return The mappingInfo
		 * /
		public MappingInfo getMappingInfo()
		{
			return mappingInfo;
		}*/

		/**
		 * Get the exception (if any).
		 * /
		public Throwable getException()
		{
			return exception;
		}*/

		/**
		 * Set the exception.
		 * /
		public void setException(Throwable exception)
		{
			this.exception = exception;
		}*/

		/**
		 * Get the next result (FIFO order).
		 * 
		 * @throws NullPointerException if there were never any results
		 * @throws NoSuchElementException if the last result was already
		 *         consumed.
		 * /
		public Object getNextResult()
		{
			return results.remove();
		}*/
		
		/**
		 * Get the results.
		 * /
		public Object getResults()
		{
			return results;
		}*/

		/**
		 *  Renew the timestamp.
		 */
		public long updateTimestamp()
		{
			return lastcheck = System.currentTimeMillis();
		}
		
		/**
		 *  Get the timestamp of the last check (i.e. last request from browser).
		 */
		public long getTimestamp()
		{
			return lastcheck;
		}

		/**
		 *  Get the future.
		 *  @return the future
		 */
		public IFuture<?> getFuture()
		{
			return future;
		}
		
		/**
		 * @param future the future to set
		 */
		public void setFuture(IFuture<?> future) 
		{
			this.future = future;
		}

		/**
		 * @return the session
		 */
		public String getSessionId() 
		{
			return sessionid;
		}

		/**
		 * @param session the session to set
		 */
		public void setSessionId(String sessionid) 
		{
			this.sessionid = sessionid;
		}
		
		/**
		 *  Get the callid.
		 *  @return the callid.
		 */
		public String getCallId() 
		{
			return callid;
		}

		/**
		 *  Set the callid.
		 *  @param callid The callid.
		 */
		public void setCallId(String callid) 
		{
			this.callid = callid;
		}

		/**
		 *  Test if it is an intermediate future.
		 *  @return True, if is intermediate future.
		 */
		public boolean isIntermediateFuture()
		{
			return future instanceof IIntermediateFuture;
		}

		public String toString() 
		{
			return "ConversationInfo [terminated=" + terminated + ", lastcheck=" + lastcheck + ", future=" + future
				+ ", sessionid=" + sessionid + ", callid=" + callid + "]";
		}
	}
	
	/**
	 *  Struct for storing info about a request and the results.
	 */
	public static class SSEInfo
	{
		protected AsyncContext context;
		
		protected String sessionid;
		
		protected long lastcheck;
		
		public SSEInfo(AsyncContext context, String sessionid)
		{
			this.context = context;
			this.sessionid = sessionid;
			this.lastcheck = System.currentTimeMillis();
		}

		public AsyncContext getContext() 
		{
			return context;
		}

		public SSEInfo setContext(AsyncContext context) 
		{
			this.context = context;
			return this;
		}

		public String getSessionId() 
		{
			return sessionid;
		}

		public SSEInfo setSessionId(String sessionid) 
		{
			this.sessionid = sessionid;
			return this;
		}

		public long getLastCheck() 
		{
			return lastcheck;
		}

		public SSEInfo setLastCheck(long lastcheck) 
		{
			this.lastcheck = lastcheck;
			return this;
		}
	}

	/**
	 *  Get metainfo about parameters from the target method via annotations.
	 */
	public Tuple2<List<Tuple2<String, String>>, Map<String, Class<?>>> getParameterInfos(Method method)
	{
		List<Tuple2<String, String>> ret = new ArrayList<>();
		Map<String, Class<?>> targettypes = new HashMap<>();
		
		Annotation[][] anns = method.getParameterAnnotations();
		Class<?>[] types = method.getParameterTypes();
		
		for(int i=0; i<anns.length; i++)
		{
			boolean done = false;
			for(Annotation ann: anns[i])
			{
				if(ann instanceof PathParam)
				{
					PathParam pp = (PathParam)ann;
					String name = pp.value();
					ret.add(new Tuple2<String, String>("path", name));
					targettypes.put(name, types[i]);
					done = true;
					break;
				}
				else if(ann instanceof QueryParam)
				{
					QueryParam qp = (QueryParam)ann;
					String name = qp.value();
					ret.add(new Tuple2<String, String>("query", name));
					targettypes.put(name, types[i]);
					done = true;
					break;
				}
				else if(ann instanceof FormParam)
				{
					FormParam qp = (FormParam)ann;
					String name = qp.value();
					ret.add(new Tuple2<String, String>("form", name));
					targettypes.put(name, types[i]);
					done = true;
					break;
				}
//				else if(ann instanceof ParameterInfo)
//				{
//					ParameterInfo qp = (ParameterInfo)ann;
//					String name = qp.value();
//					ret.add(new Tuple2<String, String>("name", name));
//					targettypes.put(name, types[i]);
//					done = true;
//					break;
//				}
				/*else
				{
					String name = ""+i;
					ret.add(new Tuple2<String, String>("no", name));
					targettypes.put(name, types[i]);
				}*/
	        }
			if(!done) //if(anns[i].length==0)
			{
				String name = ""+i;
				ret.add(new Tuple2<String, String>("no", name));
				targettypes.put(name, types[i]);
			}
		}
		
		return new Tuple2<>(ret, targettypes);
	}
	
	/**
	 * Extract caller values like ip and browser.
	 * 
	 * @param request The requrest.
	 * @param vals The values.
	 */
	public Map<String, String> extractCallerValues(Object request)
	{
		Map<String, String> ret = new HashMap<String, String>();

		// add request to map as internal parameter
		// cannot put request into map because map is cloned via service call
		if(request != null)
		{
			// if(request instanceof Request)
			// {
			// Request greq = (Request)request;
			// ret.put("ip", greq.getRemoteAddr());
			// ret.put("browser", greq.getHeader("User-Agent"));
			// ret.put("querystring", greq.getQueryString());
			// }
			// else
			if(request instanceof HttpServletRequest)
			{
				HttpServletRequest sreq = (HttpServletRequest)request;
				ret.put("ip", sreq.getRemoteAddr());
				ret.put("browser", sreq.getHeader("User-Agent"));
				ret.put("querystring", sreq.getQueryString());
			}
		}

		return ret;
	}

	protected ClassLoader getClassLoader()
	{
		return ComponentManager.get().getClassLoader();
	}
	
	/*protected ClassLoader getClassLoader(IComponent component)
	{
		return component==null? self.getClass().getClassLoader(): component.getClass().getClassLoader();
	}*/
	
	/*public IComponent getComponent()
	{
		return self;
	}*/
	
	/**
	 *  Info struct for response.
	 */
	public class ResponseInfo
	{
		protected Object result;
		protected Exception exception;
		protected Method method;
		protected int status;
		protected String callid;
		protected MappingInfo mi;
		protected HttpServletRequest request;
		protected HttpServletResponse response;
		protected boolean finished; 
		protected Integer max;
		protected List<String> resulttypes;
		//protected boolean keepopen;
		//protected boolean sse;

		/**
		 *  Create a new response info.
		 */
		public ResponseInfo() 
		{
		}
		
		/**
		 *  Create a new response info.
		 *  @param result
		 */
		public ResponseInfo(Object result) 
		{
			this.result = result;
		}

		/**
		 * @return the result
		 */
		public Object getResult() 
		{
			return result;
		}
		
		/**
		 * @param result the result to set
		 */
		public ResponseInfo setResult(Object result) 
		{
			this.result = result;
			return this;
		}
		
		/**
		 *  Get the exception.
		 *  @return the exception
		 */
		public Exception getException() 
		{
			return exception;
		}

		/**
		 *  Set the exception.
		 *  @param exception the exception to set
		 */
		public ResponseInfo setException(Exception exception) 
		{
			this.exception = exception;
			return this;
		}
		
		/**
		 * @return the status
		 */
		public int getStatus() 
		{
			return status;
		}
		
		/**
		 * @param status the status to set
		 */
		public ResponseInfo setStatus(int status) 
		{
			this.status = status;
			return this;
		}
		
		/**
		 * @return the callid
		 */
		public String getCallid() 
		{
			return callid;
		}
		
		/**
		 * @param callid the callid to set
		 */
		public ResponseInfo setCallid(String callid) 
		{
			this.callid = callid;
			return this;
		}
		
		/**
		 * @return the mi
		 */
		public MappingInfo getMappingInfo() 
		{
			return mi;
		}
		
		/**
		 * @param mi the mi to set
		 */
		public ResponseInfo setMappingInfo(MappingInfo mi) 
		{
			this.mi = mi;
			return this;
		}
		
		/**
		 * @return the request
		 */
		public HttpServletRequest getRequest() 
		{
			return request;
		}
		
		/**
		 * @param request the request to set
		 */
		public ResponseInfo setRequest(HttpServletRequest request) 
		{
			this.request = request;
			return this;
		}
		
		/**
		 * @return the response
		 */
		public HttpServletResponse getResponse() 
		{
			return response;
		}
		
		/**
		 * @param response the response to set
		 */
		public ResponseInfo setResponse(HttpServletResponse response) 
		{
			this.response = response;
			return this;
		}
		
		/**
		 * @return the finished
		 */
		public boolean isFinished() 
		{
			return finished;
		}
		
		/**
		 * @param finished the finished to set
		 */
		public ResponseInfo setFinished(boolean finished) 
		{
			this.finished = finished;
			return this;
		}
		
		/**
		 * @return the method
		 */
		public Method getMethod() 
		{
			return method;
		}

		/**
		 * @param method the method to set
		 */
		public ResponseInfo setMethod(Method method) 
		{
			this.method = method;
			return this;
		}

		/**
		 * @return the max
		 */
		public Integer getMax() 
		{
			return max;
		}
		
		/**
		 * @param max the max to set
		 */
		public ResponseInfo setMax(Integer max) 
		{
			this.max = max;
			return this;
		}

		/**
		 * @return the resulttypes
		 */
		public List<String> getResultTypes() 
		{
			return resulttypes;
		}

		/**
		 * @param resulttype the resulttypes to set
		 */
		public ResponseInfo setResultTypes(List<String> resulttypes) 
		{
			this.resulttypes = resulttypes;
			return this;
		}
		
		/**
		 *  Check if it is a SSE request.
		 *  @return True, if is sse request.
		 */
		public boolean isSSERequest()
		{
			return getRequest().getHeader("Accept")!=null && getRequest().getHeader("Accept").indexOf(MediaType.SERVER_SENT_EVENTS)!=-1;
		}
		
		/**
		 *  Check if sse connection is available.
		 *  @return True, if it is available.
		 * /
		public boolean isSSEConnectionAvailable()
		{
			boolean ret = false;
			Map<String, Object> session = getSession(getRequest(), false);
			if(session!=null)
				ret = session.get("sse")!=null;
			return ret;
		}*/
	}
	
	/**
	 *  SSE event data class.
	 */
	public static class SSEEvent
	{
		/** The real data. */
		protected Object data;
		
		/** Exception type name when exception has occurred. */
		protected String execptiontype;
		
		/** The max value. */
		protected Integer max;
		
		/** Flag indicating if the call is finished. */
		protected boolean finished;
		
		/** The callid. */
		protected String callid;
		
		public SSEEvent()
		{
		}
		
		/**
		 * @return the data
		 */
		public Object getData() 
		{
			return data;
		}
		
		/**
		 * @param data the data to set
		 */
		public SSEEvent setData(Object data) 
		{
			this.data = data;
			return this;
		}
		
		/**
		 * @return the max
		 */
		public Integer getMax() 
		{
			return max;
		}
		
		/**
		 * @param max the max to set
		 */
		public SSEEvent setMax(Integer max) 
		{
			this.max = max;
			return this;
		}
		
		/**
		 * @return the finished
		 */
		public boolean isFinished() 
		{
			return finished;
		}
		
		/**
		 * @param finished the finished to set
		 */
		public SSEEvent setFinished(boolean finished) 
		{
			this.finished = finished;
			return this;
		}
		
		/**
		 * @return the callid
		 */
		public String getCallId() 
		{
			return callid;
		}
		
		/**
		 * @param callid the callid to set
		 */
		public SSEEvent setCallId(String callid) 
		{
			this.callid = callid;
			return this;
		}
		
		/**
		 * @return the execptiontype
		 */
		public String getExecptionType() 
		{
			return execptiontype;
		}

		/**
		 * @param execptiontype the execptiontype to set
		 */
		public SSEEvent setExecptionType(String execptiontype) 
		{
			this.execptiontype = execptiontype;
			return this;
		}

		@Override
		public String toString() 
		{
			return "SSEEvent [data=" + data + ", max=" + max + ", finished=" + finished + ", callid=" + callid + "]";
		}
	}
	
	

	/**
	 * Get the servlet base url.
	 * 
	 * @param req The request.
	 * @return The servlet base url.
	 */
	public static String getServletUrl(HttpServletRequest req)
	{
		StringBuffer url = new StringBuffer(getServletHost(req));
		String cp = req.getContextPath(); // deploy directory
		String serp = req.getServletPath(); // name of servlet

		if(cp != null)
			url.append(cp);
		if(serp != null)
			url.append(serp);

		return url.toString();
	}

	/**
	 * Get the servlet base url.
	 * 
	 * @param req The request.
	 * @return The servlet base url.
	 */
	public static String getServletHost(HttpServletRequest req)
	{
		StringBuffer url = new StringBuffer();
		String scheme = req.getScheme();
		int port = req.getServerPort();

		url.append(scheme);
		url.append("://");
		url.append(req.getServerName());
		if(("http".equals(scheme) && port != 80) || ("https".equals(scheme) && port != 443))
		{
			url.append(':');
			url.append(req.getServerPort());
		}

		return url.toString();
	}

	/**
	 * Functionality blueprint for get service info web method. Creates a html
	 * page with css for style and javascript for ajax post requests. The
	 * service info site contains a section for each published method.
	 * 
	 * @param params The parameters.
	 * @return The result.
	 */
	public String getServiceInfo(Object service, String baseuri, PathManager<MappingInfo> mappings)
	{
		StringBuffer ret = new StringBuffer();

		try
		{
			String functionsjs = loadFunctionJS();
			String stylecss = loadStyleCSS();

			ret.append("<html>");
			ret.append("\n");
			ret.append("<head>");
			ret.append("\n");
			ret.append(stylecss);
			ret.append("\n");
			ret.append(functionsjs);
			ret.append("\n");
			ret.append("<script src=\"jadex.js\" type=\"text/javascript\"/></script>");
			ret.append("</head>");
			ret.append("\n");
			ret.append("<body>");
			ret.append("\n");

			ret.append("<div class=\"header\">");
			ret.append("\n");
			ret.append("<h1>");// Service Info for: ");
			String ifacename = ((IService)service).getServiceId().getServiceType().getTypeName();
			ret.append(SReflect.getUnqualifiedTypeName(ifacename));
			ret.append("</h1>");
			ret.append("\n");
			ret.append("</div>");
			ret.append("\n");

			ret.append("<div class=\"middle\">");
			ret.append("\n");

			// Class<?> clazz = service.getClass();
			// List<Method> methods = new ArrayList<Method>();
			// while(!clazz.equals(Object.class))
			// {
			// List<Method> l = SUtil.arrayToList(clazz.getDeclaredMethods());
			// methods.addAll(l);
			// clazz = clazz.getSuperclass();
			// }

			// Collections.sort(mappings, new MethodComparator());

			if(mappings != null)
			{
				for(MappingInfo mi : mappings.getElements())
				{
					Method method = mi.getMethod();
					HttpMethod restmethod = mi.getHttpMethod() != null ? mi.getHttpMethod() : guessRestType(method);

					String path = mi.getPath() != null ? mi.getPath() : method.getName();
					List<String> consumed = mi.getConsumedMediaTypes();
					List<String> produced = mi.getProducedMediaTypes();

					// Use defaults if nothing is given
					if(consumed == null)
						consumed = PARAMETER_MEDIATYPES;
					if(produced == null)
						produced = PARAMETER_MEDIATYPES;

					Class< ? >[] ptypes = method.getParameterTypes();
					String[] pnames = new String[ptypes.length];
//					java.lang.annotation.Annotation[][] pannos = method.getParameterAnnotations();

					// Find parameter names
					for(int p = 0; p < ptypes.length; p++)
					{
//						for(int a = 0; a < pannos[p].length; a++)
//						{
//							if(pannos[p][a] instanceof ParameterInfo)
//							{
//								pnames[p] = ((ParameterInfo)pannos[p][a]).value();
//							}
//						}

						if(pnames[p] == null)
						{
							pnames[p] = "arg" + p;
						}
					}

					ret.append("<div class=\"method\">");
					ret.append("\n");

					ret.append("<div class=\"methodname\">");
					// ret.append("<i><b>");
					ret.append(method.getName());
					// ret.append("</b></i>");

					ret.append("(");
					if(ptypes != null && ptypes.length > 0)
					{
						for(int j = 0; j < ptypes.length; j++)
						{
							ret.append(SReflect.getUnqualifiedClassName(ptypes[j]));
							ret.append(" ");
							ret.append(pnames[j]);
							if(j + 1 < ptypes.length)
								ret.append(", ");
						}
					}
					ret.append(")");
					ret.append("</div>");
					ret.append("\n");
					// ret.append("</br>");

					ret.append("<div class=\"restproperties\">");
					ret.append("<div id=\"httpmethod\">").append(restmethod).append("</div>");

					if(consumed != null && consumed.size() > 0)
					{
						ret.append("<i>");

						if(consumed != PARAMETER_MEDIATYPES)
						{
							ret.append("Consumes: ");
						}
						else
						{
							ret.append("Consumes [not declared by the service]: ");
						}
						ret.append("</i>");
						for(int j = 0; j < consumed.size(); j++)
						{
							ret.append(consumed.get(j));
							if(j + 1 < consumed.size())
								ret.append(" ,");
						}
						ret.append(" ");
					}

					if(produced != null && produced.size() > 0)
					{
						ret.append("<i>");
						if(produced != PARAMETER_MEDIATYPES)
						{
							ret.append("Produces: ");
						}
						else
						{
							ret.append("Produces [not declared by the service]: ");
						}
						ret.append("</i>");
						for(int j = 0; j < produced.size(); j++)
						{
							ret.append(produced.get(j));
							if(j + 1 < produced.size())
								ret.append(" ,");
						}
						ret.append(" ");
					}
					// ret.append("</br>");
					ret.append("</div>");
					ret.append("\n");

					// String link = baseuri.toString();
					// if(path!=null) // Todo: cannot be null!?
					// link = link+"/"+path;
					String link = path; // Do not use absolute URL to allow
										// reverse proxying

					// System.out.println("path: "+link);

					// if(ptypes.length>0)
					// {
					ret.append("<div class=\"servicelink\">");
					ret.append(link);
					ret.append("</div>");
					ret.append("\n");

					// For post set the media type of the arguments.
					ret.append("<form class=\"arguments\" action=\"").append(link).append("\" method=\"").append(restmethod).append("\" enctype=\"multipart/form-data\" ");

					// if(restmethod.equals(HttpMethod.POST))
					ret.append("onSubmit=\"return extract(this)\"");
					ret.append(">");
					ret.append("\n");

					for(int j = 0; j < ptypes.length; j++)
					{
						ret.append(pnames[j]).append(": ");
						ret.append("<input name=\"").append(pnames[j]).append("\" type=\"text\" />");
						// .append(" accept=\"").append(cons[0]).append("\"
						// />");
					}

					ret.append("<select name=\"mediatype\">");
					if(consumed != null && consumed.size() > 0)
					{
						// ret.append("<select name=\"mediatype\">");
						for(int j = 0; j < consumed.size(); j++)
						{
							// todo: hmm? what about others?
							if(!MediaType.MULTIPART_FORM_DATA.equals(consumed.get(j)) && !MediaType.APPLICATION_FORM_URLENCODED.equals(consumed.get(j)))
							{
								ret.append("<option>").append(consumed.get(j)).append("</option>");
							}
						}
					}
					else
					{
						ret.append("<option>").append(MediaType.TEXT_PLAIN).append("</option>");
					}
					ret.append("</select>");
					ret.append("\n");

					ret.append("<input type=\"submit\" value=\"invoke\"/>");
					ret.append("</form>");
					ret.append("\n");
					// }
					// else
					// {
					// ret.append("<div class=\"servicelink\">");
					// ret.append("<a
					// href=\"").append(link).append("\">").append(link).append("</a>");
					// ret.append("</div>");
					// ret.append("\n");
					// }

					ret.append("</div>");
					ret.append("\n");
				}
			}

			ret.append("</div>");
			ret.append("\n");

			ret.append("<div id=\"result\"></div>");

			ret.append("<div class=\"powered\"> <span class=\"powered\">powered by</span> <span class=\"jadex\">");
			// Add Jadex version header, if enabled
			/*if(Boolean.TRUE.equals(Starter.getPlatformArgument(component.getId(), "showversion")))
			{
				ret.append(VersionInfo.getInstance());
			}
			else
			{*/
				ret.append("Jadex Active Components");				
			//}
			ret.append("</span> <a class=\"jadexurl\" href=\"http://www.activecomponents.org\">http://www.activecomponents.org</a> </div>\n");
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		ret.append("</body>\n</html>\n");

		return ret.toString();
	}

	/**
	 * 
	 */
	public String loadFunctionJS()
	{
		String functionsjs;

		Scanner sc = null;
		try
		{
			InputStream is = SUtil.getResource0(getPath()+"functions.js", getClassLoader());
			sc = new Scanner(is);
			functionsjs = sc.useDelimiter("\\A").next();
			// System.out.println(functionsjs);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		finally
		{
			if(sc != null)
			{
				sc.close();
			}
		}

		return functionsjs;
	}

	/**
	 * 
	 */
	public String loadStyleCSS()
	{
		String stylecss;

		Scanner sc = null;
		try
		{

			InputStream is = SUtil.getResource0(getPath()+"style.css", getClassLoader());
			sc = new Scanner(is);
			stylecss = sc.useDelimiter("\\A").next();

			String stripes = SUtil.loadBinary(getPath()+"jadex_stripes.png");
			stylecss = stylecss.replace("$stripes", stripes);

			// System.out.println(functionsjs);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		finally
		{
			if(sc != null)
			{
				sc.close();
			}
		}

		return stylecss;
	}

	/**
	 * Guess the http type (GET, POST, PUT, DELETE, ...) of a method.
	 * 
	 * @param method The method.
	 * @return The rs annotation of the method type to use
	 */
	public HttpMethod guessRestType(Method method)
	{
		// Retrieve = GET (hasparams && hasret)
		// Update = POST (hasparams && hasret)
		// Create = PUT return is pointer to new resource (hasparams? && hasret)
		// Delete = DELETE (hasparams? && hasret?)

		HttpMethod ret = HttpMethod.GET;

		Class< ? > rettype = SReflect.unwrapGenericType(method.getGenericReturnType());
		Class< ? >[] paramtypes = method.getParameterTypes();

		boolean hasparams = paramtypes.length > 0;
		boolean hasret = rettype != null && !rettype.equals(Void.class) && !rettype.equals(void.class);

		// GET or POST if has both
		if(hasret)
		{
			if(hasparams)
			{
				if(hasStringConvertableParameters(method, rettype, paramtypes))
				{
					ret = HttpMethod.GET;
				}
				else
				{
					ret = HttpMethod.POST;
				}
			}
		}

		// todo: other types?

		// System.out.println("rest-type: "+ret.getName()+" "+method.getName()+"
		// "+hasparams+" "+hasret);

		return ret;
		// return GET.class;
	}

	/**
	 * Test if a method has parameters that are all convertible from string.
	 * 
	 * @param method The method.
	 * @param rettype The return types (possibly unwrapped from future type).
	 * @param paramtypes The parameter types.
	 * @return True, if is convertible.
	 */
	public boolean hasStringConvertableParameters(Method method, Class< ? > rettype, Class< ? >[] paramtypes)
	{
		boolean ret = true;

		for(int i = 0; i < paramtypes.length && ret; i++)
		{
			ret = SReflect.isStringConvertableType(paramtypes[i]);
		}

		return ret;
	}

	/**
	 * @param request
	 * @param cinfo
	 */
	protected void complete(HttpServletRequest request, HttpServletResponse response)
	{
		//if(request.getRequestURI().indexOf("subscribe")!=-1)
		//	System.out.println("ctx complete: "+request.getRequestURI());
		
		if(request.isAsyncStarted() && request.getAsyncContext() != null && !isComplete(request, response))
		{
			try
			{
				request.getAsyncContext().complete();
			}
			catch(Exception e)
			{
				System.out.println("Exception in context complete(): "+e);
			}
		}
	}

	/**
	 * @param request
	 * @param cinfo
	 */
	protected boolean isComplete(HttpServletRequest request, HttpServletResponse response)
	{
		IAsyncContextInfo cinfo = (IAsyncContextInfo)request.getAttribute(IAsyncContextInfo.ASYNC_CONTEXT_INFO);
		if(cinfo == null)
			System.out.println("warning, async context info is null: " + request);// .getRequestURL());
		return cinfo != null ? cinfo.isComplete() : response.isCommitted();
	}
	
	/**
	 *  Load jadex.js
	 *  @return The text from the file.
	 */
	public IFuture<byte[]> loadJadexJS()
	{
		try
		{
			InputStream is = SUtil.getResource0(getPath()+"jadex.js", getClassLoader());
			//String mt = SUtil.guessContentTypeByFilename(filename);
			
			byte[] data = SUtil.readStream(is);
		
			return new Future<byte[]>(data);
		}
		catch(Exception e)
		{
			return new Future<byte[]>(e);
		}
	}
	
	/**
	 *  Get the path to my package as path
	 *  @return The path.
	 */
	public String getPath()
	{
		String ret = SReflect.getPackageName(PublishServiceFeature.class);
		ret = ret.replace('.', '/');
		if(!ret.endsWith("/"))
			ret = ret+"/";
		return ret;
	}

	/**
	 *
	 */
	public static class MappingInfo
	{
		public enum HttpMethod
		{
			GET, POST, PUT, DELETE, OPTIONS, HEAD
		}

		/** The http method. */
		protected HttpMethod httpmethod;

		/** The target method. */
		protected Method method;

		/** The url path. */
		protected String path;

		/** The accepted media types for the response. */
		//private List<String> producedtypes = new ArrayList<String>();

		/** The accepted media types for consumption. */
		//private List<String> consumedtypes = new ArrayList<String>();

		/**
		 * Create a new mapping info.
		 */
		public MappingInfo()
		{
		}

		/**
		 * Create a new mapping info.
		 */
		public MappingInfo(HttpMethod httpMethod, Method method, String path)
		{
			this.httpmethod = httpMethod;
			this.method = method;
			this.path = path;
		}

		/**
		 * Get the httpMethod.
		 * 
		 * @return The httpMethod
		 */
		public HttpMethod getHttpMethod()
		{
			return httpmethod;
		}

		/**
		 * Set the httpMethod.
		 * 
		 * @param httpMethod The httpMethod to set
		 */
		public void setHttpMethod(HttpMethod httpMethod)
		{
			this.httpmethod = httpMethod;
		}

		/**
		 * Get the method.
		 * 
		 * @return The method
		 */
		public Method getMethod()
		{
			return method;
		}

		/**
		 * Set the method.
		 * 
		 * @param method The method to set
		 */
		public void setMethod(Method method)
		{
			this.method = method;
		}

		/**
		 * Get the path.
		 * 
		 * @return The path
		 */
		public String getPath()
		{
			return path;
		}

		/**
		 * Set the path.
		 * 
		 * @param path The path to set
		 */
		public void setPath(String path)
		{
			this.path = path;
		}

		/**
		 * Get the respmediatypes.
		 * 
		 * @return The respmediatypes
		 * /
		public List<String> getProducedMediaTypes()
		{
			return new ArrayList<String>(producedtypes);
		}*/

		/**
		 * Set the response mediatypes.
		 * @param respmediatypes The response mediatypes to set
		 * /
		public void setProducedMediaTypes(List<String> mediatypes)
		{
			this.producedtypes = mediatypes;
		}*/

		/**
		 *  Add a produced media type.
		 *  @param type The type.
		 * /
		public void addProducedMediaType(String type)
		{
			/*if(producedtypes == null)
			{
				producedtypes = new ArrayList<String>();
				if(method!=null && method.getParameterCount()==5)
					System.out.println("prod null");
			}* /
			producedtypes.add(type);
		}*/

		/**
		 *  Get the consumedmediatypes.
		 *  @return The consumedtypes
		 * /
		public List<String> getConsumedMediaTypes()
		{
			return new ArrayList<String>(consumedtypes);
		}*/

		/**
		 *  Set the respmediatypes.
		 *  @param consumedtypes The consumedtypes to set
		 * /
		public void setConsumedMediaTypes(List<String> mediatypes)
		{
			this.consumedtypes = mediatypes;
		}*/

		/**
		 *  Add a consumed media type.
		 *  @param type The type.
		 * /
		public void addConsumedMediaType(String type)
		{
			//if(consumedtypes == null)
			//	consumedtypes = new ArrayList<String>();
			consumedtypes.add(type);
		}*/

		/**
		 * Test if has no settings.
		 */
		public boolean isEmpty()
		{
			return path == null && method == null && httpmethod == null;
		}
		
		/**
		 *  Get the declared parameter names (via annotation ParameterInfo).
		 *  @return The parameter names. 
		 */
		public List<String> getParameterNames()
		{
			List<String> ret = new ArrayList<String>();
			Annotation[][] anns = method.getParameterAnnotations();
			for(Annotation[] ans: anns)
			{
				for(Annotation an: ans)
				{
					/*if(an instanceof ParameterInfo)
					{
						ParameterInfo p = (ParameterInfo)an;
						String name = p.value();
						ret.add(name);
						break;
					}
					else*/ if(an instanceof QueryParam)
					{
						QueryParam p = (QueryParam)an;
						String name = p.value();
						ret.add(name);
						break;
					}
					else if(an instanceof PathParam)
					{
						PathParam p = (PathParam)an;
						String name = p.value();
						ret.add(name);
						break;
					}
					else if(an instanceof FormParam)
					{
						FormParam p = (FormParam)an;
						String name = p.value();
						ret.add(name);
						break;
					}
				}
			}
			return ret;
		}
		
		/**
		 *  Get the consumed media types.
		 *  @return The types.
		 */
		//public void addConsumedMediaTypes()
		public List<String> getConsumedMediaTypes()
		{
			if(method.isAnnotationPresent(Consumes.class))
			{
				Consumes con = (Consumes)method.getAnnotation(Consumes.class);
				return Arrays.asList(con.value());
				/*for(String type : types)
				{
					addConsumedMediaType(type);
				}*/
			}
			return Collections.EMPTY_LIST;
		}
		
		/**
		 *  Get the produced media types.
		 *  @return The types.
		 */
		public List<String> getProducedMediaTypes()
		{
			if(method.isAnnotationPresent(Produces.class))
			{
				Produces prod = (Produces)method.getAnnotation(Produces.class);
				return Arrays.asList(prod.value());
				/*for(String type : types)
				{
					addProducedMediaType(type);
				}*/
			}
			return Collections.EMPTY_LIST;
		}
	}
	
	/**
	 * Split the query and save the order.
	 */
	public static Map<String, Object> splitQueryString(String query) throws Exception
	{
		Map<String, Object> ret = new LinkedHashMap<String, Object>();

		String[] pairs = query.split("&");
		Map<String, Set<Tuple2<Integer, String>>> compacted = new HashMap<>();

		for(String pair : pairs)
		{
			int idx = pair.indexOf("=");
			String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
			String val = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");

			idx = key.indexOf("_");
			boolean added = false;
			if(idx != -1)
			{
				String p = key.substring(idx + 1);
				try
				{
					int pos = Integer.parseInt(p);
					String ckey = key.substring(0, idx);
					Set<Tuple2<Integer, String>> col = compacted.get(ckey);
					if(col == null)
					{
						col = new TreeSet<>(new Comparator<Tuple2<Integer, String>>()
						{
							public int compare(Tuple2<Integer, String> o1, Tuple2<Integer, String> o2)
							{
								return o1.getFirstEntity() - o2.getFirstEntity();
							}
						});
						compacted.put(ckey, col);
					}
					added = true;
					col.add(new Tuple2<Integer, String>(pos, val));
				}
				catch(Exception e)
				{
				}
			}
			if(!added)
			{
				addEntry(ret, key, val);
			}
		}

		compacted.entrySet().stream().forEach(e -> 
		{
			TreeSet<Tuple2<Integer, String>> vals = (TreeSet<Tuple2<Integer, String>>)e.getValue();
			Tuple2<Integer, String> lastval = vals.last();
			
			String[] res = new String[lastval.getFirstEntity()+1];
			
			vals.stream().forEach(t -> res[t.getFirstEntity()] = t.getSecondEntity());
			
			List<String> data = Arrays.asList(res);
			
			// does not create empty slots in case of args_0, args_3, args_4
			//List<String> data = e.getValue().stream().map(a -> a.getSecondEntity()).collect(Collectors.toList());
			
			addEntry(ret, e.getKey(), data);
		});

		return ret;
	}
	
	/**
	 * @param ret
	 * @param key
	 * @param val
	 */
	protected static void addEntry(Map<String, Object> ret, String key, Object val)
	{
		if(ret.containsKey(key))
		{
			Object v = ret.get(key);
			if(v instanceof String)
			{
				List<String> col = new ArrayList<>();
				col.add((String)v);
				if(val instanceof Collection)
					col.addAll((Collection)val);
				else
					col.add((String)val);
				ret.put(key, col);
			}
			else if(v instanceof Collection)
			{
				Collection<String> col = (Collection<String>)v;
				if(val instanceof Collection)
					col.addAll((Collection)val);
				else
					col.add((String)val);
			}
		}
		else
		{
			ret.put(key, val);
		}
	}
	
	/**
	 *  Log in to the platform.
	 *  @param request The request.
	 *  @param secret The platform secret.
	 *  @return True, if login was successful.
	 */
	public IFuture<Boolean> login(HttpServletRequest request, String secret)
	{
		Future<Boolean> ret = new Future<Boolean>();
		
		System.out.println("login not implemented yet");
		
		// todo!
		/*ISecurityService ss = component.getLocalService(ISecurityService.class);
		ss.checkPlatformPassword(secret).then((Boolean ok) ->
		{
			if(ok)
				getSession(request, true).put("loggedin", Boolean.TRUE);
			ret.setResult(ok);
		}).catchEx((Exception e) -> 
		{
			ret.setResult(Boolean.FALSE);
		});*/
		
		ret.setResult(true);
		
		return ret;
	}
	
	/*
	public static void main(String[] args) throws Exception
	{
		Integer[] vals = new Integer[]{3,5,6,2,9,1};
		List<Integer> vl = Arrays.asList(vals);
		
		vl.sort((x,y) -> ((Integer)y).intValue()-((Integer)x).intValue());

		System.out.println(vl);
		
		//String query = "args_0=a&args_3=c";
		//Map<String, Object> res = splitQueryString(query);
		//System.out.println(query+ " -> "+res);
	}*/
	
	/**
	 *  Get the IP address of the client of a 
	 *  @param request The http request.
	 *  @return The ip. 
	 * /
	protected String getClientIP(HttpServletRequest request)
	{
		String addr = request.getHeader("X-FORWARDED-FOR");
        if (addr == null || "".equals(addr))
            addr = request.getRemoteAddr();
        return addr;
	}*/
	
	/**
	 * Get the async
	 * /
	protected AsyncContext getAsyncContext(HttpServletRequest request)
	{
		return request.isAsyncStarted()? request.getAsyncContext(): request.startAsync();
	}*/
	
	/**
	 *  Create a result listener that is executed on the
	 *  component thread.
	 * /
	public <T> IResultListener<T> createResultListener(IResultListener<T> listener)
	{
		return new ComponentResultListener<T>(listener, getComponent());
	}*/
	
	/**
	 *  Create a result listener that is executed on the
	 *  component thread.
	 * /
	public <T> IIntermediateResultListener<T> createResultListener(IIntermediateResultListener<T> listener)
	{
		return new IntermediateComponentResultListener<T>(listener, getComponent());
	}*/
	
	/**
	 * Evaluate the service interface and generate mappings. Return a
	 * multicollection in which for each path name the possible methods are
	 * contained (can be more than one due to different parameters).
	 */
	//public IFuture<MultiCollection<String, MappingInfo>> evaluateMapping(IServiceIdentifier sid, PublishInfo pi)
	//public IFuture<PathManager<MappingInfo>> evaluateMapping(IServiceIdentifier sid, PublishInfo pi, ClassLoader cl)
	public PathManager<MappingInfo> evaluateMapping(IServiceIdentifier sid, PublishInfo pi, ClassLoader cl)
	{
		Future<PathManager<MappingInfo>> reta = new Future<>();

		//ComponentIdentifier cid = sid.getProviderId();

		//IExternalAccess ea = getComponent().getExternalAccess(cid);

		//ea.scheduleStep(new IComponentStep<MultiCollection<String, MappingInfo>>()
		//ea.scheduleStep((IComponent ia) ->
		//{
			Class<?> mapcl = pi.getMapping() == null ? null : pi.getMapping().getType(cl);
			if(mapcl == null)
				mapcl = sid.getServiceType().getType(cl);
	
			PathManager<MappingInfo> ret = new PathManager<MappingInfo>();
			PathManager<MappingInfo> natret = new PathManager<MappingInfo>();
			
			//MultiCollection<String, MappingInfo> ret = new MultiCollection<String, MappingInfo>();
			//MultiCollection<String, MappingInfo> natret = new MultiCollection<String, MappingInfo>();
	
			for(Method m : SReflect.getAllMethods(mapcl))
			{
				MappingInfo mi = new MappingInfo();
				if(m.isAnnotationPresent(GET.class))
				{
					mi.setHttpMethod(HttpMethod.GET);
				}
				else if(m.isAnnotationPresent(POST.class))
				{
					mi.setHttpMethod(HttpMethod.POST);
				}
				else if(m.isAnnotationPresent(PUT.class))
				{
					mi.setHttpMethod(HttpMethod.PUT);
				}
				else if(m.isAnnotationPresent(DELETE.class))
				{
					mi.setHttpMethod(HttpMethod.DELETE);
				}
				else if(m.isAnnotationPresent(OPTIONS.class))
				{
					mi.setHttpMethod(HttpMethod.OPTIONS);
				}
				else if(m.isAnnotationPresent(HEAD.class))
				{
					mi.setHttpMethod(HttpMethod.HEAD);
				}
	
				if(m.isAnnotationPresent(Path.class))
				{
					Path path = m.getAnnotation(Path.class);
					mi.setPath(path.value());
				}
				else if(!mi.isEmpty())
				{
					mi.setPath(m.getName());
				}
	
				if(!mi.isEmpty())
				{
					//mi.addConsumedMediaTypes();
					//mi.addProducedMediaTypes();
					mi.setMethod(m);
					ret.addPathElement(mi.getPath(), mi);
					//ret.add(mi.getPath(), mi);
				}
	
				// Natural mapping using simply all declared methods
				MappingInfo mi2 = new MappingInfo(null, m, m.getName());
				//mi2.addConsumedMediaTypes();
				//mi2.addProducedMediaTypes();
				natret.addPathElement(m.getName(), mi2); // httpmethod, method, path
			}
		//}
			//return ret.size() > 0 ? ret : natret;
		//}).addResultListener(new DelegationResultListener<PathManager<MappingInfo>>(reta));

		reta.setResult(ret.size() > 0 ? ret : natret);
			
		//return reta;
		return ret.size() > 0 ? ret : natret;
	}
}
