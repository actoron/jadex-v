package jadex.bdi.pureshop;

import java.util.List;

import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Capability;
import jadex.bdi.annotation.Mapping;
import jadex.bdi.runtime.Val;
import jadex.core.IComponent;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;
import jadex.providedservice.ServiceScope;
import jadex.requiredservice.annotation.RequiredService;
import jadex.requiredservice.annotation.RequiredServices;


/**
 * 
 */
@Agent(type="bdip")
@RequiredServices({
	@RequiredService(name="localshopservices", type=IShopService.class, scope=ServiceScope.VM), //multiple=true,
	@RequiredService(name="remoteshopservices", type=IShopService.class, scope=ServiceScope.GLOBAL), // multiple=true,
})
public class ShopAndCustomerAgent
{
	//-------- attributes --------

	/** The agent. */
	@Agent
	protected IComponent agent;
	
	/** The customer capability. */
	@Capability(beliefmapping=@Mapping("money"))
	protected CustomerCapability customercap = new CustomerCapability();

	/** The shop capability. */
	@Capability(beliefmapping=@Mapping(value="money", target="money"))
	protected ShopCapa shopcap;
	
	public ShopAndCustomerAgent(String shopname, List<ItemInfo> catalog)
	{
		this.shopcap	= new ShopCapa(shopname, catalog);
	}
	
	/** The money. */
	@Belief
	protected Val<Double> money = new Val<>(100.0);
	
	// TODO: support @OnStart for capabilities
	@OnStart
	void	start()
	{
		customercap.start();
	}
}
