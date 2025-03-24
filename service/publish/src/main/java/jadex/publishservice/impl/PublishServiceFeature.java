package jadex.publishservice.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.core.IComponent;
import jadex.core.impl.Component;
import jadex.future.IFuture;
import jadex.injection.impl.IInjectionHandle;
import jadex.injection.impl.InjectionModel;
import jadex.providedservice.IService;
import jadex.publishservice.IPublishService;
import jadex.publishservice.IPublishServiceFeature;
import jadex.publishservice.impl.RequestManager.MappingInfo;
import jadex.publishservice.publish.PathManager;
import jadex.publishservice.publish.annotation.Publish;
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
	public abstract IFuture<Void> publishService(IService service, PublishInfo info);

	/**
	 * Get or start an api to the http server.
	 */
	public abstract Object getHttpServer(URI uri, PublishInfo info);
	
	//-------- injection model extension --------
	
	static
	{
		InjectionModel.addExtraOnStart(new Function<Class<?>, List<IInjectionHandle>>()
		{
			@Override
			public List<IInjectionHandle> apply(Class<?> pojoclazz)
			{
				List<IInjectionHandle>	ret	= new ArrayList<>();
				
				// Find class with publish annotation.
				Class<?>	test	= pojoclazz;
				while(test!=null)
				{
					if(test.isAnnotationPresent(Publish.class))
					{
						PublishInfo pi = getPublishInfo(test.getAnnotation(Publish.class));
						ret.add((comp, pojos, context) ->
						{
							IPublishServiceFeature	feature	= comp.getFeature(IPublishServiceFeature.class);
							// do we want to chain the publication on serviceStart and serviceEnd of eacht service?!
							// how could this be done? with listeners on other feature?!
							feature.publishService((IService)pojos.get(pojos.size()-1), pi).get();
						});
					}
					
					test	= test.getSuperclass();
				}
				
				// Find fields with publish annotation.
				for(Field f: InjectionModel.findFields(pojoclazz, Publish.class))
				{
					try
					{
						PublishInfo pi = getPublishInfo(f.getAnnotation(Publish.class));
	
						f.setAccessible(true);
						MethodHandle	fhandle	= MethodHandles.lookup().unreflectGetter(f);
						ret.add((comp, pojos, context) ->
						{
							try
							{
								IPublishServiceFeature	feature	= comp.getFeature(IPublishServiceFeature.class);
								Object	servicepojo	= fhandle.invoke(pojos.get(pojos.size()-1));
								if(servicepojo==null)
								{
									throw new RuntimeException("No value for provided service: "+f);
								}
								// do we want to chain the publication on serviceStart and serviceEnd of eacht service?!
								// how could this be done? with listeners on other feature?!
								feature.publishService((IService)servicepojo, pi).get();
							}
							catch(Throwable e)
							{
								SUtil.throwUnchecked(e);
							}
						});
					}
					catch(Exception e)
					{
						SUtil.throwUnchecked(e);
					}
				}
				return ret;
			}
		});
	}
	
	/**
	 *  Convert annotation to info object.
	 */
	// TODO: Just use annotation?
	protected static PublishInfo getPublishInfo(Publish p)
	{
		String pt = p.publishtagetname().length()>0? p.publishtagetname(): null;
		if(pt==null && !p.publishtarget().equals(Object.class))
			pt = SReflect.getClassName(p.publishtarget());
		
		PublishInfo pi = new PublishInfo(p.publishid(), p.publishtype(), pt, Object.class.equals(p.mapping())? null: p.mapping());
		return pi;
	}
}