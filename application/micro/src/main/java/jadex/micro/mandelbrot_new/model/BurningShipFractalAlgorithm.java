package jadex.micro.mandelbrot_new.model;

import jadex.common.ClassInfo;

/**
 *  Algorithm for calculating the mandelbrot set.
 */
public class BurningShipFractalAlgorithm implements IFractalAlgorithm
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
	 *  Can areas be filled?
	 */
	public boolean isOptimizationAllowed()
	{
		return true;
	}

	
	/**
	 *  Get default settings for rendering the fractal. 
	 */
	public AreaData	getDefaultSettings()
	{
		return new AreaData(-2, 1, -2, 1, 300, 300).setMax((short)256).setTaskSize(300).setAlgorithmClass(new ClassInfo(BurningShipFractalAlgorithm.class)).setChunkCount(4);
	}
	
	/**
	 *  Should a cyclic color scheme be used?
	 */
	public boolean useColorCycle()
	{
		return true;
	}
	
	//-------- singleton semantics --------
	
	/**
	 *  Get a string representation.
	 */
	public String toString()
	{
		return "BurningShipFractal";
	}
	
	/**
	 *  Test if two objects are equal.
	 */
	public boolean equals(Object obj)
	{
		return obj instanceof BurningShipFractalAlgorithm;
	}
	
	/**
	 *  Get the hash code.
	 */
	public int hashCode()
	{
		return 31 + getClass().hashCode();
	}

}
