package networking.remoteresolver;

import jadex.core.IComponent;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;

public class RemoteResolverAgent 
{
    /** The agent. */
	@Inject
	protected IComponent agent;

    public RemoteResolverAgent()
    {
    }

    /**
	 *  Called once after agent creation.
	 */
	@OnStart
	public IFuture<Void> agentCreated()
	{
		return IFuture.DONE;
    }

	public static void main(String[] args) {
		System.out.println("teststat");
	}
}
