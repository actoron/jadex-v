package jadex.micro.philosophers.thread;

public class Main
{
	public static void main(String[] args)
	{
		int n=5;
		Table t = new Table(n,true);
		TableGui gui = new TableGui(n, t);
		
		for(int i=0; i<5; i++)
		{
			final int no = i;
			Thread p = new Thread(new Philosopher(no, t));
			p.start();
		}
	}
}
