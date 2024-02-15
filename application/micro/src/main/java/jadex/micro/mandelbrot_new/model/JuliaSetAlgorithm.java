package jadex.micro.mandelbrot_new.model;

import jadex.common.ClassInfo;

public class JuliaSetAlgorithm extends AbstractFractalAlgorithm
{

	public short determineColor(double xn, double yn, short max)
	{
	    double zx = xn;
	    double zy = yn;
	    double cx = -0.4;
	    double cy = 0.8;
 
        short i = 0;
        while(zx * zx + zy * zy < 4 && i < max) 
        {
            double tmp = zx * zx - zy * zy + cx;
            zy = 2.0 * zx * zy + cy;
            zx = tmp;
            i++;
        }

        return i == max ? -1 : i;
    }

	/**
	 *  Get default settings for rendering the fractal. 
	 */
	public AreaData	getDefaultSettings()
	{
		return new AreaData(-2, 2, -1.5, 1.5, 300, 300).setMax((short)256).setTaskSize(300).setAlgorithmClass(new ClassInfo(JuliaSetAlgorithm.class)).setChunkCount(4);
	}
}
