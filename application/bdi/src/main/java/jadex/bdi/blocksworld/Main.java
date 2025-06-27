package jadex.bdi.blocksworld;

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
		IComponentManager.get().create(new BlocksworldAgent()).get();
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}
