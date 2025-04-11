package jadex.bdi.university;

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
		IComponentManager.get().create(new UniversityAgent(true, false)).get();
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}
