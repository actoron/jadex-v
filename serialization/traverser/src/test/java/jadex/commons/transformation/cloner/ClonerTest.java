package jadex.commons.transformation.cloner;

import jadex.common.transformation.Test;
import jadex.common.transformation.traverser.SCloner;

/**
 *  Testcases for writer and reader.
 */
public class ClonerTest extends Test
{
	
	//-------- methods --------
	
	/**
	 * 
	 */
	public Object doWrite(Object wo)
	{
		return SCloner.clone(wo);
	}
	
	/**
	 * 
	 */
	public Object doRead(Object ro) 
	{
		return ro;
	}
	
	/**
	 *  Main for testing single methods.
	 */
	public static void main(String[] args)
	{
		ClonerTest t = new ClonerTest();
		t.performTests();
	}
}
