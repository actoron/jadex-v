package jadex.micro.servicecall;

import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Argument;
import jadex.micro.annotation.Arguments;
import jadex.nfproperty.sensor.service.TagProperty;
import jadex.providedservice.ServiceScope;
import jadex.providedservice.annotation.Implementation;
import jadex.providedservice.annotation.ProvidedService;
import jadex.providedservice.annotation.ProvidedServices;

/**
 *  Agent providing a raw service.
 */
@Arguments(@Argument(name=TagProperty.NAME, clazz=String.class, defaultvalue="\"raw\""))
@ProvidedServices(@ProvidedService(type=IServiceCallService.class, scope=ServiceScope.GLOBAL,
	implementation=@Implementation(expression="new RawServiceCallService($component.getId())",
		proxytype=Implementation.PROXYTYPE_RAW)))
@Agent
public class RawServiceAgent
{
//	@Agent
//	protected IInternalAccess agent;
//	
//	@AgentKilled
//	public void killed()
//	{
//		System.out.println("killing: "+agent.getComponentIdentifier());
//	}
}
