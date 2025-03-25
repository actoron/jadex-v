package jadex.micro.helpline;

import jadex.future.IIntermediateFuture;
import jadex.future.IntermediateFuture;
import jadex.injection.annotation.Inject;

/**
 *  Helpline service implementation.
 */
public class HelplineService implements IHelpline
{
	//-------- attributes --------
	
	/** The agent. */
	@Inject
	protected HelplineAgent agent;
	
	//-------- methods --------
	
	/**
	 *  Add an information about a person.
	 *  @param name The person's name.
	 *  @param info The information.
	 */
	public void addInformation(final String name, final String info)
	{
		agent.addInformation(name, info);
	}
	
	/**
	 *  Get all locally stored information about a person.
	 *  @param name The person's name.
	 *  @return Future that contains the information.
	 */
	public IIntermediateFuture<InformationEntry> getInformation(final String name)
	{
		return new IntermediateFuture<InformationEntry>(agent.getInformation(name));
	}

	/**
	 *  Get the string representation.
	 *  @return The string representation.
	 */
	public String toString()
	{
		return "HelplineService, "+agent.getAgent().getId();
	}
}
