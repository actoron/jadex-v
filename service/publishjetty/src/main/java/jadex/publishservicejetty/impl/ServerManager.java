package jadex.publishservicejetty.impl;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;

import jadex.common.SUtil;
import jadex.common.Tuple2;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.providedservice.IService;
import jadex.providedservice.IServiceIdentifier;
import jadex.publishservice.IRequestManager;
import jadex.publishservice.PublishType;
import jadex.publishservice.IRequestManager.PublishContext;
import jadex.publishservice.impl.MappingEvaluator;
import jadex.publishservice.impl.PublishInfo;
import jadex.publishservice.impl.RequestManager;
import jadex.publishservice.impl.RequestManagerFactory;
import jadex.publishservice.impl.MappingEvaluator.MappingInfo;
import jadex.publishservice.impl.v2.http.HttpRequest;
import jadex.publishservice.impl.v2.http.HttpResponse;
import jadex.publishservice.publish.IRequestHandler;
import jadex.publishservice.publish.PathManager;
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

    /** The ws service registries per server aka port. */
    protected Map<Integer, WsServiceRegistry> registries = new HashMap<>();
    
    /** Infos for unpublishing. */
    protected Map<IServiceIdentifier, Runnable> unpublishinfos = new HashMap<>();
	
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
    //public synchronized IFuture<Void> publishService(final IServiceIdentifier serviceid, final PublishInfo info, IComponent component)
    public synchronized void publishService(final IService service, final PublishInfo info, IComponent component)
    {
        if(PublishType.REST.getId().equals(info.getPublishType()))
        {
            publishRestService(service, info, component);
        }
        else if(PublishType.WS.getId().equals(info.getPublishType()))
        {
            publishWsService(service, info, component);
        }
        else if(PublishType.SSE.getId().equals(info.getPublishType()))
        {
            throw new RuntimeException("todo!");
            //publishSseService(service, info, component);
        }
        else
        {
            throw new RuntimeException("Unknown publish type: "+info.getPublishType());
        }
    }

    /**
     *  Publish a service.
     *  @param cl The classloader.
     *  @param service The original service.
     *  @param pid The publish id (e.g. url or name).
     */
    //public synchronized IFuture<Void> publishService(final IServiceIdentifier serviceid, final PublishInfo info, IComponent component)
    public synchronized void publishRestService(final IService service, final PublishInfo info, IComponent component)
    {
    	//Future<Void> ret = new Future<>();
    	
        PathManager<MappingInfo> pm = MappingEvaluator.evaluateMapping(service.getServiceId(), info, this.getClass().getClassLoader());
               
		try
	    {
	    	//final IService service = (IService) component.getComponentFeature(IRequiredServicesFeature.class).searchService(new ServiceQuery<>( serviceid)).get();
	    	
	        final URI uri = new URI(getCleanPublishId(info.getPublishId(), component));
	        Server server = (Server)getHttpServer(uri).get();
	        System.out.println("Adding http handler to server (jetty): "+uri.getPath());
	
	        //ContextHandlerCollection collhandler = (ContextHandlerCollection)server.getHandler();
	        HandlerCollection collhandler = (HandlerCollection)server.getHandler();
	
	        ContextHandler ch = new ContextHandler()
	        {
	            public void doHandle(String target, Request baseRequest, final HttpServletRequest request, final HttpServletResponse response)
	                throws IOException, ServletException
	            {
	            	//service = getComponent().getExternalAccess().searchService(new ServiceQuery<>((Class<IService>)null).setServiceIdentifier(serviceid)).get();
	            	
	                // Hack to enable multi-part
	                // http://dev.eclipse.org/mhonarc/lists/jetty-users/msg03294.html
	                if(request.getContentType() != null && request.getContentType().startsWith("multipart/form-data")) 
	                	baseRequest.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, MULTI_PART_CONFIG);
	            	
	            	//handleRequest(service, pm, request, response, new Object[]{target, baseRequest});
                    try
                    {
                        //RequestManager.getInstance().handleRequest(new HttpRequest(request), new HttpResponse(response), new PublishContext(info, service, pm));// new Object[]{target, baseRequest});
                        IRequestManager man = RequestManagerFactory.getInstance();
                        //System.out.println("Request manager: "+man);
                        man.handleRequest(new HttpRequest(request), new HttpResponse(response), new PublishContext(info, service, pm));// new Object[]{target, baseRequest});
                    }
                    catch(Exception e)
                    {
                        throw new ServletException(e);
                    }
	            	
	            	baseRequest.setHandled(true);

	               	//System.out.println("handler is: "+uri.getPath()+" "+target);
	            }
	        };
	        ch.setContextPath(uri.getPath());
	        collhandler.prependHandler(ch);
         
            addUnpublishInfo(service.getServiceId(), () -> ((ContextHandlerCollection)server.getHandler()).removeHandler(ch));

	        ch.start(); // must be started explicitly :-((( PublishInfo info
	        System.out.println("added handler: "+uri.getPath());
	
	        addSidServer(service.getServiceId(), server);
	    }
	    catch(Exception e)
	    {
	    	SUtil.throwUnchecked(e);
	    	//ret.setException(e);
	    }
		
		//return ret;
    }

	/**
     *  Publish a service.e
     *  @param cl The classloader.
     *  @param service The original service.
     *  @param pid The publish id (e.g. url or name).
     */
    public synchronized void publishWsService(final IService service, final PublishInfo info, IComponent component)
    {
		try
	    {
	        final URI uri = new URI(getCleanPublishId(info.getPublishId(), component));

            if (uri.getPath().length() > 1) 
                System.getLogger(ServerManager.class.getName()).log(Level.WARNING, "Path in publishid: "+uri+" is ignored for WS, using fixed /ws endpoint");
                
            // fetch server to ensure that ws endpoint is created
            Server server = (Server) getHttpServer(uri).get(); 

            WsServiceRegistry registry = registries.get(uri.getPort());
            registry.add(info.getPublishName(), service);
            registry.addServiceInfo(service.getServiceId(), info);

            addUnpublishInfo(service.getServiceId(), () -> 
            {
                registry.remove(service.getServiceId());
                registry.removeServiceInfo(service.getServiceId());
            });

	        System.out.println("Adding service to ws registry (jetty): "+uri.getPath());
	    }
	    catch(Exception e)
	    {
	    	SUtil.rethrowAsUnchecked(e);
	    }
    }

    public synchronized IFuture<Object> getHttpServer(URI uri)//, PublishInfo info)
    {
        Future<Object> ret = new Future<>();
        Server server = null;

        try
        {
            server = getServer(uri.getPort());

            if (server == null)
            {
                System.out.println("Starting new server: " + uri.getPort());

                // WS Registry 
                WsServiceRegistry registry = new WsServiceRegistry();
                registries.put(uri.getPort(), registry);

                //server = new Server();

                QueuedThreadPool pool = new QueuedThreadPool(50, 2, 60000);
                pool.setDaemon(true);
                server = new Server(pool);
                ScheduledExecutorScheduler scheduler = new ScheduledExecutorScheduler("Session-Scheduler-Daemon", true);
                scheduler.start();
                server.addBean(scheduler);

                // Connector einrichten
                HttpConfiguration httpconfig = new HttpConfiguration();
                httpconfig.addCustomizer(new org.eclipse.jetty.server.ForwardedRequestCustomizer());
                ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(httpconfig));
                connector.setPort(uri.getPort());
                server.setConnectors(new ServerConnector[]{connector});

                // ContextHandlerCollection for WS + REST + Default
                ContextHandlerCollection contexts = new ContextHandlerCollection();
                server.setHandler(contexts);

                // WebSocket Context MUST BE FIRST
                ServletContextHandler wscontext = new ServletContextHandler(ServletContextHandler.SESSIONS);
                wscontext.setContextPath("/ws");
                JettyWebSocketServletContainerInitializer.configure(wscontext, (servletContext, wscontainer) -> 
                {
                    wscontainer.setMaxTextMessageSize(1024 * 1024);
                    wscontainer.setIdleTimeout(Duration.ZERO);
                    wscontainer.addMapping("/*", (req, resp) -> new WebSocketAdapter(registries.get(uri.getPort())));
                });
                contexts.addHandler(wscontext);
                wscontext.setAllowNullPathInfo(true);
                wscontext.start();

                // REST / HTTP Context
                ContextHandler restcontext = new ContextHandler();
                restcontext.setContextPath("/");
                restcontext.setHandler(new org.eclipse.jetty.server.handler.AbstractHandler()
                {
                    @Override
                    public void handle(String target, Request baserequest, HttpServletRequest request, HttpServletResponse response)
                        throws IOException, ServletException
                    {
                        if (baserequest.isHandled()) return;

                        try
                        {
                            IRequestManager man = RequestManagerFactory.getInstance();
                            man.handleRequest(new HttpRequest(request), new HttpResponse(response),
                                new PublishContext(null, null, null));
                            baserequest.setHandled(true);
                        }
                        catch(Exception e)
                        {
                            throw new ServletException(e);
                        }
                    }
                });
                contexts.addHandler(restcontext);
                restcontext.start();

                DefaultHandler dh = new DefaultHandler()
                {
                    @Override
                    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                            throws IOException, ServletException
                    {
                        if (baseRequest.isHandled()) return;
                        System.out.println("Default ignoring request: " + target);
                    }
                };
                contexts.addHandler(dh);
                dh.start();

                final Server fserver = server;
                server.addEventListener(new LifeCycle.Listener()
                {
                    public void lifeCycleStarted(LifeCycle event)
                    {
                        ret.setResult(fserver);
                    }
                });

                server.start();
                addPortServer(uri.getPort(), server);
            }
            else
            {
                ret.setResult(server);
            }
        }
        catch (Exception e)
        {
            ret.setException(e);
        }

        return ret;
    }
    
    /**
     *  Unpublish a service.
     *  @param sid The service identifier.
     */
    public synchronized void unpublishService(IServiceIdentifier sid)
    {
        Runnable unpublish = removeUnpublishInfo(sid);

        if(unpublish!=null)
        {
            System.out.println("Unpublishing: "+sid);
            unpublish.run();
        }
    }

    /**
     *  Unpublish a service.
     *  @param sid The service identifier.
     * /
    public synchronized void unpublishRestService(Server server, ContextHandler ch)
    {
    	//Tuple2<Server,ContextHandler> unpublish = removeUnpublishInfo(sid); //unpublishinfos.remove(sid);
    	//if(unpublish != null)
        ((ContextHandlerCollection)server.getHandler()).removeHandler(ch);
    	//((ContextHandlerCollection)unpublish.getFirstEntity().getHandler()).removeHandler(unpublish.getSecondEntity());
//	        throw new UnsupportedOperationException();
    }*/

    /**
     *  Unpublish a service.
     *  @param sid The service identifier.
     * /
    public synchronized void unpublishWsService(IServiceIdentifier sid, int port)
    {
    	WsServiceRegistry registry = registries.get(port));
        registry.remove(sid);
    }*/

    /**
     *  Publish a static page (without ressources).
     */
    public synchronized void publishHMTLPage(String pid, String vhost, final String html, IComponent component)
    {
    	try
        {
    		final URI uri = new URI(getCleanPublishId(pid, component));
        	//final IService service = (IService) component.getComponentFeature(IRequiredServicesFeature.class).searchService(new ServiceQuery<>( serviceid)).get();
        	
            Server server = (Server)getHttpServer(uri).get();
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
			        	
			            Server server = (Server)getHttpServer(uri).get();
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
//    	System.out.println("Jetty port servers terminated: "+ports);
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
	
        // $host is defined by component identifier also :-(
        String host = System.getProperty("host", System.getenv("host"));
        if(host!=null && host.length()>0)
        	id = id.replace("${host}", host);
        String port = System.getProperty("port", System.getenv("port"));
        if(port!=null && port.length()>0)
        	id = id.replace("${port}", port);
		
		String[] vars = findVariables(id);
		for(String var: vars)
		{
            try
            {
			    String val = ""+component.getValueProvider().getFetcher().fetchValue(var);
			    id = id.replace("${"+var+"}", val);
            }
            catch(Exception e)
            {
                if("host".equals(var))
                {
                   id = id.replace("${"+var+"}", "localhost");
                }
                else if("port".equals(var))
                {
                   id = id.replace("${"+var+"}", "8080");
                }
                else
                {
                    //System.err.println("Failed to fetch value for variable: "+var+" in publish id: "+id);
                    //e.printStackTrace();
                    SUtil.throwUnchecked(e);
                }
            }
        }
		
		return id;
	}
	
	public static String[] findVariables(String str) 
	{
		List<String> res = new ArrayList<String>();
		
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(str);

        while(matcher.find()) 
        {
            String name = matcher.group(1);
            res.add(name);
        }
        
        return res.toArray(new String[res.size()]);
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

    public synchronized void addUnpublishInfo(IServiceIdentifier serviceid, Runnable unpublish)
	{
		unpublishinfos.put(serviceid, unpublish);
	}
	
	public synchronized Runnable removeUnpublishInfo(IServiceIdentifier serviceid)
	{
		Runnable unpublish = unpublishinfos.remove(serviceid);
		return unpublish;
	}
	
	/*public synchronized void addUnpublishInfo(IServiceIdentifier serviceid, Server server, ContextHandler ch)
	{
		unpublishinfos.put(serviceid, new Tuple2<Server,ContextHandler>(server, ch));
	}
	
	public synchronized Tuple2<Server,ContextHandler> removeUnpublishInfo(IServiceIdentifier serviceid)
	{
		Tuple2<Server,ContextHandler> unpublish = unpublishinfos.remove(serviceid);
		return unpublish;
	}*/
}
