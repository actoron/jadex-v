package jadex.publishservice.impl;

import java.io.IOException;
import java.net.URI;

import jadex.core.IComponent;
import jadex.core.impl.Component;
import jadex.future.IFuture;
import jadex.providedservice.IService;
import jadex.publishservice.IPublishService;
import jadex.publishservice.IPublishServiceFeature;
import jadex.publishservice.IRequestManager.PublishContext;
import jadex.publishservice.impl.MappingEvaluator.MappingInfo;
import jadex.publishservice.impl.v2.Request;
import jadex.publishservice.impl.v2.Response;
import jadex.publishservice.impl.v2.http.HttpRequest;
import jadex.publishservice.impl.v2.http.HttpResponse;
import jadex.publishservice.publish.PathManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public abstract class PublishServiceFeature implements IPublishServiceFeature//, IParameterGuesser
{
	/** The component. */
	protected Component self;
	
	protected PublishServiceFeature(Component self)
	{
		this.self	= self;
		RequestManager.createInstance();
	}
	
	public IComponent getComponent()
	{
		return self;
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
	public abstract IFuture<Void> publishService(IService service, PublishInfo info);

	/**
	 * Get or start an api to the http server.
	 */
	public abstract Object getHttpServer(URI uri, PublishInfo info);	
}