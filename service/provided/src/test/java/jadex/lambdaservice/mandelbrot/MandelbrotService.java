package jadex.lambdaservice.mandelbrot;

import org.junit.jupiter.api.Test;

import jadex.future.Future;

public class MandelbrotService
{
	@Test
	public void testSyntax()
	{
		// Just test if this compiles -> create() is currently nop
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
