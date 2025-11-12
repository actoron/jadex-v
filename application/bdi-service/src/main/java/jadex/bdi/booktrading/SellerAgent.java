package jadex.bdi.booktrading;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.PlanFailureException;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalCreationCondition;
import jadex.bdi.annotation.GoalDropCondition;
import jadex.bdi.annotation.GoalParameter;
import jadex.bdi.annotation.GoalTargetCondition;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Trigger;
import jadex.core.ComponentTerminatedException;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnEnd;
import jadex.injection.annotation.OnStart;

@BDIAgent
public class SellerAgent implements IBuyBookService, INegotiationAgent
{
	@Inject
	protected IComponent agent;
	
	@Belief
	protected List<NegotiationReport> reports = new ArrayList<NegotiationReport>();

	protected Future<Gui> gui;
	
	/** The uninited orders, that are passed as arguments. */
	protected Order[]	ios;
	
	/** The inited orders as beliefs. */
	@Belief
	protected Set<Order>	orders	= new LinkedHashSet<>();
	
	public SellerAgent(Order[] ios)
	{
		this.ios	= ios;
	}

	/**
	 *  The agent body.
	 */
	@OnStart
	public void body()
	{
		IExecutionFeature	exe	= agent.getFeature(IExecutionFeature.class);
		if(ios!=null)
		{
			for(Order o: ios)
			{
				// Hack!!! use start time as deadline for initial orders
				o.setDeadline(new Date(exe.getTime()+o.getStartTime()), exe);
				o.setStartTime(getTime());
				orders.add(o);
			}
		}
		
		gui	= new Future<>();
		SwingUtilities.invokeLater(()->
		{
			try
			{
				gui.setResult(new Gui(agent.getComponentHandle()));
			}
			catch(ComponentTerminatedException cte)
			{
			}
		});
	}
	
	/**
	 *  Called when agent terminates.
	 */
	//@AgentKilled
	@OnEnd
	public void shutdown()
	{
		if(gui!=null)
			gui.then(thegui -> SwingUtilities.invokeLater(()->thegui.dispose()));
	}
	
	@Goal(recur=true, recurdelay=10000/*, unique=true*/)
	public class SellBook implements INegotiationGoal
	{
		@GoalParameter
		protected Order order;

		/**
		 *  Create a new SellBook. 
		 */
		@GoalCreationCondition(factadded="orders")
		public SellBook(Order order)
		{
			this.order = order;
		}

		/**
		 *  Get the order.
		 *  @return The order.
		 */
		public Order getOrder()
		{
			return order;
		}
		
		@GoalDropCondition
		public boolean checkDrop()
		{
			return order.getState().equals(Order.FAILED);
		}
		
		@GoalTargetCondition
		public boolean checkTarget()
		{
			return Order.DONE.equals(order.getState());
		}
	}
	
	@Goal
	public class MakeProposal	implements Supplier<Integer>
	{
		protected String cfp;
		protected Integer proposal	= null;
		
		/**
		 *  Create a new MakeProposal. 
		 */
		public MakeProposal(String cfp)
		{
			this.cfp = cfp;
		}

		/**
		 *  Get the cfp.
		 *  @return The cfp.
		 */
		public String getCfp()
		{
			return cfp;
		}

		@Override
		public Integer get()
		{
			return proposal;
		}

		/**
		 *  Set the proposal.
		 *  @param proposal The proposal to set.
		 */
		public void setProposal(int proposal)
		{
			this.proposal = proposal;
		}
		
	}
	
	@Goal
	public class ExecuteTask
	{
		protected String cfp;
		protected int proposal;
		
		/**
		 *  Create a new ExecuteTask. 
		 */
		public ExecuteTask(String cfp, int proposal)
		{
			super();
			this.cfp = cfp;
			this.proposal = proposal;
		}

		/**
		 *  Get the cfp.
		 *  @return The cfp.
		 */
		public String getCfp()
		{
			return cfp;
		}

		/**
		 *  Get the proposal.
		 *  @return The proposal.
		 */
		public int getProposal()
		{
			return proposal;
		}
	}

	/**
	 * 
	 */
//	@Belief(rawevents={@RawEvent(ChangeEvent.GOALADOPTED), @RawEvent(ChangeEvent.GOALDROPPED)})
	public List<Order> getOrders()
	{
		List<Order> ret = new ArrayList<Order>();
		ret.addAll(orders);
//		Collection<SellBook> goals = agent.getFeature(IBDIAgentFeature.class).getGoals(SellBook.class);
//		for(SellBook goal: goals)
//		{
//			ret.add(goal.getOrder());
//		}
		return ret;
	}
	
	/**
	 * 
	 */
	public List<Order> getOrders(String title)
	{
		List<Order> ret = new ArrayList<Order>();
		Collection<SellBook> goals = agent.getFeature(IBDIAgentFeature.class).getGoals(SellBook.class);
		for(SellBook goal: goals)
		{
			if(title==null || title.equals(goal.getOrder().getTitle()))
			{
				ret.add(goal.getOrder());
			}
		}
		return ret;
	}
	
	@Plan(trigger=@Trigger(goals=MakeProposal.class))
	protected void makeProposal(MakeProposal goal)
	{
		final long time = getTime();
		List<Order> orders = getOrders(goal.getCfp());
		
		if(orders.isEmpty())
			throw new PlanFailureException();
			
		Collections.sort(orders, new Comparator<Order>()
		{
			public int compare(Order o1, Order o2)
			{
				double prio1 = (time-o1.getStartTime()) / (o1.getDeadline().getTime()-o1.getStartTime());
				double prio2 = (time-o1.getStartTime()) / (o1.getDeadline().getTime()-o1.getStartTime());
				return prio1>prio2? 1: prio1<prio2? -1: o1.hashCode()-o2.hashCode();
			}
		});
		Order order = orders.get(0);
		
		// Use most urgent order for preparing proposal.
//		if(suitableorders.length > 0)
		if(order!=null)
		{
//			Order order = suitableorders[0];
			
			double time_span = order.getDeadline().getTime() - order.getStartTime();
			double elapsed_time = getTime() - order.getStartTime();
			double price_span = order.getLimit() - order.getStartPrice();
			int acceptable_price =  (int)(price_span * elapsed_time / time_span) + order.getStartPrice();
//			System.out.println(agent.getId().getLocalName()+" proposed: " + acceptable_price);
			
			// Store proposal data in plan parameters.
			goal.setProposal(acceptable_price);
			
			String report = "Made proposal: "+acceptable_price;
			NegotiationReport nr = new NegotiationReport(order, report, getTime());
			reports.add(nr);
		}
	}
	
	@Plan(trigger=@Trigger(goals=ExecuteTask.class))
	protected void executeTask(ExecuteTask goal)
	{
		// Search suitable open orders.
		final long time = getTime();
		List<Order> orders = getOrders(goal.getCfp());
		
		if(orders.isEmpty())
			throw new PlanFailureException();
			
		Collections.sort(orders, new Comparator<Order>()
		{
			public int compare(Order o1, Order o2)
			{
				double prio1 = (time-o1.getStartTime()) / (o1.getDeadline().getTime()-o1.getStartTime());
				double prio2 = (time-o1.getStartTime()) / (o1.getDeadline().getTime()-o1.getStartTime());
				return prio1>prio2? 1: prio1<prio2? -1: o1.hashCode()-o2.hashCode();
			}
		});
		Order order = orders.get(0);
		
		// Use most urgent order for preparing proposal.
	//	if(suitableorders.length > 0)
		if(order!=null)
		{
	//		Order order = suitableorders[0];
			
			double time_span = order.getDeadline().getTime() - order.getStartTime();
			double elapsed_time = getTime() - order.getStartTime();
			double price_span = order.getLimit() - order.getStartPrice();
			int acceptable_price =  (int)(price_span * elapsed_time / time_span) + order.getStartPrice();
		
			// Extract order data.
			int price = goal.getProposal();
			
			if(price>=acceptable_price)
			{
	//			getLogger().info("Execute order plan: "+price+" "+order);
	
				// Initiate payment and delivery.
				// IGoal pay = createGoal("payment");
				// pay.getParameter("order").setValue(order);
				// dispatchSubgoalAndWait(pay);
				// IGoal delivery = createGoal("delivery");
				// delivery.getParameter("order").setValue(order);
				// dispatchSubgoalAndWait(delivery);
			
				// Save successful transaction data.
				order.setState(Order.DONE);
				order.setExecutionPrice(price);
				order.setExecutionDate(new Date(getTime()));
				
				String report = "Sold for: "+price;
				NegotiationReport nr = new NegotiationReport(order, report, getTime());
				reports.add(nr);
			}
			else
			{
				throw new PlanFailureException();
			}
		}
	}
	
	/**
	 *  Get the current time.
	 */
	protected long getTime()
	{
		return agent.getFeature(IExecutionFeature.class).getTime();
	}
	
	/**
	 *  Ask the seller for a a quote on a book.
	 *  @param title	The book title.
	 *  @return The price.
	 */
	public IFuture<Integer> callForProposal(String title)
	{
		MakeProposal goal = new MakeProposal(title);
		return agent.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(goal);
	}

	/**
	 *  Buy a book
	 *  @param title	The book title.
	 *  @param price	The price to pay.
	 *  @return A future indicating if the transaction was successful.
	 */
	public IFuture<Void> acceptProposal(String title, int price)
	{
		ExecuteTask goal = new ExecuteTask(title, price);
		return agent.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(goal);
	}
	
	/**
	 *  Get the agent.
	 *  @return The agent.
	 */
	public IComponent getAgent()
	{
		return agent;
	}
	
	/**
	 *  Create a purchase or sell oder.
	 */
	public void createGoal(Order order)
	{
		SellBook goal = new SellBook(order);
		agent.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(goal);
	}
	
	/**
	 *  Get all purchase or sell goals.
	 */
	public Collection<INegotiationGoal> getGoals()
	{
		@SuppressWarnings({"unchecked", "rawtypes"})
		Collection<INegotiationGoal>	ret = (Collection)agent.getFeature(IBDIAgentFeature.class).getGoals(SellBook.class);
		return ret;
	}
	
	/**
	 *  Get all reports.
	 */
	public List<NegotiationReport> getReports(Order order)
	{
		List<NegotiationReport> ret = new ArrayList<NegotiationReport>();
		for(NegotiationReport rep: reports)
		{
			if(rep.getOrder().equals(order))
			{
				ret.add(rep);
			}
		}
		return ret;
	}
}
