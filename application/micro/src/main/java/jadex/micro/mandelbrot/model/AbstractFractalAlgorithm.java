package jadex.micro.mandelbrot.model;

import java.util.List;
import java.util.stream.Collectors;

import jadex.common.SReflect;

public abstract class AbstractFractalAlgorithm implements IFractalAlgorithm
{
	/**
	 *  Can areas be filled?
	 */
	public boolean isOptimizationAllowed()
	{
		return true;
	}
	
	/**
	 *  Should a cyclic color scheme be used?
	 */
	public boolean useColorCycle()
	{
		return true;
	}
	
	/**
	 *  The default algorithm.
	 */
	public boolean isDefault()
	{
		return false;
	}
	
	//-------- singleton semantics --------
	
	/**
	 *  Get a string representation.
	 */
	public String toString()
	{
		String name = SReflect.getUnqualifiedClassName(this.getClass());
		if(name.endsWith("Algorithm"))
			name = name.substring(0, name.length()-"Algorithm".length());
		return name;
	}
	
	/**
	 *  Test if two objects are equal.
	 */
	public boolean equals(Object obj)
	{
		return obj!=null && obj.getClass().equals(this.getClass());
	}
	
	/**
	 *  Get the hash code.
	 */
	public int hashCode()
	{
		return 31 + getClass().hashCode();
	}
	
	/**
	 *  Create a list of algorithm instances from their classes;
	 *  @param algos The algo classes.
	 *  @return The algo instances.
	 */
	public static List<IFractalAlgorithm> createAlgorithms(List<Class<IFractalAlgorithm>> algos)
	{
		return algos.stream().map(algo -> 
		{
			IFractalAlgorithm ret = null;
			try 
			{
				ret = (IFractalAlgorithm)algo.getConstructor(new Class[0]).newInstance(new Object[0]);
			} 
			catch(Exception e) 
			{
				e.printStackTrace();
			}
			return ret;
		}).collect(Collectors.toList());
	}
	
	/**
	 *  Get the default algorithm.
	 *  @param algos The algo classes.
	 *  @return The algo instances.
	 */
	public static IFractalAlgorithm getDefaultAlgorithm(List<IFractalAlgorithm> algos)
	{
		IFractalAlgorithm ret = null;
		for(IFractalAlgorithm algo: algos)
		{
			if(algo.isDefault())
			{
				ret = algo;
				break;
			}
		}
		return ret;
	}
}
