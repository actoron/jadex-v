package jadex.publishservice.impl;

import java.io.IOException;
import java.net.URI;

import jadex.common.SReflect;
import jadex.core.IComponent;
import jadex.core.IComponentFeature;
import jadex.core.impl.Component;
import jadex.execution.impl.ILifecycle;
import jadex.future.IFuture;
import jadex.micro.MicroAgent;
import jadex.model.IModelFeature;
import jadex.model.impl.AbstractModelLoader;
import jadex.model.modelinfo.ModelInfo;
import jadex.providedservice.IProvidedServiceFeature;
import jadex.providedservice.IService;
import jadex.providedservice.IServiceIdentifier;
import jadex.publishservice.IPublishService;
import jadex.publishservice.IPublishServiceFeature;
import jadex.publishservice.impl.RequestManager.MappingInfo;
import jadex.publishservice.publish.PathManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public abstract class PublishServiceFeature implements ILifecycle, IPublishServiceFeature, IComponentFeature//, IParameterGuesser
{	
	/** The component. */
	protected Component self;
	
	protected PublishServiceFeature(Component self)
	{
		this.self	= self;
		RequestManager.createInstance();
	}
	
	public void	onStart()
	{
		ModelInfo model = (ModelInfo)self.getFeature(IModelFeature.class).getModel();
		
		PublishServiceModel mymodel = (PublishServiceModel)model.getFeatureModel(IPublishServiceFeature.class);
		if(mymodel==null)
		{
			mymodel = (PublishServiceModel)PublishServiceLoader.readFeatureModel(((MicroAgent)self).getPojo().getClass(), this.getClass().getClassLoader());
			PublishServiceModel fmymodel = mymodel;
			AbstractModelLoader loader = AbstractModelLoader.getLoader(self.getClass());
			loader.updateCachedModel(() ->
			{
				model.putFeatureModel(IPublishServiceFeature.class, fmymodel);
			});
		}
		
		// do we want to chain the publication on serviceStart and serviceEnd of eacht service?!
		// how could this be done? with listeners on other feature?!
		for(PublishInfo pi: mymodel.getPublishInfos())
		{
			IServiceIdentifier sid = findService(pi.getPublishTarget());
			publishService(sid, pi).get();
		}
	}
	
	public void	onEnd()
	{
	}
	
	/**
	 *  Find a provided service per its provided service name or type.
	 *  @param target The service name or type.
	 *  @return The service id of the service.
	 */
	protected IServiceIdentifier findService(String target)
	{
		IServiceIdentifier ret = null;
		IService ser = null;
		
		if(target==null || target.length()==0)
		{
			Object[] sers = getComponent().getFeature(IProvidedServiceFeature.class).getProvidedServices(null);
			if(sers.length==1)
			{
				ser = (IService)sers[0];
			}
			else if(sers.length>1)
			{
				System.out.println("More than one service of ");
			}
		}
		else
		{
			Class<?> type = SReflect.findClass0(target, null, getClassLoader());
			if(type==null)
			{
				ser = getComponent().getFeature(IProvidedServiceFeature.class).getProvidedService(target);
			}
			else
			{
				ser = (IService)getComponent().getFeature(IProvidedServiceFeature.class).getProvidedService(type);
			}
		}
		
		if(ser!=null)
			ret = ser.getServiceId();
		else
			System.out.println("provided service not found: "+target);
		
		return ret;
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
	public void handleRequest(IService service, PathManager<MappingInfo> pm, final HttpServletRequest request, final HttpServletResponse response, Object[] others)
		throws IOException, ServletException
	{
		RequestManager.getInstance().handleRequest(service, pm, request, response, others);
	}

	/**
	 * Publish a service.
	 * 
	 * @param cl The classloader.
	 * @param service The original service.
	 * @param pid The publish id (e.g. url or name).
	 */
	public abstract IFuture<Void> publishService(IServiceIdentifier serviceid, PublishInfo info);

	/**
	 * Get or start an api to the http server.
	 */
	public abstract Object getHttpServer(URI uri, PublishInfo info);

}