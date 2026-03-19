package jadex.bdi.blocksworld;

import dev.langchain4j.agent.tool.Tool;
import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

@Service
public interface IBlocksworldService
{
	/**
	 *  Get the current state of the world.
	 *  @return The world state.
	 */
	@Tool("Get the current state of the world.")
	public IFuture<String> getWorldState();
	
	/**
	 *  Move a block on top of another block.
	 */
	@Tool("Move a block on top of another block.")
	public IFuture<Void> move(String block1, String block2);
}
