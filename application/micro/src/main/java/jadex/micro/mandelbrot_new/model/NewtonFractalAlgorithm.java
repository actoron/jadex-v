package jadex.micro.mandelbrot_new.model;

import jadex.common.ClassInfo;

public class NewtonFractalAlgorithm extends AbstractFractalAlgorithm
{
	public static final double EPSILON = 0.001;
	
	public short determineColor(double x, double y, short max) 
	{
        double zr = x;
        double zi = y;

        short iterations = 0;
        while (iterations < max) 
        {
            // Newton's method iteration formula for z^3 - 1 = 0
            /*double zrSquared = zr * zr;
            double ziSquared = zi * zi;
            double zrCubed = zrSquared * zr - 3 * zr * ziSquared;
            double ziCubed = 3 * zrSquared * zi - ziSquared * zi;
            double denominator = 3 * (zrSquared + ziSquared);*/
            
            // Newton's method iteration formula for z^3 - 2z + 2 = 0
            double zrSquared = zr * zr;
            double ziSquared = zi * zi;
            double zrCubed = zr * (zrSquared - 3 * ziSquared) + 2;
            double ziCubed = zi * (3 * zrSquared - ziSquared);

            double denominator = 3 * (zrSquared + ziSquared);
            
            if(denominator == 0) 
                break; 

            double deltaZr = zrCubed / denominator;
            double deltaZi = ziCubed / denominator;

            zr -= deltaZr;
            zi -= deltaZi;

            if (Math.abs(deltaZr) < EPSILON && Math.abs(deltaZi) < EPSILON) 
                return iterations;

            iterations++;
        }

        return -1;
    }
	
	/**
	 *  Get default settings for rendering the fractal. 
	 */
	public AreaData	getDefaultSettings()
	{
		return new AreaData(-2, 2, -2, 2, 300, 300).setMax((short)256).setTaskSize(300).setAlgorithmClass(new ClassInfo(this.getClass())).setChunkCount(4);
	}
}
