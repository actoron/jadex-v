package jadex.launcher;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class ApplicationLauncherTest
{
	/**
	 * 	Check if main classes and descriptions are found.
	 */
	@Test
	public void testMainClasses()
	{
		ApplicationLauncher.scanForApplications()
			.forEach(app ->
		{
			assertNotNull(app.description(), app.name());
			assertNotNull(app.mainClass(), app.name());
		});
	}
}
