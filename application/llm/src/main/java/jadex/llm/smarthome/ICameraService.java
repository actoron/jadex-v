package jadex.llm.smarthome;

import java.awt.image.RenderedImage;

import dev.langchain4j.agent.tool.Tool;
import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

/**
 *  This service provides access to the security camera images.
 */
@Service
public interface ICameraService
{
	//-------- tool methods, i.e. visible to the LLM --------
	
	/**
	 *  Get the current image from the security camera.
	 *  @return The current image, or null if no image is available.
	 */
	@Tool("Get the current image as seen by the camera, e.g., to check if you see intruders.")
	public IFuture<RenderedImage>	getCurrentImage();
	
	//-------- UI only methods --------
	
	/**
	 *  Set the current image.
	 *  @param prompt	The prompt is prefixed with "security camera image of " and used to generate the image.
	 *  				It is also used as a cache key, so the same prompt will return the same image.
	 */
	public IFuture<Void>	setCurrentImage(String prompt);
}
