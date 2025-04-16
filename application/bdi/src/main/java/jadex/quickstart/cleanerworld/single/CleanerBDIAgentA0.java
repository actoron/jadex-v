package jadex.quickstart.cleanerworld.single;

import jadex.bdi.annotation.BDIAgent;
import jadex.core.IComponentManager;
import jadex.injection.annotation.OnStart;
import jadex.quickstart.cleanerworld.environment.SensorActuator;
import jadex.quickstart.cleanerworld.gui.EnvironmentGui;
import jadex.quickstart.cleanerworld.gui.SensorGui;


/**
 *  BDI agent template.
 */
@BDIAgent    // This annotation enabled BDI features
public class CleanerBDIAgentA0
{
    //-------- fields holding agent data --------
    
    /** The sensor/actuator object gives access to the environment of the cleaner robot. */
    private SensorActuator    actsense    = new SensorActuator();
    
    //... add more field here
    
    //-------- setup code --------
    
    /**
     *  The body is executed when the agent is started.
     *  @param bdifeature    Provides access to bdi specific methods
     */
    @OnStart    // This annotation informs the Jadex platform to call this method once the agent is started
    private void    exampleBehavior()
    {
        // Open a window showing the agent's perceptions
        new SensorGui(actsense).setVisible(true);
        
        //... add more setup code here
        actsense.moveTo(Math.random(), Math.random());    // Dummy call so that the cleaner moves a little.
    }

    //-------- additional BDI agent code --------
    
    //... BDI goals and plans will be added here


	/**
	 *  Main method for starting the scenario.
	 *  @param args	ignored for now.
	 */
	public static void main(String[] args)
	{
		// Start an agent
		IComponentManager.get().create(new CleanerBDIAgentA0());
		
		// Open the world view
		EnvironmentGui.create();
	}
}
