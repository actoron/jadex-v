package jadex.bt.booktrading.domain;

import java.util.List;

import jadex.core.IComponent;

public interface INegotiationAgent
{
	public IComponent getAgent();
	
	public void createOrder(Order order);
	
	public List<Order> getOrders();
	
	public List<NegotiationReport> getReports(Order order);
}
