package jadex.bdi.booktrading;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.SwingUtilities;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.PlanFailureException;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalCreationCondition;
import jadex.bdi.annotation.GoalDropCondition;
import jadex.bdi.annotation.GoalTargetCondition;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Trigger;
import jadex.common.Tuple2;
import jadex.core.ComponentTerminatedException;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.future.CollectionResultListener;
import jadex.future.DelegationResultListener;
import jadex.future.Future;
import jadex.future.IResultListener;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnEnd;
import jadex.injection.annotation.OnStart;
import jadex.requiredservice.IRequiredServiceFeature;

/**
 * 
 */
@BDIAgent
public class BuyerAgent implements INegotiationAgent
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
	
	public BuyerAgent(Order[] ios)
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
	public class PurchaseBook implements INegotiationGoal
	{
//		@GoalParameter
		protected Order order;

		/**
		 *  Create a new PurchaseBook. 
		 */
		@GoalCreationCondition(factadded="orders")
		public PurchaseBook(Order order)
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
		
		@GoalDropCondition(/*parameters="order"*/beliefs="orders")
		public boolean checkDrop()
		{
			return order.getState().equals(Order.FAILED);
		}
		
		@GoalTargetCondition(/*parameters="order"*/beliefs="orders")
		public boolean checkTarget()
		{
			return Order.DONE.equals(order.getState());
		}
	}
	
	/**
	 * 
	 */
//	@Belief(rawevents={@RawEvent(ChangeEvent.GOALADOPTED), @RawEvent(ChangeEvent.GOALDROPPED), 
//		@RawEvent(ChangeEvent.PARAMETERCHANGED)})
	public List<Order> getOrders()
	{
//		System.out.println("getOrders belief called");
		List<Order> ret = new ArrayList<Order>();
		ret.addAll(orders);
//		Collection<PurchaseBook> goals = agent.getFeature(IBDIAgentFeature.class).getGoals(PurchaseBook.class);
//		for(PurchaseBook goal: goals)
//		{
//			ret.add(goal.getOrder());
//		}
		return ret;
	}
	
	/**
	 *  Get the current time.
	 */
	protected long getTime()
	{
		return agent.getFeature(IExecutionFeature.class).getTime();
	}
	
	/**
	 * 
	 */
	@Plan(trigger=@Trigger(goals=PurchaseBook.class))
	protected void purchaseBook(PurchaseBook goal)
	{
		Order order = goal.getOrder();
		double time_span = order.getDeadline().getTime() - order.getStartTime();
		double elapsed_time = getTime() - order.getStartTime();
		double price_span = order.getLimit() - order.getStartPrice();
		int acceptable_price = (int)(price_span * elapsed_time / time_span)
			+ order.getStartPrice();

		// Find available seller agents.
		Collection<IBuyBookService>	services = agent.getFeature(IRequiredServiceFeature.class).searchServices(IBuyBookService.class).get();
		if(services.isEmpty())
		{
//			System.out.println("No seller found, purchase failed.");
			generateNegotiationReport(order, null, acceptable_price);
			throw new PlanFailureException();
		}

		// Initiate a call-for-proposal.
		Future<Collection<Tuple2<IBuyBookService, Integer>>>	cfp	= new Future<Collection<Tuple2<IBuyBookService, Integer>>>();
		final CollectionResultListener<Tuple2<IBuyBookService, Integer>>	crl	= new CollectionResultListener<Tuple2<IBuyBookService, Integer>>(services.size(), true,
			new DelegationResultListener<Collection<Tuple2<IBuyBookService, Integer>>>(cfp));
		for(IBuyBookService seller: services)
		{
			seller.callForProposal(order.getTitle()).addResultListener(new IResultListener<Integer>()
			{
				public void resultAvailable(Integer result)
				{
					crl.resultAvailable(new Tuple2<IBuyBookService, Integer>(seller, result));
				}
				
				public void exceptionOccurred(Exception exception)
				{
					crl.exceptionOccurred(exception);
				}
			});
		}
		// Sort results by price.
		@SuppressWarnings("unchecked")
		Tuple2<IBuyBookService, Integer>[]	proposals	= cfp.get().toArray(new Tuple2[0]);
		Arrays.sort(proposals, new Comparator<Tuple2<IBuyBookService, Integer>>()
		{
			public int compare(Tuple2<IBuyBookService, Integer> o1, Tuple2<IBuyBookService, Integer> o2)
			{
				return o1.getSecondEntity().compareTo(o2.getSecondEntity());
			}
		});
		
//		System.out.println("proposals for "+order+": "+Arrays.asList(proposals));

		// Do we have a winner?
		if(proposals.length>0 && proposals[0].getSecondEntity().intValue()<=acceptable_price)
		{
			proposals[0].getFirstEntity().acceptProposal(order.getTitle(), proposals[0].getSecondEntity().intValue()).get();
			
			generateNegotiationReport(order, proposals, acceptable_price);
			
			// If contract-net succeeds, store result in order object.
			order.setState(Order.DONE);
			order.setExecutionPrice(proposals[0].getSecondEntity());
			order.setExecutionDate(new Date(getTime()));
		}
		else
		{
			generateNegotiationReport(order, proposals, acceptable_price);
			
			throw new PlanFailureException();
		}
		//System.out.println("result: "+cnp.getParameter("result").getValue());
	}
	
	/**
	*  Generate and add a negotiation report.
	*/
	protected void generateNegotiationReport(Order order, Tuple2<IBuyBookService, Integer>[] proposals, double acceptable_price)
	{
		String report = "Accepable price: "+acceptable_price+", proposals: ";
		if(proposals!=null)
		{
			for(int i=0; i<proposals.length; i++)
			{
				report += proposals[i].getSecondEntity()+"-"+proposals[i].getFirstEntity().toString();
				if(i+1<proposals.length)
					report += ", ";
			}
		}
		else
		{
			report	+= "No seller found, purchase failed.";
		}
		NegotiationReport nr = new NegotiationReport(order, report, getTime());
		//System.out.println("REPORT of agent: "+getAgentName()+" "+report);
		reports.add(nr);
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
		PurchaseBook goal = new PurchaseBook(order);
		agent.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(goal);
	}
	
	/**
	 *  Get all purchase or sell goals.
	 */
	public Collection<INegotiationGoal> getGoals()
	{
		@SuppressWarnings({"unchecked", "rawtypes"})
		Collection<INegotiationGoal>	ret = (Collection)agent.getFeature(IBDIAgentFeature.class).getGoals(PurchaseBook.class);
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




