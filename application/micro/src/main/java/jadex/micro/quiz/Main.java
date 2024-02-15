package jadex.micro.quiz;

import jadex.core.IComponent;
import jadex.core.IExternalAccess;
import jadex.providedservice.ServiceScope;

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
		IComponent.create(new QuizMasterAgent());
		
		IComponent.create(new QuizClientAgent());
		
		/**IExternalAccess platform = Starter.createPlatform(PlatformConfigurationHandler.getDefaultNoGui()).get();
		platform.createComponent(new CreationInfo()
			.setFilenameClass(QuizMasterAgent.class)
			.addArgument("scope", ServiceScope.GLOBAL)).get();
		platform.createComponent(new CreationInfo()
			.setFilenameClass(QuizClientAgent.class)
			.addArgument("scope", ServiceScope.GLOBAL)).get();*/
	}
}
