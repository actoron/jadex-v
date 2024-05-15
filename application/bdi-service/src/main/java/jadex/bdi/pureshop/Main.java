package jadex.bdi.pureshop;

import jadex.bdi.runtime.IBDIAgent;

/**
 *  Main for starting the example programmatically.
 *  
 *  To start the example via this Main.java Jadex platform 
 *  as well as examples must be in classpath.
 */
public class Main 
{
	/**
	 *  Start a platform and the example.
	 */
	public static void main(String[] args) 
	{
		IBDIAgent.create(new ShopAgent("Ladl", 
			java.util.Arrays.asList(new ItemInfo[]
			{
				new ItemInfo("Cookies", 0.89, 10),
				new ItemInfo("Milk", 0.56, 2),
				new ItemInfo("Bread", 1.09, 8),
				new ItemInfo("Meat", 2.89, 6)
			})));

		IBDIAgent.create(new ShopAgent("Herzie",
			java.util.Arrays.asList(new ItemInfo[]
			{
				new ItemInfo("Shoes", 55.99, 2),
				new ItemInfo("Shirt", 15.00, 10),
				new ItemInfo("Pants", 75.99, 15)
			})));

		IBDIAgent.create(new CustomerAgent());
	}
}
