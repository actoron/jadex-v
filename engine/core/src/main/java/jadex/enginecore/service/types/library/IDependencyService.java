package jadex.enginecore.service.types.library;

import java.net.URL;
import java.util.List;
import java.util.Map;

import jadex.common.Tuple2;
import jadex.enginecore.IResourceIdentifier;
import jadex.enginecore.service.annotation.CheckNotNull;
import jadex.future.IFuture;

/**
 *  Service for resolving deployment artifact dependencies.
 */
public interface IDependencyService
{
	/**
	 *  Load dependencies from a resource identifier.
	 *  @param rid	A local or global resource identifier. If both local and global ids are present,
	 *    local takes precedence, e.g. resolving to workspace urls before fetching an older snapshot from a repository.
	 *  @return A map containing the dependencies as mapping (parent RID -> list of children RIDs).
	 */
	public IFuture<Tuple2<IResourceIdentifier, Map<IResourceIdentifier, List<IResourceIdentifier>>>>	
		loadDependencies(@CheckNotNull IResourceIdentifier rid, boolean workspace);
	
	/**
	 *  Get the resource identifier for an url.
	 *  @param url The url.
	 *  @return The resource identifier.
	 */
	public IFuture<IResourceIdentifier> getResourceIdentifier(@CheckNotNull URL url);

}
