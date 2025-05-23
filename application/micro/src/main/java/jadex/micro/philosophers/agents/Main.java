package jadex.micro.philosophers.agents;

import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;

public class Main
{
	public static void main(String[] args)
	{
		int n=7;

		for(int i=0; i<n; i++)
		{
			final int no = i;
			IComponentManager.get().create(new PhilosophAgent(no)).get();
		}
				
		IComponentHandle ta = IComponentManager.get().create(new TableAgent(n, true)).get();

		
		TableGui gui = new TableGui(n, ta);
	}
}
