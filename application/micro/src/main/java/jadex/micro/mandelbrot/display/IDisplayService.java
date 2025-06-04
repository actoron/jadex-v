package jadex.micro.mandelbrot.display;

import java.util.List;

import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.micro.mandelbrot.model.AreaData;
import jadex.micro.mandelbrot.model.IFractalAlgorithm;
import jadex.micro.mandelbrot.model.PartDataChunk;
import jadex.micro.mandelbrot.model.ProgressData;
import jadex.providedservice.annotation.Service;

/**
 *  Service for displaying the result of a calculation. 
 */
@Service
//@Security(roles=Security.UNRESTRICTED)
public interface IDisplayService
{
	/**
	 *  Display the result of a calculation.
	 */
	public IFuture<Void> displayResult(AreaData result);

	/**
	 *  Display intermediate calculation results.
	 */
	public IFuture<Void> displayIntermediateResult(ProgressData progress);
	
	/**
	 *  Display intermediate calculation results.
	 */
	public IFuture<Void> displayIntermediateResult(PartDataChunk progress);
	
	/**
	 *  Subscribe to display events.
	 *  Can receive AreaData, ProgressData or PartDataChunk
	 */
//	@Timeout(Timeout.NONE)
	public ISubscriptionIntermediateFuture<Object> subscribeToDisplayUpdates(String displayid);
	
	/**
	 *  Get info about an algorithm (for web). todo: move?!
	 *  @return The info.
	 */
	public IFuture<AreaData> getAlgorithmDefaultSettings(Class<IFractalAlgorithm> clazz);
	
	/**
	 *  Get available algorithms.
	 *  @return The algos.
	 */
	public IFuture<List<Class<IFractalAlgorithm>>> getAlgorithms();
}
