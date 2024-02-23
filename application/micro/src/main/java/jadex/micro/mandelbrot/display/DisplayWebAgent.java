package jadex.micro.mandelbrot.display;

import jadex.common.SGUI;
import jadex.core.IComponent;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Description;
import jadex.micro.mandelbrot.generate.IGenerateService;
import jadex.model.annotation.OnStart;
import jadex.providedservice.annotation.Implementation;
import jadex.providedservice.annotation.ProvidedService;
import jadex.providedservice.annotation.ProvidedServices;
import jadex.publishservice.IPublishServiceFeature;
import jadex.publishservice.publish.annotation.Publish;
import jadex.requiredservice.annotation.RequiredService;
import jadex.requiredservice.annotation.RequiredServices;

/**
 *  Agent offering a display service.
 */
@Description("Agent offering a web display service.")
@ProvidedServices({
	@ProvidedService(type=IDisplayService.class, implementation=@Implementation(DisplayService.class))
})
@RequiredServices({
	@RequiredService(name="generateservice", type=IGenerateService.class),
})
@Publish(publishid="http://localhost:${port}/${app}/mandelbrotdisplay", publishtarget = IDisplayService.class)
@Agent
public class DisplayWebAgent
{
	//-------- attributes --------
	
	/** The agent. */
	@Agent
	protected IComponent agent;
		
	protected String app;
	
	protected int port;

	public DisplayWebAgent()
	{
		this(8081, "mandelbrot");
	}
	
	public DisplayWebAgent(int port, String app)
	{
		this.port = port;
		this.app = app;
	}
	
	//-------- MicroAgent methods --------
	
	@OnStart
	public void onStart()
	{
		IPublishServiceFeature ps = agent.getFeature(IPublishServiceFeature.class);
		ps.publishResources("http://localhost:"+port+"/"+app, "jadex/micro/mandelbrot/webui");
		
		System.out.println("open in browser");
		SGUI.openInBrowser("http://localhost:"+port+"/"+app);
		
		//wps.publishResources("[http://localhost:"+port+"/mandelbrot]", "META-INF/resources/mandelbrot").get();
	}
	
	/**
	 *  Wait for the IWebPublishService and then publish the resources.
	 *  @param pubser The publish service.
	 * /
	@OnService(requiredservice = @RequiredService(min = 1, max = 1))
	protected void publish(IWebPublishService wps)
	{
		IServiceIdentifier sid = ((IService)agent.getProvidedService(IDisplayService.class)).getServiceId();
		
		wps.publishService(sid, new PublishInfo("[http://localhost:"+port+"/]mandelbrotdisplay", IPublishService.PUBLISH_RS, null)).get();
		
		System.out.println("display publish started: "+wps);
		wps.publishResources("[http://localhost:"+port+"/mandelbrot]", "META-INF/resources/mandelbrot").get();
	}*/
}
