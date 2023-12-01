package jadex.bdi.shop;

import java.util.ArrayList;
import java.util.List;

import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Capability;
import jadex.bdi.annotation.Mapping;
import jadex.bdi.runtime.IBDIAgentFeature;
import jadex.core.IComponent;
import jadex.micro.annotation.Agent;

/**
 *  Shop bdi agent.
 */
@Agent(type="bdi")
public class ShopAgent
{
	//-------- attributes --------

	@Agent
	protected IComponent agent;
	
	// Principles: 
	// - each belief should only be represented as one field! (no assignments)
	// - access of beliefs of capabilities via getters/setters
	// - delegation to the outside via own getter/setters (allows renaming)
	// - abstract beliefs need to be declared via native getter/setter pairs
	
	/** The customer capability. */
	@SuppressWarnings("unchecked")
	@Capability(beliefmapping=@Mapping("money"))
	protected ShopCapa shopcap	= new ShopCapa((String)agent.getFeature(IBDIAgentFeature.class).getArgument("shopname"), 
		(List<ItemInfo>)agent.getFeature(IBDIAgentFeature.class).getArgument("catalog"));
	
	/** The money. */
	@Belief
	protected double	money	= 100;
	
	/**
	 *  Get some default catalog.
	 */
	public static List<ItemInfo> getDefaultCatalog()
	{
		List<ItemInfo> ret = new ArrayList<ItemInfo>();
		ret.add(new ItemInfo("Paper", 0.89, 10));
		ret.add(new ItemInfo("Pencil", 0.56, 2));
		return ret;
	}
}
