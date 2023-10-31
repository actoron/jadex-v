package jadex.mj.publishservicejetty.impl;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import jadex.core.impl.Component;
import jadex.future.IFuture;
import jadex.mj.publishservice.IPublishService;
import jadex.mj.publishservice.impl.PublishInfo;
import jadex.mj.publishservice.impl.PublishServiceFeature;
import jadex.providedservice.IServiceIdentifier;

public class PublishServiceJettyFeature extends PublishServiceFeature
{
	protected Set<Integer> ports;
	
	protected PublishServiceJettyFeature(Component self)
	{
		super(self);
		this.ports = new HashSet<Integer>();
	}
    
    public IFuture<Void> onStart()
    {
    	super.onStart().get();
    	//System.out.println("Jetty started");
    	return IFuture.DONE;
    }
    
    public IFuture<Void> onEnd()
    {
    	super.onEnd().get();
    	
    	// Terminate all servers created by this component
    	ServerManager.getInstance().terminateServers(ports);
    	
    	return IFuture.DONE;
    }
    
    /**
	 * Publish a service.
	 * 
	 * @param cl The classloader.
	 * @param service The original service.
	 * @param pid The publish id (e.g. url or name).
	 */
	public void publishService(IServiceIdentifier serviceid, PublishInfo info)
	{
		ServerManager.getInstance().publishService(serviceid, info, self);
	}
	
	/**
	 *  Unpublish a service.
	 *  @param sid The service identifier.
	 */
	public void unpublishService(IServiceIdentifier sid)
	{
		ServerManager.getInstance().unpublishService(sid);
	}

	/**
	 * Get or start an api to the http server.
	 */
	public IFuture<Object> getHttpServer(URI uri, PublishInfo info)
	{
		return ServerManager.getInstance().getHttpServer(uri, info);
	}
	
	/**
	 *  Publish a static page (without ressources).
	 */
	public void publishHMTLPage(String uri, String vhost, String html)
	{
		ServerManager.getInstance().publishHMTLPage(uri, vhost, html, self);
	}
	
	/**
	 *  Publish file resources from the classpath.
	 */
	public void publishResources(String uri, String rootpath)
	{
		ServerManager.getInstance().publishResources(uri, rootpath, self);
	}
    
    /**
     *  Test if publishing a specific type is supported (e.g. web service).
     *  @param publishtype The type to test.
     *  @return True, if can be published.
     */
    public IFuture<Boolean> isSupported(String publishtype)
    {
        return IPublishService.PUBLISH_RS.equals(publishtype) ? IFuture.TRUE : IFuture.FALSE;
    }

	/*public IFuture<Void> publishRedirect(URI uri, String html)
	{
        throw new UnsupportedOperationException();
	}


	public IFuture<Void> unpublish(String vhost, URI uri)
	{
        throw new UnsupportedOperationException();
	}

	
	public IFuture<Void> mirrorHttpServer(URI sourceserveruri, URI targetserveruri, PublishInfo info)
	{
        throw new UnsupportedOperationException();
	}


	public IFuture<Void> shutdownHttpServer(URI uri)
	{
        throw new UnsupportedOperationException();
	}*/
	
}