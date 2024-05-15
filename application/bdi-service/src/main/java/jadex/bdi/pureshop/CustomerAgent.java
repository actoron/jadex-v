package jadex.bdi.pureshop;

import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Capability;
import jadex.bdi.annotation.Mapping;
import jadex.bdi.runtime.wrappers.belief;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;
import jadex.providedservice.ServiceScope;
import jadex.requiredservice.annotation.RequiredService;
import jadex.requiredservice.annotation.RequiredServices;

/**
 *  Customer capability.
 */
@Agent(type="bdip")
@RequiredServices({
	@RequiredService(name="localshopservices", type=IShopService.class, scope=ServiceScope.VM), //multiple=true,
	@RequiredService(name="remoteshopservices", type=IShopService.class, scope=ServiceScope.GLOBAL), // multiple=true,
})
public class CustomerAgent
{
	//-------- attributes --------

	/** The customer capability. */
	@Capability(beliefmapping=@Mapping("money"))
	protected CustomerCapability cap = new CustomerCapability();
	
	/** The money. */
	@Belief
	protected belief<Double> money = new belief<>(100.0);
	
	// TODO: support @OnStart for capabilities
	@OnStart
	void	start()
	{
		cap.start();
	}
}
