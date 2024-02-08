package jadex.micro.mandelbrot_new.calculate;

import jadex.future.IIntermediateFuture;
import jadex.micro.mandelbrot_new.model.AreaData;
import jadex.micro.mandelbrot_new.model.PartDataChunk;
import jadex.providedservice.annotation.Security;
import jadex.providedservice.annotation.Service;
import jadex.providedservice.annotation.Timeout;

/**
 *  Interface for calculating an area of points.
 */
@Service
@Security(roles=Security.UNRESTRICTED)
public interface ICalculateService
{
	/**
	 *  Calculate colors for an area of points.
	 *  @param data	The area to be calculated.
	 *  @return	A future containing the calculated area.
	 * /
	@Timeout(30000)
	public IFuture<AreaData> calculateArea(AreaData data);*/
	
	/**
	 *  Calculate colors for an area of points.
	 *  @param data	The area to be calculated.
	 *  @return	A future containing the calculated area.
	 */
	//@Timeout(30000)
	public IIntermediateFuture<PartDataChunk> calculateArea(AreaData data);
}
