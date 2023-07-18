package jadex.enginecore.service.types.cli;

import jadex.common.Tuple2;
import jadex.enginecore.service.annotation.CheckNotNull;
import jadex.enginecore.service.annotation.Service;
import jadex.future.IFuture;

/**
 *  Service to invoke the command line via a service call.
 */
@Service(system=true)
public interface ICliService
{
	/**
	 *  Execute a command line command and
	 *  get back the results.
	 *  @param command The command.
	 *  @return The result of the command.
	 */
	public IFuture<String> executeCommand(@CheckNotNull String command, 
		@CheckNotNull Tuple2<String, Integer> session);
}
