package jadex.injection.impl;

/**
 *  Add handles to model for executing extra code, e.g. on start.
 */
@FunctionalInterface
public interface IExtraCodeCreator
{
	/**
	 *  Perform creation of extra code handles at model time.
	 *  Called once on init of each model
	 *  
	 *  @param model The injection model to add code handles to.
	 */
	public void	addExtraCode(InjectionModel model);
}
