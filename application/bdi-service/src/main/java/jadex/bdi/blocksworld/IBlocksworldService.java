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
//	@Tool("Get the current state of the world.")
	@Tool("Get all information about existing blocks.")
	public IFuture<String> getWorldState();
	
	/**
	 *  Move a block on top of another block.
	 */
//	@Tool("Move a block on top of another block.")
	@Tool("Move a block on top of another block, table or into the bucket.\n"
		+ "Blocks are referred to by their name and number, e.g. 'Block 1', and not by their color.\n"
		+ "Check the world state to find out which blocks exist, how they are named, and where they are located.\n"
		+ "Both blocks must be clear, i.e. no block on top of them.\n"
		+ "The table and bucket are always clear to put blocks on/into.")
	public IFuture<Void> move(String block1, String block2);
}
