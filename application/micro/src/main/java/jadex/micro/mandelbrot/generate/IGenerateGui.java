package jadex.micro.mandelbrot.generate;

import jadex.micro.mandelbrot.model.AreaData;

/**
 *  Interface for generate gui.
 */
public interface IGenerateGui 
{
	/**
	 *  Update the area data.
	 *  @param data The data.
	 */
	public void updateData(AreaData data);
	
	/** 
	 * Update the status.
	 * @param cnt The cnt.
	 * @param number The number.
	 */
	public void updateStatus(int cnt, int number);
}
