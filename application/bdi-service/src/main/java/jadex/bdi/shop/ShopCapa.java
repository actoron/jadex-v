package jadex.bdi.shop;

import java.util.List;
import java.util.function.Supplier;

import jadex.bdi.Val;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Trigger;
import jadex.providedservice.annotation.ProvideService;

/**
 * 
 */
//@Capability
public class ShopCapa
{
	@Belief
	protected Val<Double>	money	= new Val<>(100.0);
	
	/** The shop name. */
	protected String shopname;
	
	/** The shop catalog. */
	@Belief
	protected List<ItemInfo> catalog;
	
	@ProvideService
	protected IShopService	shopserv;
	
	/**
	 *  Create a shop capability.
	 */
	public ShopCapa(String shopname, List<ItemInfo> catalog)
	{
		this.shopname	= shopname;
		this.catalog	= catalog;
		this.shopserv	= new ShopService(shopname);
	}
	
	/**
	 *  Get the shop name.
	 */
	public String	getShopname()
	{
		return shopname;
	}
	
	/**
	 *  Get the catalog.
	 */
	public List<ItemInfo>	getCatalog()
	{
		return catalog;
	}
	
	@Goal
	public class SellGoal	implements Supplier<ItemInfo>
	{
		/** The text. */
		protected String name;
		
		/** The price. */
		protected double price;
		
		/** The result. */
		protected ItemInfo result;

		/**
		 *  Create a new SellGoal. 
		 */
		public SellGoal(String name, double price)
		{
			this.name = name;
			this.price = price;
		}

		/**
		 *  Get the name.
		 *  @return The name.
		 */
		public String getName()
		{
			return name;
		}

		/**
		 *  Get the price.
		 *  @return The price.
		 */
		public double getPrice()
		{
			return price;
		}

		/**
		 *  Get the result.
		 *  @return The result.
		 */
		@Override
		public ItemInfo get()
		{
			return result;
		}

		/**
		 *  Set the result.
		 *  @param result The result to set.
		 */
		public void setResult(ItemInfo result)
		{
			this.result = result;
		}
	}
	
	/**
	 *  Plan for handling a sell goal.
	 *  @param goal The goal.
	 */
	@Plan(trigger=@Trigger(goals=SellGoal.class))
	public void sell(SellGoal goal)
	{
		ItemInfo tst = new ItemInfo(goal.getName());
		ItemInfo ii = null;
		int pos = 0;
		for(; pos<catalog.size(); pos++)
		{
			ItemInfo tmp = catalog.get(pos);
			if(tmp.equals(tst))
			{
				ii = tmp;
				break;
			}
		}
		
		// Check if enough money is given and it is in stock.
		if(ii==null || ii.getQuantity()==0)
		{
			throw new RuntimeException("Item not in store: "+goal.getName());
		}
		else if(ii.getQuantity()>0 && ii.getPrice()<=goal.getPrice())
		{
			// Sell item by updating catalog and account
////		System.out.println(getComponentName()+" sell item: "+name+" for: "+price);
			ii.setQuantity(ii.getQuantity()-1);
			goal.setResult(new ItemInfo(goal.getName(), ii.getPrice(), 1));
//			getBeliefbase().getBeliefSet("catalog").modified(ii);
			catalog.set(pos, ii);
			
			money.set(money.get()+goal.getPrice());
		}
		else
		{
			throw new RuntimeException("Payment not sufficient: "+goal.getPrice());
		}
	}
}
