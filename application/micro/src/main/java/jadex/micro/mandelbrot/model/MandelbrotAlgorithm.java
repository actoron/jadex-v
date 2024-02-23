package jadex.micro.mandelbrot.model;

import jadex.common.ClassInfo;

/**
 *  Algorithm for calculating the mandelbrot set.
 */
public class MandelbrotAlgorithm extends AbstractFractalAlgorithm
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
		double x0 = xn;
		double y0 = yn;
		short i = 0;
		double c =  Math.sqrt(xn*xn + yn*yn);
		
		for(i=0; c<2 && i<max; i++)
		{
			double xn1 = xn*xn - yn*yn + x0;
			double yn1 = 2*xn*yn + y0;
			xn = xn1;
			yn = yn1;
			c =  Math.sqrt(xn*xn + yn*yn);
		}
		
		return i==max? -1: i;
	}
	
	/**
	 *  Get default settings for rendering the fractal. 
	 */
	public AreaData	getDefaultSettings()
	{
		//return new AreaData(-2, 1, -1.5, 1.5, 100, 100, (short)256, 10, 300, this, null);
//		return new AreaData(-2, 1, -1.5, 1.5, 100, 100, (short)256, 300, this, null, 20);
//		return new AreaData(-2, 1, -1.5, 1.5, 300, 300, (short)256, 300, new ClassInfo(MandelbrotAlgorithm.class), null, 4);
		return new AreaData(-2, 1, -1.5, 1.5, 300, 300).setMax((short)256).setTaskSize(300).setAlgorithmClass(new ClassInfo(MandelbrotAlgorithm.class)).setChunkCount(4);

	}
	
	/**
	 *  The default algorithm.
	 */
	public boolean isDefault()
	{
		return true;
	}
}
