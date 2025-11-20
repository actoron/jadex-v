package jadex.injection;

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
		doSet(value, true);
	}
	
	/**
	 *  Called on component init.
	 */
	@Override
	void	init(IComponent comp, String name)
	{
		super.init(comp, name);
		
		observeNewValue(null, value);
	}
	
	/**
	 *  Set the value.
	 *  @throws IllegalStateException when called before component init.
	 */
	public void	set(T value)
	{
		// Support builder pattern for POJO, see BT UniversityAgent
//		if(comp==null)
//			throw new IllegalStateException("Wrapper not inited. Missing annotation?");
		
		doSet(value, false);
	}
	
	/**
	 *  Set the observation mode for inner values.
	 *  Default is COLLECTION_AND_BEAN.
	 */
	public Val<T> setObservationMode(ObservationMode mode)
	{
		super.setObservationMode(mode);
		return this;
	}
}
