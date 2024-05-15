package jadex.bdi.pureshop;

import java.util.ArrayList;
import java.util.List;

import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Capability;
import jadex.bdi.annotation.Mapping;
import jadex.bdi.runtime.wrappers.belief;
import jadex.core.IComponent;
import jadex.micro.annotation.Agent;
import jadex.providedservice.annotation.Implementation;
import jadex.providedservice.annotation.ProvidedService;
import jadex.providedservice.annotation.ProvidedServices;

/**
 *  Shop bdi agent.
 */
@Agent(type="bdip")
@ProvidedServices(@ProvidedService(type=IShopService.class, implementation=@Implementation(value=ShopService.class)))
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
	@Capability(beliefmapping=@Mapping("money"))
	protected ShopCapa shopcap;
	
	/** The money. */
	@Belief
	protected belief<Double> money = new belief<>(100.0);
	
	public ShopAgent(String shopname, List<ItemInfo> catalog)
	{
		this.shopcap	= new ShopCapa(shopname, catalog);
	}
	
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
