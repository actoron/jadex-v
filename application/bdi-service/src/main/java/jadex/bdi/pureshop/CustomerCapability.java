package jadex.bdi.pureshop;

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Capability;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Trigger;
import jadex.bdi.runtime.ICapability;
import jadex.bdi.runtime.Val;
import jadex.core.IComponent;
import jadex.future.IFuture;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;

/**
 *  Customer capability.
 */
@Capability
//TODO
//@RequiredServices({
//	@RequiredService(name="localshopservices", type=IShopService.class, scope=ServiceScope.PLATFORM), //multiple=true,
//	@RequiredService(name="remoteshopservices", type=IShopService.class, scope=ServiceScope.GLOBAL), // multiple=true,
//})
public class CustomerCapability
{
	//-------- attributes --------

	/** The agent. */
	@Agent
	protected IComponent agent;
	
	/** The capability. */
	@Agent
	protected ICapability capa;
	
	/** The inventory. */
	@Belief
	protected List<ItemInfo> inventory = new ArrayList<ItemInfo>();
	
	@Belief
	protected Val<Double>	money;	// Abstract belief -> value is injected automatically
	
	//-------- methods --------
	
	/**
	 *  Called when the agent is started.
	 */
	@OnStart
	public void start()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				new CustomerFrame(agent, capa);
			}
		});
	}
	
	//-------- goals --------
	
	/**
	 *  Goal to buy an item.
	 */
	@Goal
	public static class BuyItem
	{
		//-------- attributes --------
		
		/** The item name. */
		public String	name;
		
		/** The shop. */
		public IShopService	shop;
		
		/** The price. */
		public double	price; 
		
		//-------- constructors --------

		/**
		 *  Create a buy item goal.
		 */
		public BuyItem(String name, IShopService shop, double price)
		{
			this.name	= name;
			this.shop	= shop;
			this.price	= price;
		}
	}
	
	//-------- plans --------
	
	/**
	 *  Plan for buying an item.
	 */
	@Plan(trigger=@Trigger(goals=BuyItem.class))
	public void	buyItem(BuyItem big)
	{
		// Check if enough money to buy the item
		if(money.get()<big.price)
			throw new RuntimeException("Not enough money to buy: "+big.name);
		
		// Buy the item at the shop (the shop is a service at another agent)
		System.out.println(agent.getId().getLocalName()+" buying item: "+big.name);
		IFuture<ItemInfo>	future	= big.shop.buyItem(big.name, big.price);
		System.out.println(agent.getId().getLocalName()+" getting item: "+future);
		ItemInfo item = (ItemInfo)future.get();
		System.out.println(agent.getId().getLocalName()+" bought item: "+item);
		
		// Update the customer inventory 
		ItemInfo ii = null;
		for(ItemInfo test: inventory)
		{
			if(test.equals(item))
			{
				ii	= test;
				break;
			}
		}
		if(ii==null)
		{
			ii = new ItemInfo(big.name, big.price, 1);
			inventory.add(ii);
		}
		else
		{
			ii.setQuantity(ii.getQuantity()+1);
			// Hack!!! Should use beliefModified()?
			int	index	= inventory.indexOf(ii);
			inventory.set(index, ii);
		}
		
		// Update the account
		money.set(money.get() - big.price);
	}

	/**
	 * @return the capa
	 */
	public ICapability getCapability()
	{
		return capa;
	}
}
