package jadex.micro.mandelbrot.generate;

import jadex.core.IComponent;
import jadex.injection.annotation.Inject;
import jadex.micro.mandelbrot.display.IDisplayService;
import jadex.publishservice.publish.annotation.Publish;
import jadex.requiredservice.IRequiredServiceFeature;

/**
 *  Agent that can process generate requests.
 */
//@Description("Agent offering a generate service.")
//@ProvidedServices(@ProvidedService(type=IGenerateService.class, implementation=@Implementation(GenerateService.class)))
//@RequiredServices({
//	@RequiredService(name="displayservice", type=IDisplayService.class),
//	@RequiredService(name="calculateservice", type=ICalculateService.class, scope=ServiceScope.GLOBAL), 
//	@RequiredService(name="generateservice", type=IGenerateService.class)
//})
@Publish(publishid="http://localhost:${port}/${app}/mandelbrotgenerate", publishtarget = IGenerateService.class)
public class GenerateWebAgent
{
	@Inject
	protected IComponent agent;
	
	protected IDisplayService displayservice;
	
	protected String app;
	
	protected int port;

	public GenerateWebAgent()
	{
		this(8081, "mandelbrot");
	}
	
	public GenerateWebAgent(int port, String app)
	{
		this.port = port;
		this.app = app;
	}
	
//	@OnService(name="displayservice")
	@Inject
	protected void displayServiceAvailable(IDisplayService ds)
	{
		System.out.println("Found display service: "+ds);
		this.displayservice = ds;
		//if(calcservices.size()>0)
			agent.getFeature(IRequiredServiceFeature.class).getLocalService(IGenerateService.class).generateArea(null);
	}
}
