package jadex.mj.publishservicejetty.impl;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.component.LifeCycle;

import jadex.common.SUtil;
import jadex.common.Tuple2;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.mj.core.IComponent;
import jadex.mj.feature.providedservice.IService;
import jadex.mj.feature.providedservice.IServiceIdentifier;
import jadex.mj.feature.providedservice.impl.search.ServiceQuery;
import jadex.mj.publishservice.impl.PublishInfo;
import jadex.mj.publishservice.impl.RequestManager;
import jadex.mj.publishservice.impl.RequestManager.MappingInfo;
import jadex.mj.publishservice.publish.PathManager;
import jadex.mj.requiredservice.IMjRequiredServiceFeature;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ServerManager 
{
	// Hack constant for enabling multi-part :-(
	private static final MultipartConfigElement MULTI_PART_CONFIG = new MultipartConfigElement(System.getProperty("java.io.tmpdir"));

    /** The servers per service id. */
	protected Map<IServiceIdentifier, Server> sidservers;

    /** The servers per port. */
    protected Map<Integer, Server> portservers;
    
    /** Infos for unpublishing. */
    protected Map<IServiceIdentifier, Tuple2<Server, ContextHandler>> unpublishinfos = new HashMap<IServiceIdentifier, Tuple2<Server,ContextHandler>>();
	
    protected static ServerManager instance;
    
    public synchronized static ServerManager getInstance()
    {
    	if(instance==null)
    		instance = new ServerManager();
    	return instance;
    }
    
	/**
     *  Publish a service.
     *  @param cl The classloader.
     *  @param service The original service.
     *  @param pid The publish id (e.g. url or name).
     */
    public synchronized void publishService(final IServiceIdentifier serviceid, final PublishInfo info, IComponent component)
    {
        PathManager<MappingInfo> pm = RequestManager.getInstance().
        	evaluateMapping(serviceid, info, this.getClass().getClassLoader());
               
		try
	    {
	    	//final IService service = (IService) component.getComponentFeature(IRequiredServicesFeature.class).searchService(new ServiceQuery<>( serviceid)).get();
	    	
	        final URI uri = new URI(getCleanPublishId(info.getPublishId(), component));
	        Server server = (Server)getHttpServer(uri, info).get();
	        System.out.println("Adding http handler to server (jetty): "+uri.getPath());
	
	        //ContextHandlerCollection collhandler = (ContextHandlerCollection)server.getHandler();
	        HandlerCollection collhandler = (HandlerCollection)server.getHandler();
	
	        ContextHandler ch = new ContextHandler()
	        {
	        	protected IService service = null;
	        	
	            public void doHandle(String target, Request baseRequest, final HttpServletRequest request, final HttpServletResponse response)
	                throws IOException, ServletException
	            {
	            	if(service==null)
	            	{
	            		service = component.getExternalAccess().scheduleStep((IComponent agent) ->
	            		{
	            			return agent.getFeature(IMjRequiredServiceFeature.class).searchService(new ServiceQuery<>((Class<IService>)null).setServiceIdentifier(serviceid)).get();
	            		}).get();
	            	}
	            	//service = getComponent().getExternalAccess().searchService(new ServiceQuery<>((Class<IService>)null).setServiceIdentifier(serviceid)).get();
	            	
	                // Hack to enable multi-part
	                // http://dev.eclipse.org/mhonarc/lists/jetty-users/msg03294.html
	                if(request.getContentType() != null && request.getContentType().startsWith("multipart/form-data")) 
	                	baseRequest.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, MULTI_PART_CONFIG);
	            	
	            	//handleRequest(service, pm, request, response, new Object[]{target, baseRequest});
	               	RequestManager.getInstance().handleRequest(service, pm, request, response, new Object[]{target, baseRequest});
	            	
	            	baseRequest.setHandled(true);

	               	//System.out.println("handler is: "+uri.getPath()+" "+target);
	            }
	        };
	        ch.setContextPath(uri.getPath());
	        collhandler.prependHandler(ch);
	        addUnpublishInfo(serviceid, server, ch);
	        //unpublishinfos.put(serviceid, new Tuple2<Server,ContextHandler>(server, ch));
	        ch.start(); // must be started explicitly :-(((
	
	        addSidServer(serviceid, server);
	    }
	    catch(Exception e)
	    {
	    	SUtil.rethrowAsUnchecked(e);
	    }
    }

    /**
     *  Get or start an api to the http server.
     */
    public synchronized IFuture<Object> getHttpServer(URI uri, PublishInfo info)
    {
    	Future<Object> ret = new Future<Object>();
    	
    	//System.out.println("in: "+Thread.currentThread()+" "+this.hashCode());
        Server server = null;

        try
        {
//	        URI baseuri = new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), null, null, null);
            server = getServer(uri.getPort());

            if(server==null)
            {
                System.out.println("Starting new server: "+uri.getPort());
                server = new Server(uri.getPort());
                
                // Activate forward module
                // https://stackoverflow.com/questions/26333846/configuring-embedded-jetty-9-for-x-forwarded-proto-with-spring-boot
                HttpConfiguration httpConfig = new HttpConfiguration();
                // Add support for X-Forwarded headers
                httpConfig.addCustomizer(new org.eclipse.jetty.server.ForwardedRequestCustomizer());
                // Create the http connector
                HttpConnectionFactory connectionFactory = new HttpConnectionFactory(httpConfig);
                ServerConnector connector = new ServerConnector(server, connectionFactory);
                // Make sure you set the port on the connector, the port in the Server constructor is overridden by the new connector
                connector.setPort(uri.getPort());
                // Add the connector to the server
                server.setConnectors(new ServerConnector[]{connector});
                
                // todo: http2, browser only support with tls?!
                //ServerConnector connector = new ServerConnector(server, ssl, alpn, http2, http1);
                //connector.setPort(webProperties.getPort());
                //server.addConnector(connector);
                
                //server.dumpStdErr();

                // https://stackoverflow.com/questions/62199102/sessionhandler-becomes-null-in-jetty-v9-4-5
                HandlerCollection collhandler = new HandlerCollection(new org.eclipse.jetty.server.session.SessionHandler());
                
                //ContextHandlerCollection collhandler = new ContextHandlerCollection();

/*                WebSocketHandler wsh = new WebSocketHandler()
                {
                	@Override
                	public void configure(WebSocketServletFactory factory)
                	{
                		factory.register(RestWebSocket.class);
                	}
                };
                ContextHandler context = new ContextHandler();
                //context.setContextPath("/wswebapi");
                context.setContextPath("/ws");
                context.setHandler(wsh);
                //server.addHandler(context);
                collhandler.addHandler(wsh);
                */
                     
                // todo: websocket is still in ts :-(
                /*textPath("/wswebapi");
                ch.setAllowNullPathInfo(true); // disable redirect from /ws to /ws/
                final JettyWebSocketCreator wsc = new JettyWebSocketCreator() 
                {
                	public Object createWebSocket(JettyServerUpgradeRequest request, JettyServerUpgradeResponse response) 
                	{
                		return new JettyWebsocketServer(getComponent());
                	}
                };
                Handler wsh = new WebSocketUpgradeHandler() 
                {
                	//public void configure(WebSocketServletFactory factory) 
                	public void configure(JettyWebSocketServletFactory factory) 
                	{
                		factory.setCreator(wsc);
                		//factory.register(RestWebSocket.class);
                	}
                };
                ch.setHandler(wsh);
                collhandler.addHandler(ch);*/
                
                // add session support
                /*ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
                context.setContextPath("/");
                context.setResourceBase(System.getProperty("java.io.tmpdir"));
                //server.setHandler(context);
               
                SessionHandler sessions = context.getSessionHandler();
                SessionCache cache = new DefaultSessionCache(sessions);
                cache.setSessionDataStore(new NullSessionDataStore());
                sessions.setSessionCache(cache);
                collhandler.addHandler(new DefaultHandler());
                collhandler.addHandler(context);*/
                
                server.setHandler(collhandler);
                
                final Server fserver = server;
                server.addEventListener(new LifeCycle.Listener()
                {
                	public void lifeCycleStarted(LifeCycle event)
                    {
                		ret.setResult(fserver);
                    }
                });

                server.start();
                //server.join();
                
                DefaultHandler dh = new DefaultHandler()
                {
                    @Override
                    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException 
                    {
                    	if(baseRequest.isHandled())
                    		return;
                    	
                    	//System.out.println("handler is: "+request.getRequestURI());

                    	if(target.indexOf("jadex.js")!=-1 || target.endsWith("events"))
                    	{
                    		RequestManager.getInstance().handleRequest(null, null, request, response, new Object[]{target, baseRequest});
                    		//handleRequest(null, null, request, response, new Object[]{target, baseRequest});
                    		baseRequest.setHandled(true);
                    	}
                    	else
                    	{
                    		System.out.println("default ignoring: "+target);
                    	}
                    }
                };
                
                /*ContextHandler ch = new ContextHandler()
                {
                    public void doHandle(String target, Request baseRequest, final HttpServletRequest request, final HttpServletResponse response)
                        throws IOException, ServletException
                    {
                    	//System.out.println("handler is: "+request.getRequestURI());

                    	RequestManager.getInstance().handleRequest(null, null, request, response, new Object[]{target, baseRequest});
                    	//handleRequest(null, null, request, response, new Object[]{target, baseRequest});
                    	
                        baseRequest.setHandled(true);
                    }
                };
                ch.setContextPath("/webapi");*/
                
                collhandler.addHandler(dh);
                dh.start();
                
                addPortServer(uri.getPort(), server);
            }
            else
            {
            	ret.setResult(server);
            }
        }
        catch(Exception e)
        {
            ret.setException(e);
        }

        //System.out.println("out: "+Thread.currentThread()+" "+this.hashCode());
        
        return ret;
    }
    
    /**
     *  Unpublish a service.
     *  @param sid The service identifier.
     */
    public synchronized void unpublishService(IServiceIdentifier sid)
    {
    	Tuple2<Server,ContextHandler> unpublish = removeUnpublishInfo(sid); //unpublishinfos.remove(sid);
    	if(unpublish != null)
    		((ContextHandlerCollection)unpublish.getFirstEntity().getHandler()).removeHandler(unpublish.getSecondEntity());
//	        throw new UnsupportedOperationException();
    }

    /**
     *  Publish a static page (without ressources).
     */
    public synchronized void publishHMTLPage(String pid, String vhost, final String html, IComponent component)
    {
    	try
        {
    		final URI uri = new URI(getCleanPublishId(pid, component));
        	//final IService service = (IService) component.getComponentFeature(IRequiredServicesFeature.class).searchService(new ServiceQuery<>( serviceid)).get();
        	
            Server server = (Server)getHttpServer(uri, null).get();
            System.out.println("Adding http handler to server (jetty): "+uri.getPath());

            HandlerCollection collhandler = (HandlerCollection)server.getHandler();

            ContextHandler ch = new ContextHandler()
            {
                public void doHandle(String target, Request baseRequest, final HttpServletRequest request, final HttpServletResponse response)
                    throws IOException, ServletException
                {
                	response.getWriter().write(html);
                	
//	                  System.out.println("handler is: "+uri.getPath());
                    baseRequest.setHandled(true);
                }
            };
            ch.setContextPath(uri.getPath());
            collhandler.prependHandler(ch);
            ch.start(); // must be started explicitly :-(((
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     *  Publish file resources from the classpath.
     */
    public synchronized IFuture<Void> publishResources(final String pid, final String rootpath, IComponent component)
    {
		final Future<Void>	ret	= new Future<Void>();
		try
		{
//			IComponentIdentifier	cid	= ServiceCall.getLastInvocation()!=null && ServiceCall.getLastInvocation().getCaller()!=null ? ServiceCall.getLastInvocation().getCaller() : component.getId();
//			component.getDescription(cid)
//				.addResultListener(new ExceptionDelegationResultListener<IComponentDescription, Void>(ret)
//			{
//				public void customResultAvailable(IComponentDescription desc)
//				{
//					ILibraryService	ls	= component.getFeature(IRequiredServicesFeature.class).getLocalService(new ServiceQuery<>( ILibraryService.class, ServiceScope.PLATFORM));
//					ls.getClassLoader(desc.getResourceIdentifier())
//						.addResultListener(new ExceptionDelegationResultListener<ClassLoader, Void>(ret)
//					{
//						public void customResultAvailable(ClassLoader cl1) throws Exception 
//						{
						final URI uri = new URI(getCleanPublishId(pid, component));
			        	//final IService service = (IService) component.getComponentFeature(IRequiredServicesFeature.class).searchService(new ServiceQuery<>( serviceid)).get();
			        	
			            Server server = (Server)getHttpServer(uri, null).get();
			            System.out.println("Adding http handler to server (jetty): "+uri.getPath()+" rootpath: "+rootpath);

			            HandlerCollection collhandler = (HandlerCollection)server.getHandler();
			            
			            ResourceHandler	rh	= new ResourceHandler();
			            ContextHandler	ch	= new ContextHandler()
			            {
			            	@Override
			            	public void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
			            	{
			            		super.doHandle(target, baseRequest, request, response);
			            	}
			            };
//				            ch.setBaseResource(Resource.newClassPathResource(rootpath));
			            ch.setBaseResource(new UniversalClasspathResource(rootpath));
			            ch.setHandler(rh);
			            ch.setContextPath(uri.getPath());
			            collhandler.prependHandler(ch);
			            ch.start(); // must be started explicitly :-(((
						
						System.out.println("Resource published at: "+uri.getPath());
						ret.setResult(null);
//						}
//					});
//				}
//			});
		}
		catch(Exception e)
		{
			ret.setException(e);
		}
		
		return ret;
    }
    
    public void terminateServers(Set<Integer> ports)
    {
    	if(portservers != null && ports!=null)
    	{
    		for(int port: ports)
    		{
    			Server server = portservers.get(port);
    			try
				{
					server.stop();
				}
				catch (Exception e)
				{
				}
    		}
    	}
    	System.out.println("Jetty port servers terminated: "+ports);
    }
    
	/**
	 * Get the cleaned publish id. Square brackets for the optional host and
	 * context part are removed.
	 */
	public String getCleanPublishId(String id, IComponent component)
	{
		id = id != null ? id.replace("[", "").replace("]", "") : null;
		id = id.replace("${componentid}", ""+component.getId().getLocalName());
		id = id.replace("${cid}", ""+component.getId().getLocalName());
		return id;
	}
	
	public synchronized Server getServer(int port)
	{
		Server ret = portservers==null? null: portservers.get(port);
		//System.out.println("getServer: "+port+" "+ret+" "+portservers);
		return ret;
	}
	
	public synchronized void addPortServer(int port, Server server)
	{
		 if(portservers==null)
             portservers = new HashMap<Integer, Server>();
         portservers.put(port, server);
         //System.out.println("added server: "+port);
	}
	
	public synchronized void addSidServer(IServiceIdentifier sid, Server server)
	{
		 if(sidservers==null)
             sidservers = new HashMap<IServiceIdentifier, Server>();
         sidservers.put(sid, server);
	}
	
	public synchronized void addUnpublishInfo(IServiceIdentifier serviceid, Server server, ContextHandler ch)
	{
		unpublishinfos.put(serviceid, new Tuple2<Server,ContextHandler>(server, ch));
	}
	
	public synchronized Tuple2<Server,ContextHandler> removeUnpublishInfo(IServiceIdentifier serviceid)
	{
		Tuple2<Server,ContextHandler> unpublish = unpublishinfos.remove(serviceid);
		return unpublish;
	}
}
