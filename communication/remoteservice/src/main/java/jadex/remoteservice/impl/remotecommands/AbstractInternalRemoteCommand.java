package jadex.remoteservice.impl.remotecommands;

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
		super(null, null);
	}
	
	/**
	 *  Create a remote command.
	 */
	public AbstractInternalRemoteCommand(String id,  ComponentIdentifier sender, Map<String, Object> nonfunc)
	{
		super(id, sender);
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
