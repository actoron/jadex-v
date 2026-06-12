package jadex.launcher;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import jadex.core.Application;
import jadex.core.IComponentListener;
import jadex.core.IComponentManager;
import jadex.core.IComponentManager.ComponentEventType;
import jadex.future.Future;
import jadex.launcher.ApplicationLauncher.ApplicationConfig;

public class ApplicationLauncherTest
{
	static final long	TIMEOUT = 10000;
	
	/**
	 * 	Check if configs derived from readmes are correct/complete.
	 */
	@Test
	public void checkConfigs()
	{
		ApplicationLauncher.scanForApplications()
			.forEach(app ->
		{
			// Check that description and main class are found.
			assertNotNull(app.description(), "Missing description: "+app.name());
			assertNotNull(app.mainClass(), "Missing main class: "+app.name());
			
			// Check if the icon resource can be found, if specified.
			if(app.icon()!=null)
				assertNotNull(ApplicationLauncher.class.getResource(app.icon()), "Missing icon: "+app.name()+", icon: "+app.icon());
		});
	}

	/**
	 *  Check if the example applications can be launched.
	 */
	@Test
	public void testStartStop() throws Exception
	{
		for(ApplicationConfig config: ApplicationLauncher.scanForApplications())
		{
			Future<Application>	appfut	= new Future<>();
			Future<Void>	donefut	= new Future<>();
			IComponentManager.get().addComponentListener(new IComponentListener() 
			{
				@Override
				public void applicationAdded(Application app) 
				{
					appfut.setResultIfUndone(app);
				}
			}, ComponentEventType.APPLICATION_ADDED);
			
			new Thread(() ->
			{
				try
				{
					config.mainClass().getMethod("main", String[].class).invoke(null, (Object) new String[0]);
					donefut.setResult(null);
				}
				catch(Exception e)
				{
					appfut.setExceptionIfUndone(e);
				}
			}).start();
			
			Application	app	= appfut.get(TIMEOUT);
			// Give the app some time to start before termination.
			for(int i=0; i<5 && !donefut.isDone(); i++)
			{
				Thread.sleep(1000);
			}
			app.terminate().get(TIMEOUT);
		}
	}
}
