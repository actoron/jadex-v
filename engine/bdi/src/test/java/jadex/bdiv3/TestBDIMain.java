package jadex.bdiv3;

import jadex.core.IComponent;

public class TestBDIMain
{
	
	public static void main(String[] args)
	{
		IComponent.create("bdi:jadex.bdiv3.TestBDI");
		
		IComponent.waitForLastComponentTerminated();
	}
	
}

