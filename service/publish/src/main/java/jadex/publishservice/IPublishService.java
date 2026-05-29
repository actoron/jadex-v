package jadex.publishservice;

import jadex.core.IComponentFeature;
import jadex.future.IFuture;
import jadex.providedservice.IService;
import jadex.providedservice.IServiceIdentifier;
import jadex.publishservice.impl.PublishInfo;

/**
 *  Service for publishing services in other technologies such as web services.
 */
//@Service(system=true)
public interface IPublishService extends IComponentFeature
{
	/** The publish type web service. */
	public static final String PUBLISH_WS = "ws";
	
	/** The publish type rest service. */
	public static final String PUBLISH_RS = "rs";
	
	/** The default publish implementations for rest. * /
	public static final String[] DEFAULT_RSPUBLISH_COMPONENTS = new String[]
	{
		"/jadex/extension/rs/publish/NanoRSPublishAgent.class",
		"/jadex/extension/rs/publish/JettyRSPublishAgent.class",
		"/jadex/extension/rs/publish/GrizzlyRSPublishAgent.class",
		"/jadex/extension/rs/publish/LegacyGrizzlyRSPublishAgent.class",
		"/jadex/extension/rs/publish/ExternalRSPublishAgent.class"
	};*/
	
	/**
	 *  Test if publishing a specific type is supported (e.g. web service).
	 *  @param publishtype The type to test.
	 *  @return True, if can be published.
	 */
	public IFuture<Boolean> isSupported(String publishtype);
	
	/**
	 *  Publish a service.
	 *  @param service The original service.
	 *  @param pid The publish id (e.g. url or name).
	 */
	public IFuture<Void> publishService(IService service, PublishInfo pi);
	
	/**
	 *  Unpublish a service.
	 *  @param sid The service identifier.
	 */
	public void unpublishService(IServiceIdentifier sid);
}
