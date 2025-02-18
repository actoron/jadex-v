package jadex.bdi.marsworld;

import jadex.bdi.marsworld.carry.CarryAgent;
import jadex.bdi.marsworld.environment.Homebase;
import jadex.bdi.marsworld.environment.MarsworldEnvironment;
import jadex.bdi.marsworld.environment.Target;
import jadex.bdi.marsworld.producer.ProducerAgent;
import jadex.bdi.marsworld.sentry.SentryAgent;
import jadex.bdi.marsworld.ui.EnvGui;
import jadex.core.IComponentManager;
import jadex.environment.Environment;
import jadex.math.Vector2Double;

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
		MarsworldEnvironment env = IComponentManager.get().create(new MarsworldEnvironment(5)).get().getPojoHandle(MarsworldEnvironment.class);
		String envid = Environment.add(env);
		//System.out.println("hash2: "+env.hashCode());
		
		env.addSpaceObject(new Homebase(new Vector2Double(0.3, 0.3), System.currentTimeMillis()+90000)).get();
		env.addSpaceObject(new Target(new Vector2Double(0.1, 0.2), 0)).get();
		env.addSpaceObject(new Target(new Vector2Double(0.05, 0.7), 200)).get();
		env.addSpaceObject(new Target(new Vector2Double(0.5, 0.6), 0)).get();
		env.addSpaceObject(new Target(new Vector2Double(0.8, 0.1), 50)).get();
		env.addSpaceObject(new Target(new Vector2Double(0.7, 0.4), 100)).get();
		env.addSpaceObject(new Target(new Vector2Double(0.8, 0.8), 25)).get();
		
		// Start minimal scenario
		startScenario(envid, 1, 1, 1);
		
		// Start small scenaio
		startScenario(envid, 1, 2, 3);
		
		// Start large scenario
		startScenario(envid, 2, 5, 10);

		EnvGui.createEnvGui(envid);
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
	
	protected static void startScenario(String envid, int scnt, int pcnt, int ccnt)
	{
		for(int i=0; i<ccnt; i++)
			IComponentManager.get().create(new CarryAgent(envid)).get();
		
		for(int i=0; i<pcnt; i++)
			IComponentManager.get().create(new ProducerAgent(envid)).get();

		for(int i=0; i<scnt; i++)
			IComponentManager.get().create(new SentryAgent(envid)).get();
	}
}
