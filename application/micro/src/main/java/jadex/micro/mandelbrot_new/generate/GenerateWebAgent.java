package jadex.micro.mandelbrot_new.generate;

import jadex.core.IComponent;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentArgument;
import jadex.micro.annotation.Description;
import jadex.micro.mandelbrot_new.calculate.ICalculateService;
import jadex.micro.mandelbrot_new.display.IDisplayService;
import jadex.providedservice.ServiceScope;
import jadex.providedservice.annotation.Implementation;
import jadex.providedservice.annotation.ProvidedService;
import jadex.providedservice.annotation.ProvidedServices;
import jadex.publishservice.publish.annotation.Publish;
import jadex.requiredservice.IRequiredServiceFeature;
import jadex.requiredservice.annotation.OnService;
import jadex.requiredservice.annotation.RequiredService;
import jadex.requiredservice.annotation.RequiredServices;

/**
 *  Agent that can process generate requests.
 */
@Description("Agent offering a generate service.")
@ProvidedServices(@ProvidedService(type=IGenerateService.class, implementation=@Implementation(GenerateService.class)))
@RequiredServices({
	@RequiredService(name="displayservice", type=IDisplayService.class),
	@RequiredService(name="calculateservice", type=ICalculateService.class, scope=ServiceScope.GLOBAL), 
	@RequiredService(name="generateservice", type=IGenerateService.class)
})
@Publish(publishid="http://localhost:${port}/${app}/mandelbrotgenerate", publishtarget = IGenerateService.class)
@Agent
public class GenerateWebAgent
{
	@Agent
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
	
	@OnService(name="displayservice")
	protected void displayServiceAvailable(IDisplayService ds)
	{
		System.out.println("Found display service: "+ds);
		this.displayservice = ds;
		//if(calcservices.size()>0)
			agent.getFeature(IRequiredServiceFeature.class).getLocalService(IGenerateService.class).generateArea(null);
	}
}
