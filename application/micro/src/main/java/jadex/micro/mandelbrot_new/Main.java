package jadex.micro.mandelbrot_new;

import jadex.common.SUtil;
import jadex.core.IComponent;
import jadex.micro.mandelbrot_new.calculate.CalculateAgent;
import jadex.micro.mandelbrot_new.display.DisplayAgent;
import jadex.micro.mandelbrot_new.display.DisplayWebAgent;
import jadex.micro.mandelbrot_new.generate.GenerateAgent;
import jadex.micro.mandelbrot_new.generate.GenerateWebAgent;

/**
 *  Main for starting the example programmatically.
 */
public class Main 
{
	/**
	 *  Start a platform and the example.
	 */
	public static void main(String[] args) 
	{
		//SUtil.DEBUG = true;

		/*
		IComponent.create(new GenerateAgent());
		IComponent.create(new CalculateAgent());
		IComponent.create(new DisplayAgent());
		**/
		
		IComponent.create(new GenerateWebAgent());
		IComponent.create(new CalculateAgent());
		IComponent.create(new DisplayWebAgent());
	}
}



/*
@Arguments(
{
	@Argument(name="scope", clazz=ServiceScope.class, defaultvalue="ServiceScope.GLOBAL", description="Scope of the calculators")
})
@Imports(
{
	"jadex.platform.service.servicepool.*",
	"jadex.platform.service.distributedservicepool.*",
	"jadex.bridge.service.*",
	"jadex.bridge.service.search.*"
})
@ComponentTypes({
	@ComponentType(name="Generator", clazz=GenerateAgent.class),
	@ComponentType(name="Display", clazz=DisplayAgent.class),
	// DistriPool for finding and managing calculators on client side
	@ComponentType(name="DistributedPool", filename = "jadex/platform/service/distributedservicepool/DistributedServicePoolAgent.class"),
	// LocalPool for setting up own calculators
	@ComponentType(name="CalculatorPool", filename = "jadex/platform/service/servicepool/ServicePoolAgent.class")	// avoid compile time dependency to platform
})
@Configurations({
	
	@Configuration(name="default", components={
		@Component(type="Generator"),
		@Component(type="Display")
	})
	@Configuration(name="pool", components={
		@Component(type="Generator"),
		@Component(type="CalculatorPool", arguments = {
			@NameValue(name="serviceinfos",
				value="new PoolServiceInfo[]{new PoolServiceInfo().setWorkermodel(\"jadex/micro/examples/mandelbrot_new/CalculateAgent.class\").setServiceType(ICalculateService.class).setPoolStrategy(new jadex.commons.DefaultPoolStrategy(2, 2)).setPublicationScope($args.scope)}"),
			@NameValue(name="scope", value="$args.scope")
		}),
		@Component(type="DistributedPool", arguments = 
		{
			@NameValue(name="serviceinfo", value="new ServiceQuery(ICalculateService.class).setScope($args.scope)"),
			@NameValue(name="scope", value="$args.scope")	
		}),
		@Component(type="Display")
	}),
	@Configuration(name="pools", components={
		@Component(type="Generator"),
		@Component(type="CalculatorPool", number = "3", arguments = {
			@NameValue(name="serviceinfos",
				value="new PoolServiceInfo[]{new PoolServiceInfo().setWorkermodel(\"jadex/micro/examples/mandelbrot_new/CalculateAgent.class\").setServiceType(ICalculateService.class).setPoolStrategy(new jadex.commons.DefaultPoolStrategy(2, 2)).setPublicationScope($args.scope)}"),
			@NameValue(name="scope", value="$args.scope")
		}),
		@Component(type="Display"),
		@Component(type="DistributedPool", arguments = 
		{
			@NameValue(name="serviceinfo", value="new ServiceQuery(ICalculateService.class).setScope($args.scope)"),
			@NameValue(name="scope", value="$args.scope")	
		})
	}),
	@Configuration(name="nocalcs", components={
		@Component(type="Generator"),
		@Component(type="Display"),
		@Component(type="DistributedPool", arguments = 
		{
			@NameValue(name="serviceinfo", value="new ServiceQuery(ICalculateService.class).setScope($args.scope)"),
			@NameValue(name="scope", value="$args.scope")	
		})
	})
})
@Agent
public class MandelbrotAgent
{
}
*/