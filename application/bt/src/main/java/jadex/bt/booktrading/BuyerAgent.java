package jadex.bt.booktrading;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.swing.SwingUtilities;

import org.w3c.dom.events.Event;

import jadex.bt.IBTProvider;
import jadex.bt.actions.TerminableUserAction;
import jadex.bt.actions.UserAction;
import jadex.bt.booktrading.domain.IBuyBookService;
import jadex.bt.booktrading.domain.INegotiationAgent;
import jadex.bt.booktrading.domain.NegotiationReport;
import jadex.bt.booktrading.domain.Order;
import jadex.bt.booktrading.gui.Gui;
import jadex.bt.cleanerworld.environment.IChargingstation;
import jadex.bt.cleanerworld.environment.ILocation;
import jadex.bt.cleanerworld.environment.IWaste;
import jadex.bt.cleanerworld.environment.IWastebin;
import jadex.bt.decorators.ChildCreationDecorator;
import jadex.bt.decorators.ConditionalDecorator;
import jadex.bt.decorators.FailureDecorator;
import jadex.bt.decorators.RepeatDecorator;
import jadex.bt.decorators.RetryDecorator;
import jadex.bt.decorators.SuccessDecorator;
import jadex.bt.decorators.TriggerDecorator;
import jadex.bt.impl.BTAgentFeature;
import jadex.bt.nodes.ActionNode;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.SelectorNode;
import jadex.bt.nodes.SequenceNode;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.nodes.ParallelNode;
import jadex.common.Tuple2;
import jadex.core.ComponentTerminatedException;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.future.CollectionResultListener;
import jadex.future.DelegationResultListener;
import jadex.future.Future;
import jadex.future.IResultListener;
import jadex.future.ITerminableFuture;
import jadex.future.TerminableFuture;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnEnd;
import jadex.model.annotation.OnStart;
import jadex.requiredservice.IRequiredServiceFeature;
import jadex.rules.eca.EventType;

@Agent(type="bt")
public class BuyerAgent implements INegotiationAgent, IBTProvider
{
	@Agent
	protected IComponent agent;
	
	protected List<NegotiationReport> reports = new ArrayList<NegotiationReport>();
	
	protected List<Order> orders = new ArrayList<Order>();
	
	protected Future<Gui> gui;
	
	protected Order[] ios;
	
	public BuyerAgent(Order[] ios)
	{
		this.ios = ios;
	}
	
	public Node<IComponent> createBehaviorTree()
	{
		ParallelNode<IComponent> buybooks = new ParallelNode<>("buybooks");
		buybooks.addDecorator(new ChildCreationDecorator<IComponent>()
			.setCondition((node, state, context) -> true)
			.observeCondition(new EventType[]{new EventType(BTAgentFeature.VALUEADDED, "orders")})
			.setChildCreator((event) -> createPurchaseAction((Order)event.value())));
		return buybooks;
	}
	
	int bookcnt = 0;
	public Node<IComponent> createPurchaseAction(Order order)
	{
		ActionNode<IComponent> purchasebook = new ActionNode<>("purchasebook_"+bookcnt++);
		purchasebook.setAction(new TerminableUserAction<IComponent>((e, agent) ->
		{
			//Order order = (Order)e.value();
			System.out.println("Purchase book: "+order);
			
			TerminableFuture<NodeState> ret = new TerminableFuture<>();
			
			//ret.setTerminationCommand(ex -> {System.out.println("terminate on actsense moveTo"); fut.terminate();});
			//fut.then(Void -> ret.setResultIfUndone(NodeState.FAILED)).catchEx(ex -> ret.setResultIfUndone(NodeState.FAILED));

			//Order order = goal.getOrder();
			double time_span = order.getDeadline().getTime() - order.getStartTime();
			double elapsed_time = getTime() - order.getStartTime();
			double price_span = order.getLimit() - order.getStartPrice();
			int acceptable_price = (int)(price_span * elapsed_time / time_span)
				+ order.getStartPrice();

			// Find available seller agents.
			Collection<IBuyBookService>	services = agent.getFeature(IRequiredServiceFeature.class).getServices(IBuyBookService.class).get();
			if(services.isEmpty())
			{
//				System.out.println("No seller found, purchase failed.");
				generateNegotiationReport(order, null, acceptable_price);
				//throw new PlanFailureException();
				ret.setResult(NodeState.FAILED);
			}

			// Initiate a call-for-proposal.
			Future<Collection<Tuple2<IBuyBookService, Integer>>> cfp = new Future<Collection<Tuple2<IBuyBookService, Integer>>>();
			final CollectionResultListener<Tuple2<IBuyBookService, Integer>> crl = new CollectionResultListener<Tuple2<IBuyBookService, Integer>>(services.size(), true,
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
			Tuple2<IBuyBookService, Integer>[] proposals = cfp.get().toArray(new Tuple2[0]);
			Arrays.sort(proposals, new Comparator<Tuple2<IBuyBookService, Integer>>()
			{
				public int compare(Tuple2<IBuyBookService, Integer> o1, Tuple2<IBuyBookService, Integer> o2)
				{
					return o1.getSecondEntity().compareTo(o2.getSecondEntity());
				}
			});

			// Do we have a winner?
			if(proposals.length>0 && proposals[0].getSecondEntity().intValue()<=acceptable_price)
			{
				proposals[0].getFirstEntity().acceptProposal(order.getTitle(), proposals[0].getSecondEntity().intValue()).get();
				
				generateNegotiationReport(order, proposals, acceptable_price);
				
				// If contract-net succeeds, store result in order object.
				order.setState(Order.DONE);
				order.setExecutionPrice(proposals[0].getSecondEntity());
				order.setExecutionDate(new Date(getTime()));
				
				ret.setResult(NodeState.SUCCEEDED);
			}
			else
			{
				generateNegotiationReport(order, proposals, acceptable_price);
				
				ret.setResult(NodeState.FAILED);
			}
			
			return ret;
			//System.out.println("result: "+cnp.getParameter("result").getValue());
		}));
		
		
		purchasebook.addDecorator(new FailureDecorator<IComponent>()
			.setCondition((node, state, context) -> order.getState().equals(Order.FAILED))
			.observeCondition(new EventType[]{new EventType(BTAgentFeature.PROPERTYCHANGED, "orders", "state")}));
		
		return purchasebook;
	}
	
	@Override
	public List<Order> getOrders() 
	{
		return orders;
	}
	
	@Override
	public void createOrder(Order order) 
	{
		orders.add(order);
	}
	
	@OnStart
	public void start()
	{
		IExecutionFeature exe = agent.getFeature(IExecutionFeature.class);
		if(ios!=null)
		{
			for(Order o: ios)
			{
				// Hack!!! use start time as deadline for initial orders
				o.setDeadline(new Date(exe.getTime()+o.getStartTime()), exe);
				o.setStartTime(getTime());
				createOrder(o);
			}
		}
		
		gui	= new Future<>();
		SwingUtilities.invokeLater(()->
		{
			try
			{
				gui.setResult(new Gui(agent.getExternalAccess()));
			}
			catch(ComponentTerminatedException cte)
			{
			}
		});
	}
	
	/**
	 *  Called when agent terminates.
	 */
	@OnEnd
	public void shutdown()
	{
		if(gui!=null)
			gui.then(thegui -> SwingUtilities.invokeLater(()->thegui.dispose()));
	}
	
	public IComponent getAgent()
	{
		return agent;
	}
	
	/*
	@Goal(recur=true, recurdelay=10000, unique=true)
	public class PurchaseBook implements INegotiationGoal
	{
		@GoalParameter
		protected Order order;

		public PurchaseBook(Order order)
		{
			this.order = order;
		}

		public Order getOrder()
		{
			return order;
		}
		
		@GoalDropCondition(parameters="order")
		public boolean checkDrop()
		{
			return order.getState().equals(Order.FAILED);
		}
		
		@GoalTargetCondition(parameters="order")
		public boolean checkTarget()
		{
			return Order.DONE.equals(order.getState());
		}
	}*/
	
	/*@Belief(rawevents={@RawEvent(ChangeEvent.GOALADOPTED), @RawEvent(ChangeEvent.GOALDROPPED), 
		@RawEvent(ChangeEvent.PARAMETERCHANGED)})
	public List<Order> getOrders()
	{
//		System.out.println("getOrders belief called");
		List<Order> ret = new ArrayList<Order>();
		Collection<PurchaseBook> goals = agent.getFeature(IBDIAgentFeature.class).getGoals(PurchaseBook.class);
		for(PurchaseBook goal: goals)
		{
			ret.add(goal.getOrder());
		}
		return ret;
	}*/
	
	protected long getTime()
	{
		return agent.getFeature(IExecutionFeature.class).getTime();
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




