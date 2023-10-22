package jadex.serialization;

import jadex.bytecode.ProxyFactory;


/**
 *  Default replacement code for equals() method.
 */
public class DefaultEqualsMethodReplacement implements IMethodReplacement
{
	/**
	 *  Invoke the method on the given object with the given args.
	 */
	public Object invoke(Object obj, Object[] args)
	{
		// Todo: compare proxy infos instead of invocation handlers?
		return Boolean.valueOf(args[0]!=null && ProxyFactory.isProxyClass(args[0].getClass())
			&& ProxyFactory.getInvocationHandler(obj).equals(ProxyFactory.getInvocationHandler(args[0])));
	}
}