package jadex.bdi.shop;

import java.util.List;

import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Capability;
import jadex.core.IComponent;
import jadex.injection.Val;
import jadex.injection.annotation.Inject;

/**
 *  Shop bdi agent.
 */
@BDIAgent
public class ShopAgent
{
	//-------- attributes --------

	@Inject
	protected IComponent agent;
	
	// Principles: 
	// - each belief should only be represented as one object (Val, List...)
	// Abstract beliefs get assigned the object on startup.
	// TODO: abstract bean belief!? -> use Val
	
	/** The shop capability. */
	@Capability//(beliefmapping=@Mapping("money"))
	protected ShopCapa shopcap;
	
	/** The money. */
	@Belief
	protected Val<Double>	money	= new Val<>(100.0);
	
	//-------- constructors --------
	
	/**
	 *  Create a shop agent.
	 */
	public ShopAgent(String shopname, List<ItemInfo> catalog)
	{
		this.shopcap	= new ShopCapa(shopname, catalog);
	}
}
