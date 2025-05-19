package jadex.benchmark;

import java.lang.System.Logger.Level;

import jadex.core.IComponentManager;
import jadex.logger.ILoggingFeature;

public class BaseTest
{
	// Hack!!! depends on which test is executed first so all tests should extend this
	static
	{
		IComponentManager.get().getFeature(ILoggingFeature.class).setDefaultSystemLoggingLevel(Level.ERROR);
		IComponentManager.get().getFeature(ILoggingFeature.class).setDefaultAppLoggingLevel(Level.ERROR);
	}
}
