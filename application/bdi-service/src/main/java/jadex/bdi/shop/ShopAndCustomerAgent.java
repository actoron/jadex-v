package jadex.bdi.shop;

import java.util.List;

import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Capability;
import jadex.injection.Val;


/**
 *  Agent with both shop and customer capability.
 */
@BDIAgent
public class ShopAndCustomerAgent
{
	//-------- attributes --------

	/** The customer capability. */
	@Capability//(beliefmapping=@Mapping("money"))
	protected CustomerCapability customercap = new CustomerCapability();

	/** The shop capability. */
	@Capability//(beliefmapping=@Mapping(value="money", target="money"))
	protected ShopCapa shopcap;
	
	/** The money. */
	@Belief
	protected Val<Double>	money	= new Val<>(100.0);
	
	//-------- constructors --------
	
	/**
	 *  Create a shop agent.
	 */
	public ShopAndCustomerAgent(String shopname, List<ItemInfo> catalog)
	{
		this.shopcap	= new ShopCapa(shopname, catalog);
	}
}
