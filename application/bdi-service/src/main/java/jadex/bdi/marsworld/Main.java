package jadex.bdi.marsworld;

import jadex.bdi.marsworld.carry.CarryAgent;
import jadex.bdi.marsworld.environment.Environment;
import jadex.bdi.marsworld.environment.Homebase;
import jadex.bdi.marsworld.environment.MarsworldEnvironment;
import jadex.bdi.marsworld.environment.Target;
import jadex.bdi.marsworld.math.Vector2Double;
import jadex.bdi.marsworld.producer.ProducerAgent;
import jadex.bdi.marsworld.sentry.SentryAgent;
import jadex.bdi.marsworld.ui.EnvGui;
import jadex.bdi.marsworld.ui.GoalViewer;
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
		MarsworldEnvironment env = IComponentManager.get().create(new MarsworldEnvironment(5)).get().getPojoHandle(MarsworldEnvironment.class);
		String id = Environment.add(env);
		//System.out.println("hash2: "+env.hashCode());
		
		env.addSpaceObject(new Homebase(new Vector2Double(0.3, 0.3), System.currentTimeMillis()+90000)).get();
		env.addSpaceObject(new Target(new Vector2Double(0.1, 0.2), 0)).get();
		env.addSpaceObject(new Target(new Vector2Double(0.05, 0.7), 200)).get();
		env.addSpaceObject(new Target(new Vector2Double(0.5, 0.6), 0)).get();
		env.addSpaceObject(new Target(new Vector2Double(0.8, 0.1), 50)).get();
		env.addSpaceObject(new Target(new Vector2Double(0.7, 0.4), 100)).get();
		env.addSpaceObject(new Target(new Vector2Double(0.8, 0.8), 25)).get();
		
		int ccnt = 1;
		int pcnt = 0;
		int scnt = 1;
		
		for(int i=0; i<ccnt; i++)
			IComponentManager.get().create(new CarryAgent(id)).get();
		
		for(int i=0; i<pcnt; i++)
			IComponentManager.get().create(new ProducerAgent(id)).get();

		for(int i=0; i<scnt; i++)
			IComponentManager.get().create(new SentryAgent(id)).get();

		EnvGui.createEnvGui(id);
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}
