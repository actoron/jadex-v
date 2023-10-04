package jadex.mj.publishservice.impl;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.UUID;

import jadex.common.SReflect;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.mj.core.IComponent;
import jadex.mj.core.MjComponent;
import jadex.mj.core.modelinfo.ModelInfo;
import jadex.mj.feature.lifecycle.impl.IMjLifecycle;
import jadex.mj.feature.providedservice.IMjProvidedServiceFeature;
import jadex.mj.feature.providedservice.IService;
import jadex.mj.feature.providedservice.IServiceIdentifier;
import jadex.mj.micro.MjMicroAgent;
import jadex.mj.publishservice.IMjPublishServiceFeature;
import jadex.mj.publishservice.IPublishService;
import jadex.mj.publishservice.impl.RequestManager.MappingInfo;
import jadex.mj.publishservice.impl.RequestManager.MappingInfo.HttpMethod;
import jadex.mj.publishservice.publish.PathManager;
import jadex.serialization.ISerializationServices;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;

public abstract class MjPublishServiceFeature implements IMjLifecycle, IMjPublishServiceFeature//, IParameterGuesser
{	
	/** The component. */
	protected MjComponent self;
	
	protected MjPublishServiceFeature(MjComponent self)
	{
		this.self	= self;
	}
	
	public IFuture<Void> onStart()
	{
		//Future<Void> ret = new Future<Void>();
		
		ModelInfo model = (ModelInfo)self.getModel();
		
		PublishServiceModel mymodel = (PublishServiceModel)model.getFeatureModel(IMjPublishServiceFeature.class);
		if(mymodel==null)
		{
			mymodel = (PublishServiceModel)PublishServiceLoader.readFeatureModel(((MjMicroAgent)self).getPojo().getClass(), this.getClass().getClassLoader());
			model.putFeatureModel(IMjPublishServiceFeature.class, mymodel);
			
			// todo: save model to cache
		}
		
		// do we want to chain the publication on serviceStart and serviceEnd of eacht service?!
		// how could this be done? with listeners on other feature?!
		for(PublishInfo pi: mymodel.getPublishInfos())
		{
			IServiceIdentifier sid = findService(pi.getPublishTarget());
			publishService(sid, pi).get();
		}
		
		return IFuture.DONE;
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
		ISerializationServices ss = getComponent().getFeature(IMjProvidedServiceFeature.class).getSerializationService();
		RequestManager.getInstance(ss).handleRequest(service, pm, request, response, others);
	}
	
	/**
	 * Get the cleaned publish id. Square brackets for the optional host and
	 * context part are removed.
	 */
	public String getCleanPublishId(String id)
	{
		return id != null ? id.replace("[", "").replace("]", "") : null;
	}
	
	/**
	 * Evaluate the service interface and generate mappings. Return a
	 * multicollection in which for each path name the possible methods are
	 * contained (can be more than one due to different parameters).
	 */
	//public IFuture<MultiCollection<String, MappingInfo>> evaluateMapping(IServiceIdentifier sid, PublishInfo pi)
	public IFuture<PathManager<MappingInfo>> evaluateMapping(IServiceIdentifier sid, PublishInfo pi, ClassLoader cl)
	{
		Future<PathManager<MappingInfo>> reta = new Future<>();

		UUID cid = sid.getProviderId();

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
			
		return reta;
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