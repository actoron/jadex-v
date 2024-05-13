package jadex.micro.mandelbrot.model;

import jadex.common.ClassInfo;

/**
 *  Algorithm for calculating the mandelbrot set.
 */
public class BurningShipFractalAlgorithm extends AbstractFractalAlgorithm
{
	//-------- IFractalAlgorithm interface --------
	
	/**
	 *  Determine the color of a point.
	 *  @param x	The x coordinate.
	 *  @param y	The y coordinate.
	 *  @param max	The maximum depth.
	 *  @return	A value for the point from 0 to max.
	 */
	public short determineColor(double xn, double yn, short max)
	{
		double zx = 0;
	    double zy = 0;
	    double cX = xn;
	    double cY = yn;

	    short i = 0;
	    while (zx * zx + zy * zy < 4 && i < max) 
	    {
	    	double tmp = zx * zx - zy * zy + cX;
	        zy = Math.abs(2.0 * zx * zy) + cY;
	        zx = tmp;
	        i++;
	    }

	    return i==max? -1: i;
	}
	
	/**
	 *  Get default settings for rendering the fractal. 
	 */
	public AreaData	getDefaultSettings()
	{
		return new AreaData(-2, 1, -2, 1, 300, 300).setMax((short)256).setTaskSize(300).setAlgorithmClass(new ClassInfo(BurningShipFractalAlgorithm.class)).setChunkCount(4);
	}
}
