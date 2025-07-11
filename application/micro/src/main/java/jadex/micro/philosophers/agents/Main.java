package jadex.micro.philosophers.agents;

import jadex.core.Application;
import jadex.core.IComponentHandle;

public class Main
{
	public static void main(String[] args)
	{
		Application app = new Application("Dining Philosophers");
		
		int n=7;

		for(int i=0; i<n; i++)
		{
			final int no = i;
			app.create(new PhilosophAgent(no)).get();
		}
				
		IComponentHandle ta = app.create(new TableAgent(n, true)).get();

		
		TableGui gui = new TableGui(n, ta);
	}
}
