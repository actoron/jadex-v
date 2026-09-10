package jadex.bdi.blocksworld;

import java.awt.image.RenderedImage;

import dev.langchain4j.agent.tool.Tool;
import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

/**
 *  A service providing tools to interact with the blocksworld environment.
 *  Uses image-based world state representation.
 */
@Service
public interface IBlocksworldImageService	extends IBlocksworldBaseService
{
	/**
	 *  Get the current state of the world.
	 *  @return The world state.
	 */
	@Tool("Get all information about existing blocks.")
	public IFuture<RenderedImage> getWorldState();
}
