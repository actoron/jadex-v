package jadex.provided2;

import jadex.core.IComponentFeature;

/**
 *  Marker interface for provided service feature that handles detection and registration of provided services.
 */
public interface IProvided2Feature extends IComponentFeature
{
	/**
	 *  Get a locally provided service.
	 *  @return	The first provided service matching the type.
	 */
	public <T> T getProvidedService(Class<T> type);
}
