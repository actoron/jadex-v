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
 *  Agent providing a direct service.
 */
@Arguments(@Argument(name=TagProperty.NAME, clazz=String.class, defaultvalue="\"direct\""))
@ProvidedServices(@ProvidedService(type=IServiceCallService.class, scope=ServiceScope.GLOBAL,
	implementation=@Implementation(value=ServiceCallService.class,
		proxytype=Implementation.PROXYTYPE_DIRECT)))
@Agent
public class DirectServiceAgent
{
}
