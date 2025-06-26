package jadex.micro.nfpropvis;

import jadex.injection.annotation.OnStart;

/**
 *  Just provider without using services.
 */
public class ProviderAgent extends ProviderAndUserAgent
{
	/**
	 *  The agent body.
	 */
	@OnStart
	public void body()
	{
		// Empty overridden.
	}
}
