package jadex.remoteservices.impl.remotecommands;

import jadex.core.ComponentIdentifier;

import java.util.Map;

/**
 *  Base class for Jadex built-in remote commands.
 *  Handles exchange of non-functional properties.
 */
public abstract class AbstractInternalRemoteCommand extends AbstractIdSenderCommand
{
	//-------- attributes ---------
	
	/** The non-functional properties. */
	private Map<String, Object>	nonfunc;
	
	//-------- constructors --------
	
	/**
	 *  Create a remote command.
	 */
	public AbstractInternalRemoteCommand()
	{
		// Bean constructor.
	}
	
	/**
	 *  Create a remote command.
	 */
	public AbstractInternalRemoteCommand(String id,  ComponentIdentifier sender, Map<String, Object> nonfunc)
	{
		setId(id);
		setSender(sender);
		this.nonfunc	= nonfunc;
	}

	//-------- bean property methods --------

	/**
	 *  Get the non-functional properties.
	 */
	public Map<String, Object>	getProperties()
	{
		return nonfunc;
	}
	
	/**
	 *  Set the non-functional properties.
	 */
	public void	setProperties(Map<String, Object> nonfunc)
	{
		this.nonfunc	= nonfunc;
	}	
}
