package jadex.micro.mandelbrot_new.generate;

import jadex.future.IFuture;
import jadex.micro.mandelbrot_new.model.AreaData;
import jadex.providedservice.annotation.Service;

/**
 *  Service for generating a specific area.
 */
@Service
public interface IGenerateService
{
	/**
	 *  Generate a specific area using a defined x and y size.
	 */
	public IFuture<Void> generateArea(AreaData data);
	//public IFuture<AreaData> generateArea(AreaData data);
	
	/**
	 *  Calculate and display the default image from current settings.
	 * /
	public IFuture<Void> calcDefaultImage();*/
}
