package jadex.collection;

import java.util.Set;

/**
 * 
 */
public class SetWrapper <T> extends CollectionWrapper<T> implements Set<T>
{
	/**
	 *  Create a new wrapper.
	 *  @param delegate The delegate.
	 */
	public SetWrapper(Set<T> delegate)
	{
		super(delegate);
	}
}
