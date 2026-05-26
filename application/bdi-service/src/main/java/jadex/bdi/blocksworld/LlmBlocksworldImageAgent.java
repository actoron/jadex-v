package jadex.bdi.blocksworld;

import java.awt.Component;
import java.awt.Container;
import java.awt.image.RenderedImage;

import jadex.future.IFuture;
import jadex.micro.llmcall2.LlmHelper;

/**
 *  Interact with a blocksworld via natural language prompts.
 *  Only image-based world state representation.
 */
public class LlmBlocksworldImageAgent	extends LlmBlocksworldBaseAgent	implements IBlocksworldImageService
{
	@Override
	public IFuture<RenderedImage> getWorldState()
	{
		return gui.thenApply(gui -> 
		{
			// Generate base64 encoded image of current world state
			Container	worlds	= (Container)gui.getContentPane().getComponent(0);
			Component	world	= 	((Container) ((Container) ((Container) worlds.getComponent(1)).getComponent(0)).getComponent(0)).getComponent(0);
			return LlmHelper.createImageFromComponent(world);
		});
	}
	
	/**
	 *  Start the agent.
	 */
	public static void main(String[] args)
	{
		runGui(new LlmBlocksworldImageAgent());
	}
}
