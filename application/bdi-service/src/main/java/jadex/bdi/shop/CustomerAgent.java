package jadex.bdi.shop;

import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Capability;
import jadex.bdi.annotation.Mapping;
import jadex.micro.annotation.Agent;
import jadex.providedservice.ServiceScope;
import jadex.requiredservice.annotation.RequiredService;
import jadex.requiredservice.annotation.RequiredServices;

/**
 *  Customer capability.
 */
@Agent(type="bdi")
@RequiredServices({
	@RequiredService(name="localshopservices", type=IShopService.class, scope=ServiceScope.LOCAL), //multiple=true,
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
	protected double money = 100;

	/**
	 *  Get the capability.
	 *  @return the cap
	 */
	public CustomerCapability getCapability()
	{
		return cap;
	}
}
