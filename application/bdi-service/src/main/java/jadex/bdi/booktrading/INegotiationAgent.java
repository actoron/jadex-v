package jadex.bdi.booktrading;

import java.util.Collection;
import java.util.List;

import jadex.core.IComponent;

/**
 * 
 */
public interface INegotiationAgent
{
	/**
	 *  Get the agent.
	 *  @return The agent.
	 */
	public IComponent getAgent();
	
	/**
	 *  Create a purchase or sell oder.
	 */
	public void createGoal(Order order);
	
	/**
	 *  Get all purchase or sell goals.
	 */
	public Collection<INegotiationGoal> getGoals();
	
	/**
	 *  Get all orders.
	 */
	public List<Order> getOrders();
	
	/**
	 *  Get all reports.
	 */
	public List<NegotiationReport> getReports(Order order);
}
