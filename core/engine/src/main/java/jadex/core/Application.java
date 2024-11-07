package jadex.core;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

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
	public IFuture<IExternalAccess> create(Object pojo, ComponentIdentifier cid)
	{		
		return create(pojo, null, this);
	}
	
	/**
	 *  Wait for the last component being terminated.
	 *  This call keeps the calling thread waiting till termination.
	 */
	public void waitForLastComponentTerminated()
	{
		// Use reentrant lock/condition instead of synchronized/wait/notify to avoid pinning when using virtual threads.
		ReentrantLock lock	= new ReentrantLock();
		Condition	wait	= lock.newCondition();

	    try 
	    { 
	    	lock.lock();
		    //synchronized(ComponentManager.get().components) 
		    //{
	    	ComponentManager.get().runWithComponentsLock(() ->
	    	{
		        if(ComponentManager.get().getNumberOfComponents(getId()) == 0) 
		        {
		        	System.out.println("ret with 0");
		            return;
		        }
		        IComponentManager.get().addComponentListener(new IComponentListener() 
		        {
		            @Override
		            public void lastComponentRemoved(ComponentIdentifier cid, String appid) 
		            {
		            	if(!getId().equals(appid))
		            		return;
		            	
		        	    try 
		        	    { 
		        	    	lock.lock();
		        	    	IComponentManager.get().removeComponentListener(this, IComponentManager.COMPONENT_LASTREMOVEDAPP);
		                    wait.signal();
		                }
		        	    finally
		        	    {
		        			lock.unlock();
		        		}
		            }
		        }, IComponentManager.COMPONENT_LASTREMOVEDAPP);
		    });
		    
	    	try 
		    {
		    	wait.await();
		    } 
		    catch(InterruptedException e) 
		    {
		        e.printStackTrace();
		    }
	    }
	    finally
	    {
			lock.unlock();
		}
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
