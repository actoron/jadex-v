package jadex.quickstart.cleanerworld;

import javax.swing.SwingUtilities;

import jadex.mj.micro.MjMicroAgent;
import jadex.quickstart.cleanerworld.gui.EnvironmentGui;

/**
 *  Main class for starting a cleaner-world scenario
 */
public class Main
{
	/** Use higher values (e.g. 2.0) for faster cleaner movement and lower values (e.g. 0.5) for slower movement. */
	protected static double	CLOCK_SPEED	= 1;
	
	/**
	 *  Main method for starting the scenario.
	 *  @param args	ignored for now.
	 */
	public static void main(String[] args)
	{
		MjMicroAgent.create(new SimpleCleanerAgentZero());
		
		// Open world view window on Swing Thread
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				new EnvironmentGui().setVisible(true);
			}
		});
	}
}
