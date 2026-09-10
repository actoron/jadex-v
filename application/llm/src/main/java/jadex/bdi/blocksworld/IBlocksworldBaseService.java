package jadex.bdi.blocksworld;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jadex.future.IFuture;

/**
 *  A service providing tools to interact with the blocksworld environment.
 *  Base service for common tools.
 */
public interface IBlocksworldBaseService
{
	/**
	 *  Move a block on top of another block.
	 */
	@Tool("# Purpose\n"
		+ "Move a block on top of another block, table or into the bucket.\n"
		+ "## Usage"
		+ "1. Blocks are referred to by their name and number, e.g. 'Block 1', and not by their color.\n"
		+ "2. Check the world state to find out which blocks exist, how they are named, and where they are located.\n"
		+ "## Constraints\n"
		+ "1. Both blocks must be clear, i.e. no block on top of them.\n"
		+ "2. The table and bucket are always clear targets to put more blocks on/into.")
	public IFuture<Void> move(
		@P("The block to move") String block1,
		@P("The target block, table or bucket") String block2);
}
