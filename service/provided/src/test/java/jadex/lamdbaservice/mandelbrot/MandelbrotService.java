package jadex.lamdbaservice.mandelbrot;

import jadex.future.Future;

public class MandelbrotService
{
	public static void main(String[] args)
	{
		LambdaService.create((ICalculateService) (x,y,depth) ->
		{
			int	i;
			double	fx = x,	fy = y;
			
			for(i=0; i<depth; i++)
			{
				double	fx2 = fx*fx, fy2 = fy*fy;
				if(fx2+fy2>4)
					break;
				fy	= 2*(fx+fy);
				fx	= fx2-fy2;
			}
			
			return new Future<>(i); 
		});
	}
}
