package jadex.micro.helpline;

import jadex.core.IComponentManager;

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
		// todo: how to use component clock?!
		long cur = System.currentTimeMillis();
		
		IComponentManager.get().create(new HelplineAgent(
			new InformationEntry[]
			{
				new InformationEntry("Lennie Lost", "First aid given at Hummel square.", cur-2*60*60*1000),
				new InformationEntry("Lennie Lost", "Brought to collection point.", cur-1*60*60*1000),
				new InformationEntry("Lennie Lost", "Savely reached Mainville Hospital.", cur)
			}));
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}
