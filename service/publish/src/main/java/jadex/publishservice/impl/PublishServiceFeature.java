package jadex.publishservice.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import jadex.core.IComponent;
import jadex.core.impl.Component;
import jadex.core.impl.ILifecycle;
import jadex.future.IFuture;
import jadex.providedservice.IService;
import jadex.providedservice.IServiceIdentifier;
import jadex.publishservice.IPublishService;
import jadex.publishservice.IPublishServiceFeature;
import jadex.publishservice.IRequestManager.PublishContext;
import jadex.publishservice.impl.v2.Request;
import jadex.publishservice.impl.v2.Response;

public abstract class PublishServiceFeature implements IPublishServiceFeature, ILifecycle//, IParameterGuesser
{
	/** The component. */
	protected Component self;
	
	protected List<IServiceIdentifier> published = new ArrayList<>();

	protected PublishServiceFeature(Component self)
	{
		this.self	= self;
		RequestManagerFactory.createInstance();
	}
	
	public IComponent getComponent()
	{
		return self;
	}

	//-------- ILifecycle methods --------
	
	@Override
	public void	init()
	{
		// NOP -> injection is done by extending injection feature in Provided2FeatureProvider
	}
	
	@Override
	public void	cleanup()
	{
		for(IServiceIdentifier sid: published.toArray(new IServiceIdentifier[published.size()]))
		{
			unpublishService(sid);
		}
	}

	/**
	 * Test if publishing a specific type is supported (e.g. web service).
	 * 
	 * @param publishtype The type to test.
	 * @return True, if can be published.
	 */
	public IFuture<Boolean> isSupported(String publishtype)
	{
		return IPublishService.PUBLISH_RS.equals(publishtype) ? IFuture.TRUE : IFuture.FALSE;
	}
	
	public ClassLoader getClassLoader()
	{
		return this.getClass().getClassLoader();
	}
	
	/**
	 * Handle a web request.
	 * 
	 * @param service The service.
	 * @param mappings The collected mapping infos for the service.
	 * @param request The request.
	 * @param response The response.
	 */
	public void handleRequest(final Request request, final Response response, PublishContext context)
		throws Exception
	{
		RequestManager.getInstance().handleRequest(request, response, context);//, others);
	}

	/**
	 * Publish a service.
	 * 
	 * @param cl The classloader.
	 * @param service The original service.
	 * @param pid The publish id (e.g. url or name).
	 */
	public IFuture<Void> publishService(IService service, PublishInfo info)
	{
		published.add(service.getServiceId());
		return IFuture.DONE;
	}

	/**
	 * Unpublish a service.
	 * @param sid The service id.
	 */
	public void unpublishService(IServiceIdentifier sid)
	{
		published.remove(sid);
	}

	/**
	 * Get or start an api to the http server.
	 */
	public abstract Object getHttpServer(URI uri);//, PublishInfo info);	
}