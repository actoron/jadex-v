package jadex.micro.quiz;

import jadex.core.IComponentManager;

/**
 *  Main for starting the application.
 */
public class Main
{
	/**
	 *  Main method.
	 */
	public static void main(String[] args)
	{
		IComponentManager.get().create(new QuizMasterAgent());
		
		IComponentManager.get().create(new QuizClientAgent());
		
		IComponentManager.get().waitForLastComponentTerminated();
		
		/**IExternalAccess platform = Starter.createPlatform(PlatformConfigurationHandler.getDefaultNoGui()).get();
		platform.createComponent(new CreationInfo()
			.setFilenameClass(QuizMasterAgent.class)
			.addArgument("scope", ServiceScope.GLOBAL)).get();
		platform.createComponent(new CreationInfo()
			.setFilenameClass(QuizClientAgent.class)
			.addArgument("scope", ServiceScope.GLOBAL)).get();*/
	}
}
