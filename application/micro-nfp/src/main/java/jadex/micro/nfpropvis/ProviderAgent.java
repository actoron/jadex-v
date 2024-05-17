package jadex.micro.nfpropvis;

import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;
import jadex.providedservice.annotation.Service;

/**
 *  Just provider without using services.
 */
@Agent
@Service
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
