package jadex.bdi.shop;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.shop.ShopCapa.SellGoal;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.providedservice.annotation.Service;

/**
 *  The shop for buying goods at the shop.
 */
@Service
public class ShopService implements IShopService 
{
	//-------- attributes --------
	
	/** The component. */
	@Inject
	protected ShopCapa	shopcap;
	
	/** The component. */
	@Inject
	protected IComponent	agent;
	
	/** The shop name. */
	protected String name;
	
	//-------- constructors --------

	/**
	 *  Create a new shop service.
	 */
	public ShopService(String name)
	{
		this.name = name;
	}

	//-------- methods --------
	
	/**
	 *  Get the shop name. 
	 *  @return The name.
	 */
	public IFuture<String> getName()
	{
		return new Future<>(name);
	}
	
	/**
	 *  Buy an item.
	 *  @param item The item.
	 */
	public IFuture<ItemInfo> buyItem(final String item, final double price)
	{
//		System.out.println("buyItem in ShopService: "+ServiceCall.getCurrentInvocation().getCaller());
		
		SellGoal sell = shopcap.new SellGoal(item, price);
		return agent.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(sell);
	}
	
	/**
	 *  Get the item catalog.
	 *  @return  The catalog.
	 */	
	public IFuture<ItemInfo[]> getCatalog()
	{
		final Future<ItemInfo[]> ret = new Future<ItemInfo[]>();
		ret.setResult(shopcap.getCatalog().toArray(new ItemInfo[shopcap.getCatalog().size()]));
		return ret;
	}

	/**
	 *  Get the string representation.
	 *  @return The string representation.
	 */
	public String toString()
	{
		return name;
	}
}
