package jadex.bdi;

import jadex.collection.IEventPublisher;
import jadex.core.IComponent;

/**
 *  Wrapper for observable values.
 *  Generates appropriate events on changes.
 */
public class Val<T>	extends AbstractDynVal<T>
{
	/**
	 *  Create an observable with a given value.
	 */
	public Val(T value)
	{
		doSet(value);
	}
	
	/**
	 *  Called on component init.
	 */
	@Override
	void	init(IComponent comp, IEventPublisher changehandler, boolean observeinner)
	{
		super.init(comp, changehandler, observeinner);
		
		observeNewValue(null, value);
	}
	
	/**
	 *  Set the value.
	 *  @throws IllegalStateException when called before component init.
	 */
	public void	set(T value)
	{
		if(changehandler==null)
			throw new IllegalStateException("Wrapper not inited. Missing annotation?");
		
		doSet(value);
	}
}
