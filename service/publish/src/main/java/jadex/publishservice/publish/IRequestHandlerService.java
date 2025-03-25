package jadex.publishservice.publish;

import jadex.core.annotation.NoCopy;
import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 *  Interface for handling http requests.
 */
@Service
public interface IRequestHandlerService
{
	/**
	 *  Handle the request.
	 *  @param request The request.
	 *  @param response The response.
	 *  @param args Container specific args.
	 */
	public IFuture<Void> handleRequest(@NoCopy HttpServletRequest request, @NoCopy HttpServletResponse response, @NoCopy Object args);
}
