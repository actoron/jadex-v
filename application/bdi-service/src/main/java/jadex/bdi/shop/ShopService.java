package jadex.bdi.shop;

import jadex.bdi.runtime.IBDIAgentFeature;
import jadex.bdi.runtime.ICapability;
import jadex.bdi.shop.ShopCapa.SellGoal;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;
import jadex.providedservice.annotation.ServiceComponent;
import jadex.providedservice.impl.service.ServiceCall;

/**
 *  The shop for buying goods at the shop.
 */
@Service
public class ShopService implements IShopService 
{
	//-------- attributes --------
	
	/** The component. */
	@ServiceComponent
	protected IComponent	agent;
	
	/** The component. */
	@ServiceComponent
	protected ICapability	capa;
	
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
	 *  
	 *  @directcall (Is called on caller thread).
	 */
	public String getName()
	{
		return name;
	}
	
	/**
	 *  Buy an item.
	 *  @param item The item.
	 */
	public IFuture<ItemInfo> buyItem(final String item, final double price)
	{
		System.out.println("buyItem in ShopService: "+ServiceCall.getCurrentInvocation().getCaller());
		
		ShopCapa shop = (ShopCapa)capa.getPojoCapability();
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
		ShopCapa shop = (ShopCapa)capa.getPojoCapability();
		ret.setResult(shop.getCatalog().toArray(new ItemInfo[shop.getCatalog().size()]));
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
