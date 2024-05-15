package jadex.bdi.pureshop;

import jadex.bdi.pureshop.ShopCapa.SellGoal;
import jadex.bdi.runtime.IBDIAgentFeature;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;
import jadex.providedservice.annotation.ServiceComponent;

/**
 *  The shop for buying goods at the shop.
 */
@Service
public class ShopService implements IShopService 
{
	//-------- attributes --------
	
	/** The component. */
	@ServiceComponent
	protected ShopAgent	shopagent;
	
	/** The component. */
	@ServiceComponent
	protected IComponent	agent;
	
//	/** The component. */
//	@ServiceComponent
//	protected ICapability	capa;
	
	//-------- methods --------
	
	/**
	 *  Get the shop name. 
	 *  @return The name.
	 *  
	 *  @directcall (Is called on caller thread).
	 */
	public String getName()
	{
		ShopCapa shop = shopagent.shopcap;
		return shop.getShopname();
	}
	
	/**
	 *  Buy an item.
	 *  @param item The item.
	 */
	public IFuture<ItemInfo> buyItem(final String item, final double price)
	{
		//TODO
//		System.out.println("buyItem in ShopService: "+ServiceCall.getCurrentInvocation().getCaller());
		
//		ShopCapa shop = (ShopCapa)capa.getPojoCapability();
		ShopCapa shop = shopagent.shopcap;
		SellGoal sell = shop.new SellGoal(item, price);
		return agent.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(sell);
	}
	
	/**
	 *  Get the item catalog.
	 *  @return  The catalog.
	 */	
	public IFuture<ItemInfo[]> getCatalog()
	{
		final Future<ItemInfo[]> ret = new Future<ItemInfo[]>();
		ShopCapa shop = shopagent.shopcap;
		ret.setResult(shop.getCatalog().toArray(new ItemInfo[shop.getCatalog().size()]));
		return ret;
	}

	/**
	 *  Get the string representation.
	 *  @return The string representation.
	 */
	public String toString()
	{
		return getName();
	}
}
