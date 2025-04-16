package jadex.bdi.blocksworld;

import java.awt.Color;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import jadex.bdi.IPlan;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.blocksworld.BlocksworldAgent.ConfigureGoal;
import jadex.bdi.blocksworld.BlocksworldAgent.StackGoal;
import jadex.injection.annotation.Inject;

/**
 *  Stack blocks according to the target configuration.
 */
//@Plan
public class ConfigureBlocksPlan
{
	//-------- attributes --------

	@Inject
	protected BlocksworldAgent capa;
	
	@Inject
	protected IPlan rplan;
	
	@Inject
	protected ConfigureGoal goal;
	
	//-------- methods --------

	/**
	 *  The plan body.
	 */
	@PlanBody
	public void body()
	{
		try
		{
		Table configuration	= goal.getConfiguration();
//		getParameterSet("blocks").addValues(configuration.getAllBlocks());
		
		// Create set of blocks currently on the table.
		Table table = capa.getTable();
		Block[]	blocks	= table.getAllBlocks();
		Set<Block>	oldblocks	= new HashSet<Block>();
		for(int i=0; i<blocks.length; i++)
			oldblocks.add(blocks[i]);
		blocks = (Block[])capa.getBlocks().toArray(new Block[0]);

		// Move all blocks in configuration to desired target location.
		Block[][]	stacks	= configuration.getStacks();
		for(int i=0; i<stacks.length; i++)
		{
			for(int j=0; j<stacks[i].length; j++)
			{
				// Get blocks from beliefs.
				Color	blockcolor	= stacks[i][j].getColor();
				Color	targetcolor	= stacks[i][j].getLower().getColor();
				Block	block	= null;
				Block	target	= stacks[i][j].getLower()==configuration ? table : null;
				for(int k=0; (block==null || target==null) && k<blocks.length; k++)
				{
					if(block==null && blocks[k].getColor().equals(blockcolor))
					{
						block	= blocks[k];
					}
					if(target==null && blocks[k].getColor().equals(targetcolor))
					{
						target	= blocks[k];
					}
				}
				if(block==null || target==null)
					throw new RuntimeException("No such blocks: "+stacks[i][j]+", "+stacks[i][j].getLower());
	
				// Create stack goal.
				StackGoal stack = capa.new StackGoal(block, target);
				rplan.dispatchSubgoal(stack).get();
	
				// Remove processed block from oldblocks.
				oldblocks.remove(block);
			}
		}

		// Move old blocks, which are not part of configuration, to bucket.
		Table	bucket	= capa.getBucket();
		for(Iterator<Block> i=oldblocks.iterator(); i.hasNext(); )
		{
			// Create stack goal.
			StackGoal stack = capa.new StackGoal(i.next(), bucket);
			try
			{
			rplan.dispatchSubgoal(stack).get();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
}
