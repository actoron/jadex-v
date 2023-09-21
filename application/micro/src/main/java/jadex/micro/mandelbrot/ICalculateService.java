package jadex.micro.mandelbrot;

import jadex.future.IFuture;

@FunctionalInterface
public interface ICalculateService
{
	/**
	 *  Calculate the iteration depth for a fractal function.
	 */
	public IFuture<Integer> calculateArea(double x, double y, int depth);
}
