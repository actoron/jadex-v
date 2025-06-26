package jadex.micro.helpline;

import jadex.future.IIntermediateFuture;
import jadex.providedservice.annotation.Service;

/**
 *  Basic interface for helpline.
 *  Allows to get local information about a person and
 *  add information about a person.
 */
//@Security(roles=Security.UNRESTRICTED)
@Service
public interface IHelpline
{
	/**
	 *  Get all locally stored information about a person.
	 *  @param name The person's name.
	 *  @return Future that contains all information records as collection.
	 */
	public IIntermediateFuture<InformationEntry> getInformation(String name);
	
	/**
	 *  Add an information about a person.
	 *  @param name The person's name.
	 *  @param info The information.
	 */
	public void addInformation(String name, String info);
}
