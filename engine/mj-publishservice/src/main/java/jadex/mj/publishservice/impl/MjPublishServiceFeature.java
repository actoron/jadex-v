package jadex.mj.publishservice.impl;

import java.io.IOException;
import java.net.URI;

import jadex.common.SReflect;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.mj.core.IComponent;
import jadex.mj.core.impl.MjComponent;
import jadex.mj.feature.execution.impl.IMjLifecycle;
import jadex.mj.feature.providedservice.IMjProvidedServiceFeature;
import jadex.mj.feature.providedservice.IService;
import jadex.mj.feature.providedservice.IServiceIdentifier;
import jadex.mj.micro.MjMicroAgent;
import jadex.mj.model.AbstractModelLoader;
import jadex.mj.model.IMjModelFeature;
import jadex.mj.model.modelinfo.ModelInfo;
import jadex.mj.publishservice.IMjPublishServiceFeature;
import jadex.mj.publishservice.IPublishService;
import jadex.mj.publishservice.impl.RequestManager.MappingInfo;
import jadex.mj.publishservice.publish.PathManager;
import jadex.serialization.ISerializationServices;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public abstract class MjPublishServiceFeature implements IMjLifecycle, IMjPublishServiceFeature//, IParameterGuesser
{	
	/** The component. */
	protected MjComponent self;
	
	protected MjPublishServiceFeature(MjComponent self)
	{
		this.self	= self;
		ISerializationServices ss = getComponent().getFeature(IMjProvidedServiceFeature.class).getSerializationService();
		RequestManager.createInstance(ss);
	}
	
	public IFuture<Void> onStart()
	{
		Future<Void> ret = new Future<Void>();
		
		ModelInfo model = (ModelInfo)self.getFeature(IMjModelFeature.class).getModel();
		
		PublishServiceModel mymodel = (PublishServiceModel)model.getFeatureModel(IMjPublishServiceFeature.class);
		if(mymodel==null)
		{
			mymodel = (PublishServiceModel)PublishServiceLoader.readFeatureModel(((MjMicroAgent)self).getPojo().getClass(), this.getClass().getClassLoader());
			PublishServiceModel fmymodel = mymodel;
			
			AbstractModelLoader loader = AbstractModelLoader.getLoader(self.getClass());
			loader.updateCachedModel(() ->
			{
				model.putFeatureModel(IMjPublishServiceFeature.class, fmymodel);
			});
		}
		
		try
		{
			// do we want to chain the publication on serviceStart and serviceEnd of eacht service?!
			// how could this be done? with listeners on other feature?!
			for(PublishInfo pi: mymodel.getPublishInfos())
			{
				IServiceIdentifier sid = findService(pi.getPublishTarget());
				publishService(sid, pi);
			}
			
			ret.setResult(null);
		}
		catch(Exception e)
		{
			ret.setException(e);
		}
		
		return ret;
	}
	
	public IFuture<Void> onEnd()
	{
		return IFuture.DONE;
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
		
		if(target.length()==0)
		{
			Object[] sers = getComponent().getFeature(IMjProvidedServiceFeature.class).getProvidedServices(null);
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
				ser = getComponent().getFeature(IMjProvidedServiceFeature.class).getProvidedService(target);
			}
			else
			{
				ser = (IService)getComponent().getFeature(IMjProvidedServiceFeature.class).getProvidedService(type);
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
	public abstract void publishService(IServiceIdentifier serviceid, PublishInfo info);

	/**
	 * Get or start an api to the http server.
	 */
	public abstract Object getHttpServer(URI uri, PublishInfo info);

}