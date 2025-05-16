package jadex.bdi.shop;

import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

/**
 *  The shop interface for buying goods at the shop.
 */
@Service
public interface IShopService
{
	/**
	 *  Get the shop name. 
	 *  @return The name.
	 */
	public IFuture<String> getName();
	
	/**
	 *  Buy an item.
	 *  @param item The item.
	 */
	public IFuture<ItemInfo> buyItem(String item, double price);
	
	/**
	 *  Get the item catalog.
	 *  @return  The catalog.
	 */
	public IFuture<ItemInfo[]> getCatalog();
}
