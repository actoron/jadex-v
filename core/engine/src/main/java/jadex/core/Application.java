package jadex.core;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import jadex.core.impl.ComponentManager;
import jadex.future.IFuture;

public class Application implements IComponentFactory
{
	private static final AtomicInteger idgen = new AtomicInteger();
	
	protected String id;
	
	protected String name;
	
	protected String secret;

	public Application(String name)
	{
		this(name, name+"_"+idgen.getAndIncrement());
	}
	
	public Application(String name, String id)
	{
		this.name = name;
		this.id = id;
	}
	
	public String getName() 
	{
		return name;
	}

	public String getId() 
	{
		return id;
	}

	public void setSecret(String secret) 
	{
		this.secret = secret;
	}

	public String getSecret() 
	{
		return secret;
	}
	
	/**
	 *  Create a component based on a pojo.
	 *  @param pojo The pojo.
	 *  @param cid The component id.
	 *  @return The external access of the running component.
	 */
	public IFuture<IComponentHandle> create(Object pojo, ComponentIdentifier cid)
	{		
		return create(pojo, null, this);
	}
	
	@Override
	public Set<ComponentIdentifier> getAllComponents()
	{
		return ComponentManager.get().getAllComponents(this);
	}
	
	@Override
	public void waitForLastComponentTerminated()
	{
		ComponentManager.get().doWaitForLastComponentTerminated(this);
	}

	@Override
	public int hashCode() 
	{
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) 
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Application other = (Application) obj;
		return Objects.equals(id, other.id);
	}
}
