package jadex.bdi.shop;

import java.util.List;

import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Capability;
import jadex.bdi.annotation.Mapping;
import jadex.bdi.runtime.IBDIAgentFeature;
import jadex.core.IComponent;
import jadex.micro.annotation.Agent;


/**
 * 
 */
@Agent(type="bdi")
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
	@SuppressWarnings("unchecked")
	@Capability(beliefmapping=@Mapping(value="money", target="money"))
	protected ShopCapa shopcap = new ShopCapa((String)agent.getFeature(IBDIAgentFeature.class).getArgument("shopname"), 
		(List<ItemInfo>)agent.getFeature(IBDIAgentFeature.class).getArgument("catalog"));
	
	/** The money. */
	@Belief
	protected double money	= 100.0;
}
