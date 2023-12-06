package jadex.bdi.puzzle;

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
		IBDIAgent.create("jadex.bdi.puzzle.SokratesMLRAgent");
//		IBDIAgent.create("jadex.bdi.puzzle.SokratesV3Agent");
	}
}
