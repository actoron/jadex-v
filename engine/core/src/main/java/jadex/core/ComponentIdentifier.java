package jadex.core;

import java.util.concurrent.atomic.AtomicLong;

import jadex.core.impl.ComponentManager;
import jadex.idgenerator.IdGenerator;

/**
 *  Identifier for components.
 */
public class ComponentIdentifier
{
	protected static IdGenerator gen = new IdGenerator();
	
	/** Counter for auto-generated local IDs */
	private static final AtomicLong ID_COUNTER = new AtomicLong();
	
	/** The process-local name. */
	public String localname;
	
	/** The process ID of the process on the host running the component. */
	public long pid;
	
	/** The host running the process that is running the component. */
	public String host;
	
	/**
	 *  Auto-generates a ComponentIdentifier.
	 *  
	 */
	public ComponentIdentifier()
	{
		ComponentManager cm = ComponentManager.get();
		this.localname = ComponentManager.get().isComponentIdNumberMode()? ""+ID_COUNTER.getAndIncrement(): gen.idStringFromNumber(ID_COUNTER.getAndIncrement());
		this.pid = cm.pid();
		this.host = cm.host();
	}
	
	/**
	 *  Generates a ComponentIdentifier using a custom local ID.
	 *  
	 *  @param localid Local identifier of the component.
	 */
	public ComponentIdentifier(String localname)
	{
		ComponentManager cm = ComponentManager.get();
		this.localname = localname;
		this.pid = cm.pid();
		this.host = cm.host();
	}
	
	/**
	 *  Generates a ComponentIdentifier from its elements.
	 *  
	 *  @param localid Local identifier of the component.
	 *  @param pid Process ID of the process on the host running the component
	 *  @param host Host running the process that is running the component
	 */
	public ComponentIdentifier(String localname, long pid, String host)
	{
		this.localname = localname;
		this.pid = pid;
		this.host = host;
	}
	
	/**
	 *  Returns the local id as a 
	 * @return
	 */
	public String getLocalName()
	{
		return localname;
	}
	
	/**
	 *  Generates a hashcode.
	 */
	public int hashCode()
	{
		return 13 * (localname.hashCode() + Long.hashCode(pid) + host.hashCode());
	}
	
	/**
	 *  Compares the ID.
	 */
	public boolean equals(Object obj)
	{
		if (obj instanceof ComponentIdentifier)
		{
			ComponentIdentifier other = (ComponentIdentifier) obj;
			return localname.equals(other.localname) && pid == other.pid && host.equals(other.host);
		}
		return false;
	}
	
	/**
	 *  Converts the ID to a unique String.
	 */
	public String toString()
	{
		return localname + "@" + pid + "@" + host;
	}
	
	/**
	 *  Instantiates a ComponentIdentifier using an ID-String.
	 *  
	 *  @param idstring The ID-String obtained by calling toString().
	 *  @return A ComponentIdentifier.
	 */
	public static final ComponentIdentifier fromString(String idstring)
	{
		String[] splitstr = idstring.split("@");
		
		if (splitstr.length == 3)
		{
			long pid = Long.parseLong(splitstr[1]);
			
			return new ComponentIdentifier(splitstr[0], pid, splitstr[2]);
		}
		throw new IllegalArgumentException("Not a component identifier: " + idstring);
	}
	
	/**
	 *  Make the id generation deterministic or random. Default is random.
	 *  @param deterministic The deterministic flag.
	 */
	public static void setDeterministicNameGeneration(boolean deterministic)
	{
		gen = new IdGenerator(deterministic);
	}
	
	/**
	 *  Test main.
	 *  @param args Command-line args, unused. 
	 */
	public static void main(String[] args)
	{
		for (int i = 0; i < 10; ++i)
			System.out.println(new ComponentIdentifier());
	}
}
