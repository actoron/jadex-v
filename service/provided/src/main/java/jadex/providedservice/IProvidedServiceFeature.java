package jadex.providedservice;

import jadex.core.IComponentFeature;

/**
 *  Marker interface for provided service feature that handles detection and registration of provided services.
 */
public interface IProvidedServiceFeature extends IComponentFeature
{
	/**
	 *  Get a locally provided service.
	 *  @return	The first provided service matching the type.
	 */
	public <T> T getProvidedService(Class<T> type);
}
