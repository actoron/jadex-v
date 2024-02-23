package jadex.micro.mandelbrot;

import jadex.core.IComponent;
import jadex.micro.mandelbrot.calculate.CalculateAgent;
import jadex.micro.mandelbrot.display.DisplayWebAgent;
import jadex.micro.mandelbrot.generate.GenerateWebAgent;
import jadex.micro.mandelbrot.model.AreaData;
import jadex.micro.mandelbrot.model.PartDataChunk;
import jadex.micro.taskdistributor.IntermediateTaskDistributorAgent;

/**
 *  Main for starting the example programmatically.
 */
public class MainWeb
{
	/**
	 *  Start the example.
	 */
	public static void main(String[] args) 
	{
		//SUtil.DEBUG = true;

		IComponent.create(new IntermediateTaskDistributorAgent<PartDataChunk, AreaData>());
		IComponent.create(new GenerateWebAgent());
		IComponent.create(new DisplayWebAgent());
		
		int cores = Runtime.getRuntime().availableProcessors();
		System.out.println("creating calculators: "+(cores+1));
		for(int i=0; i<=cores; i++)
			IComponent.create(new CalculateAgent());
		
		IComponent.waitForLastComponentTerminated();
	}
}