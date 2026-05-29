package jadex.bt.booktrading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import jadex.bt.IBTProvider;
import jadex.bt.actions.TerminableUserAction;
import jadex.bt.booktrading.domain.IBuyBookService;
import jadex.bt.booktrading.domain.INegotiationAgent;
import jadex.bt.booktrading.domain.NegotiationReport;
import jadex.bt.booktrading.domain.Order;
import jadex.bt.booktrading.gui.Gui;
import jadex.bt.decorators.ChildCreationDecorator;
import jadex.bt.decorators.FailureDecorator;
import jadex.bt.nodes.ActionNode;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.nodes.ParallelNode;
import jadex.bt.tool.BTViewer;
import jadex.core.ChangeEvent;
import jadex.core.ChangeEvent.Type;
import jadex.core.ComponentTerminatedException;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.TerminableFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnEnd;
import jadex.injection.annotation.OnStart;

public class SellerAgent implements IBuyBookService, INegotiationAgent, IBTProvider
{
	public record MakeProposal(String cfp, Future<Integer> proposal)
	{
	}
	
	public record ExecuteTask(String cfp, int proposal, Future<Void> result)
	{
	}
	
	@Inject
	protected IComponent agent;
	
	protected List<NegotiationReport> reports = new ArrayList<NegotiationReport>();
	
	protected List<Order> orders = new ArrayList<>();
	
	protected List<MakeProposal> mprops = new ArrayList<>();

	protected List<ExecuteTask> tasks = new ArrayList<>();
	
	protected Future<Gui> gui;
	
	protected Order[] ios;

	protected BTViewer btviewer;
	
	public SellerAgent(Order[] ios)
	{
		this.ios = ios;
	}
	
	public Node<IComponent> createBehaviorTree()
	{
		ParallelNode<IComponent> sellbooks = new ParallelNode<>("sellbooks");
		sellbooks.setKeepRunning(true); // do not terminate when no children
		sellbooks.addDecorator(new ChildCreationDecorator<IComponent>()
			.setCondition((node, state, context) -> true)
			.setEvents(new ChangeEvent(Type.ADDED, "mprops"))
			.setChildCreator((event) -> createMakeProposalAction((MakeProposal)event.value())));
		sellbooks.addDecorator(new ChildCreationDecorator<IComponent>()
			.setCondition((node, state, context) -> true)
			.setEvents(new ChangeEvent(Type.ADDED, "tasks"))
			.setChildCreator((event) -> createExecuteTaskAction((ExecuteTask)event.value())));
		sellbooks.addDecorator(new FailureDecorator<IComponent>()
			.setCondition((node, state, context) -> 
			{
				System.out.println("sellbooks failure check: "+orders.size());
				if(orders.isEmpty())
					return false;

				boolean done = true;

				for(Order order: orders)
				{
					if(order.getState().equals(Order.OPEN))
					{
						done = false;
						break;
					}
				}
				System.out.println("sellbooks finish check: "+done);
				return done;
			})
			.setEvents(new ChangeEvent(Type.CHANGED, "orders")));
			//.setEvents(new ChangeEvent(Type.CHANGED, "orders", "state")));
		
		// add a repeat decorator that keeps the node running for ever
		// must add an event as rulebase checks and throws exception when rule without 
		//sellbooks.addDecorator(new RepeatDecorator<IComponent>((e, s, c) -> new Future<Boolean>(false))
		//	.setEvents(new EventType[]{new EventType(BTAgentFeature.PROPERTYCHANGED, "none")}));
		return sellbooks;
	}
	
	int cnt = 0;
	public Node<IComponent> createMakeProposalAction(MakeProposal mprop)
	{
		ActionNode<IComponent> makeproposal = new ActionNode<>("makeproposal_"+cnt++);
		makeproposal.setAction(new TerminableUserAction<IComponent>((e, agent) ->
		{
			TerminableFuture<NodeState> ret = new TerminableFuture<>();
			
			//MakeProposal mprop = (MakeProposal)e.value();
			System.out.println("Make proposal: "+mprop);
			
			final long time = getTime();
			List<Order> orders = getOrders(mprop.cfp());
			
			if(orders.isEmpty())
			{
				System.out.println("no orders found for: "+mprop.cfp());
				ret.setResult(NodeState.FAILED);
				return ret;
			}
				
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
			if(order!=null)
			{
				double time_span = order.getDeadline().getTime() - order.getStartTime();
				double elapsed_time = getTime() - order.getStartTime();
				double price_span = order.getLimit() - order.getStartPrice();
				int acceptable_price =  (int)(price_span * elapsed_time / time_span) + order.getStartPrice();
				System.out.println(agent.getId().getLocalName()+" proposed: " + acceptable_price);
				
				// Store proposal data in plan parameters.
				mprop.proposal().setResult(acceptable_price);
				
				String report = "Made proposal: "+acceptable_price;
				NegotiationReport nr = new NegotiationReport(order, report, getTime());
				reports.add(nr);
				ret.setResult(NodeState.SUCCEEDED);
			}
			else
			{
				ret.setResult(NodeState.FAILED);
			}
			
			return ret;
		}));
		
		return makeproposal;
	}
		
	public Node<IComponent> createExecuteTaskAction(ExecuteTask task)
	{
		ActionNode<IComponent> executetask = new ActionNode<>("executetask_"+cnt++);
		executetask.setAction(new TerminableUserAction<IComponent>((e, agent) ->
		{
			System.out.println("execute task: "+task);
			
			TerminableFuture<NodeState> ret = new TerminableFuture<>();
			
			// Search suitable open orders.
			final long time = getTime();
			List<Order> orders = getOrders(task.cfp());
			
			if(orders.isEmpty())
			{
				ret.setResult(NodeState.FAILED);
				return ret;
			}
				
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
			
			System.out.println("found order: "+order);
			
			// Use most urgent order for preparing proposal.
			if(order!=null)
			{
				double time_span = order.getDeadline().getTime() - order.getStartTime();
				double elapsed_time = getTime() - order.getStartTime();
				double price_span = order.getLimit() - order.getStartPrice();
				int acceptable_price =  (int)(price_span * elapsed_time / time_span) + order.getStartPrice();
			
				// Extract order data.
				int price = task.proposal();
				
				if(price>=acceptable_price)
				{
					//getLogger().info("Execute order plan: "+price+" "+order);
					System.out.println("Execute order plan: "+price+" "+order);
		
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
					// Store proposal data in plan parameters.
					task.result().setResult(null);
					ret.setResult(NodeState.SUCCEEDED);
				}
				else
				{
					ret.setResult(NodeState.FAILED);
				}
			}
			else
			{
				ret.setResult(NodeState.FAILED);
			}
			
			return ret;
		}));
		//executetask.addDecorator(new RepeatDecorator<IComponent>()
		//	.setCondition((event, state, context) -> true));
			//.observeCondition(new EventType[]{new EventType(BTAgentFeature.VALUEADDED, "mprops")})
			//.setChildCreator((event) -> createSellSubTree((MakeProposal)event.value())));
	
		//SequenceNode<IComponent> sellbook = new SequenceNode<IComponent>("sellbook"+bookcnt++);
//		sellbook.addChild(sellbook);
//		
//		sellbook.addDecorator(new FailureDecorator<IComponent>()
//			.setCondition((node, state, context) -> order.getState().equals(Order.FAILED))
//			.observeCondition(new EventType[]{new EventType(BTAgentFeature.PROPERTYCHANGED, "orders", "state")}));
		
		// retry all 10 secs
		
		return executetask;
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
				new Gui(agent.getComponentHandle(), 10000);
				new BTViewer(agent.getComponentHandle(), 0).setVisible(true);; 
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
	public void shutdown(IComponent agent, Exception ex)
	{
		System.out.println("Seller agent terminating: "+agent.getId()+" "+ex);
		
		/*if(btviewer!=null)
		{
			SwingUtilities.invokeLater(() -> 
			{
				//System.out.println("Disposing btviewer... "+agent.getId().getLocalName()+" "+btviewer);
				btviewer.dispose();
			});
		}*/
	}
	
	protected long getTime()
	{
		return agent.getFeature(IExecutionFeature.class).getTime();
	}
	
	// called by buyer 1
	public IFuture<Integer> callForProposal(String title)
	{
		Future<Integer> ret = new Future<Integer>();
		MakeProposal mprop = new MakeProposal(title, ret);
		mprops.add(mprop);
		return ret;
	}

	// called by buyer 2
	public IFuture<Void> acceptProposal(String title, int price)
	{
		final Future<Void>	ret	= new Future<Void>();
		ExecuteTask task = new ExecuteTask(title, price, ret);
		tasks.add(task);
		return ret;
	}
	
	public IComponent getAgent()
	{
		return agent;
	}
	
	public void createOrder(Order order)
	{
		orders.add(order);
		System.out.println("order created: "+orders.size()+" "+order);
	}
	
	public List<Order> getOrders()
	{
		return orders;
	}
	
	public List<Order> getOrders(String title)
	{
		List<Order> ret = new ArrayList<Order>();
		for(Order order: orders)
		{
			if(title==null || title.equals(order.getTitle()))
			{
				ret.add(order);
			}
		}
		return ret;
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
