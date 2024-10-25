package jadex.bt.booktrading;


import java.lang.System.Logger.Level;

import jadex.bt.booktrading.domain.Order;
import jadex.core.IComponent;
import jadex.logger.JadexLoggerFinder;

/**
 *  Main for starting the example programmatically.
 */
public class Main 
{
	/**
	 *  Start a buyer and seller.
	 */
	public static void main(String[] args) 
	{
		//JadexLoggerFinder.setDefaultSystemLoggingLevel(Level.INFO);
		
		IComponent.create(new BuyerAgent(
			new Order[]
			{
				new Order("All about agents", 60000, 100, 120, true),
				new Order("All about web services", 60000, 40, 60, true),
				new Order("Harry Potter", 60000, 5, 10, true),
				new Order("Agents in the real world", 60000, 30, 65, true)
			}));

		IComponent.create(new SellerAgent(
			new Order[]
			{
				new Order("All about agents", 60000, 130, 110, false),
				new Order("All about web services", 60000, 50, 30, false),
				new Order("Harry Potter", 60000, 15, 9, false),
				new Order("Agents in the real world", 60000, 100, 60, false)
			}));
	}
}
