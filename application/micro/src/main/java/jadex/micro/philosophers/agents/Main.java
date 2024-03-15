package jadex.micro.philosophers.agents;

import jadex.core.IComponent;
import jadex.core.IExternalAccess;

public class Main
{
	public static void main(String[] args)
	{
		int n=5;
		
		for(int i=0; i<n; i++)
		{
			final int no = i;
			IComponent.create(new PhilosophAgent(no)).get();
		}
		
		IExternalAccess ta = IComponent.create(new TableAgent(n, true)).get();
		TableGui gui = new TableGui(n, ta);
	}
}
