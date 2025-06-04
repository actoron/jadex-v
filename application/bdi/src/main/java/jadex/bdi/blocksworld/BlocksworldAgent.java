package jadex.bdi.blocksworld;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

import javax.swing.SwingUtilities;

import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalTargetCondition;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Trigger;
import jadex.core.IComponent;
import jadex.future.SubscriptionIntermediateFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;

/**
 *  Blocksworld agent for stacking blocks.
 */
@BDIAgent
@Plan(impl=ClearBlocksPlan.class, trigger=@Trigger(goals=BlocksworldAgent.ClearGoal.class))
@Plan(impl=StackBlocksPlan.class, trigger=@Trigger(goals=BlocksworldAgent.StackGoal.class))
@Plan(impl=ConfigureBlocksPlan.class, trigger=@Trigger(goals=BlocksworldAgent.ConfigureGoal.class))
//@Plan(impl=BenchmarkPlan.class)
public class BlocksworldAgent
{
	public enum Mode{NORMAL, STEP, SLOW}
	
	/** The mode. */
	protected Mode mode = Mode.NORMAL;
	
	/** The flag for turning on/off output. */
	protected boolean quiet;
	
	/** The table for the blocks. */
//	@Belief
	protected Table table = new Table();
	
	/** The bucket for currently unused blocks. */
//	@Belief
	protected Table bucket = new Table("Bucket", Color.lightGray);
	
	/** The currently existing blocks. */
	@Belief
	protected Set<Block> blocks = new HashSet<Block>();
	
	/** The future to communicate step events from gui to plan. */
	protected SubscriptionIntermediateFuture<Void>	steps	= new SubscriptionIntermediateFuture<Void>();
	
//	/** The gui (if any). */
//	@Belief
//	protected BlocksworldGui gui;
	
	/** The agent. */
	@Inject
	protected IComponent agent;
	
	@Goal
	public class ClearGoal
	{
		/** The block. */
		protected Block block;

		/**
		 * 
		 */
		public ClearGoal(Block block)
		{
			this.block = block;
		}
		
		/**
		 * 
		 * @return True, if clear.
		 */
		@GoalTargetCondition(beliefs="blocks")
		public boolean checkClear()
		{
//			System.out.println("clear target condition for: "+block+" "+block.isClear());
			return block.isClear();
		}

		/**
		 *  Get the block.
		 *  @return The block.
		 */
		public Block getBlock()
		{
			return block;
		}
		
		/**
		 *  Get the target.
		 *  @return The target.
		 */
		public Block getTarget()
		{
			return table;
		}
	}
	
	@Goal
	public class StackGoal
	{
		/** The block. */
		protected Block block;
		
		/** The target. */
		protected Block target;
		
		/**
		 * 
		 */
		public StackGoal(Block block, Block target)
		{
			this.block = block;
			this.target = target;
		}

		@GoalTargetCondition(beliefs="blocks")
		public boolean checkOn()
		{
//			System.out.println("stack target condition for: "+block+" "+target+" "+block.getLower().equals(target));
			return block.getLower().equals(target);
		}

		/**
		 *  Get the block.
		 *  @return The block.
		 */
		public Block getBlock()
		{
			return block;
		}

		/**
		 *  Get the target.
		 *  @return The target.
		 */
		public Block getTarget()
		{
			return target;
		}
	}
	
	@Goal
	public class ConfigureGoal
	{
		/** The block. */
		protected Table configuration;
		
		/** The target. */
		protected Set<Block> blocks;
		
		/**
		 * 
		 */
		public ConfigureGoal(Table configuration, Set<Block> blocks)
		{
			this.configuration = configuration;
			this.blocks = blocks;
		}

		@GoalTargetCondition(beliefs="blocks")
		public boolean checkConfiguration()
		{
//			System.out.println("check configure goal: "+table.configurationEquals(configuration));
//			System.out.println(table);
//			System.out.println(configuration);
			return table.configurationEquals(configuration);
		}

		/**
		 *  Get the configuration.
		 *  @return The configuration.
		 */
		public Table getConfiguration()
		{
			return configuration;
		}
	}

	/**
	 *  The init code.
	 */
	@OnStart
	public void agentCreated()
	{
		Block b0 = new Block(new Color(240, 16, 16), table);
		Block b1 = new Block(new Color(16, 16, 240), table);
		Block b2 = new Block(new Color(240, 240, 16), b0);
		blocks.add(b0);
		blocks.add(b1);
		blocks.add(b2);
		blocks.add(new Block(new Color(16, 240, 16), b2));
		blocks.add(new Block(new Color(240, 16, 240), bucket));
		blocks.add(new Block(new Color(16, 240, 240), bucket));
		blocks.add(new Block(new Color(240, 240, 240), bucket));
		
	//	<fact>new Block(new Color(240, 16, 16), (Table)$beliefbase.table)</fact>
	//	<fact>new Block(new Color(16, 16, 240), (Table)$beliefbase.table)</fact>
	//	<fact>new Block(new Color(240, 240, 16), ((Table)$beliefbase.table).getAllBlocks()[0])</fact>
	//	<fact>new Block(new Color(16, 240, 16), ((Table)$beliefbase.table).getAllBlocks()[2])</fact>
	//	<fact>new Block(new Color(240, 16, 240), (Table)$beliefbase.bucket)</fact>
	//	<fact>new Block(new Color(16, 240, 240), (Table)$beliefbase.bucket)</fact>
	//	<fact>new Block(new Color(240, 240, 240), (Table)$beliefbase.bucket)</fact>
		
//		if("gui".equals(agent.getConfiguration()))
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					new BlocksworldGui(agent.getComponentHandle());
				}
			});
		}
//		else if("benchmark(runs=10/goals=10)".equals(agent.getConfiguration()))
//		{
//			quiet = true;
//			agent.getFeature(IBDIAgentFeature.class).adoptPlan(new BenchmarkPlan(10, 10));
//		}
//		else if("benchmark(runs=10/goals=50)".equals(agent.getConfiguration()))
//		{
//			quiet = true;
//			agent.getFeature(IBDIAgentFeature.class).adoptPlan(new BenchmarkPlan(10, 50));
//		}
//		else if("benchmark(runs=10/goals=500)".equals(agent.getConfiguration()))
//		{
//			quiet = true;
//			agent.getFeature(IBDIAgentFeature.class).adoptPlan(new BenchmarkPlan(10, 500));
//		}
	}
	
	/**
	 *  Get the mode.
	 *  @return The mode.
	 */
	public Mode getMode()
	{
		return mode;
	}
	
	/**
	 *  Set the mode.
	 *  @param mode The mode to set.
	 */
	public void setMode(Mode mode)
	{
		this.mode = mode;
	}

	/**
	 *  Get the quiet.
	 *  @return The quiet.
	 */
	public boolean isQuiet()
	{
		return quiet;
	}

	/**
	 *  Get the table.
	 *  @return The table.
	 */
	public Table getTable()
	{
		return table;
	}

	/**
	 *  Get the blocks.
	 *  @return The blocks.
	 */
	public Set<Block> getBlocks()
	{
		return blocks;
	}

	/**
	 *  Get the bucket.
	 *  @return The bucket.
	 */
	public Table getBucket()
	{
		return bucket;
	}

	/**
	 *  Get the agent.
	 *  @return The agent.
	 */
	public IComponent getAgent()
	{
		return agent;
	}
	
}
