package jadex.bdi.blocksworld;

import jadex.bdi.runtime.IBDIAgent;
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
		IBDIAgent.create("jadex.bdi.blocksworld.BlocksworldAgent");
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}
