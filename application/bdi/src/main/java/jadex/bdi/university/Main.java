package jadex.bdi.university;

import jadex.bdi.runtime.BDICreationInfo;
import jadex.bdi.runtime.IBDIAgent;
import jadex.core.IComponent;

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
//		IBDIAgent.create("jadex.bdi.university.UniversityAgent");
		IBDIAgent.create(new BDICreationInfo("jadex.bdi.university.UniversityAgent")
			.addArgument("raining", true)
			.addArgument("waiting", false));
		
		IComponent.waitForLastComponentTerminated();
	}
}
