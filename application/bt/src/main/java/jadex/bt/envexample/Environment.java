package jadex.bt.envexample;

import java.util.ArrayList;
import java.util.List;

public class Environment 
{
	private static List<BTRandomAgent> agents = new ArrayList<BTRandomAgent>();
	
	public static synchronized void addAgent(BTRandomAgent agent)
	{
		agents.add(agent);
	}
	
	public static synchronized void removeAgent(BTRandomAgent agent)
	{
		agents.remove(agent);
	}
	
	public static synchronized List<BTRandomAgent> getAgents()
	{
		return new ArrayList<BTRandomAgent>(agents);
	}
}
