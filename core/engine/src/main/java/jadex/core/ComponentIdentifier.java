package jadex.core;

import java.util.concurrent.atomic.AtomicLong;

import jadex.core.annotation.NoCopy;
import jadex.core.impl.ComponentManager;
import jadex.core.impl.GlobalProcessIdentifier;
import jadex.idgenerator.IdGenerator;

/**
 *  Identifier for components.
 */
@NoCopy	// object is immutable -> no copy necessary when used in component or service methods
public class ComponentIdentifier
{
	protected static IdGenerator gen = new IdGenerator();
	
	/** Counter for auto-generated local IDs */
	private static final AtomicLong ID_COUNTER = new AtomicLong();
	
	/** The process-local name. */
	private String localname;
	
	/** Represents the globally identifiable process */
	private GlobalProcessIdentifier gpid;
	
	/**
	 *  Auto-generates a ComponentIdentifier.
	 *  
	 */
	public ComponentIdentifier()
	{
		this.localname = ComponentManager.get().isComponentIdNumberMode()? ""+ID_COUNTER.getAndIncrement(): gen.idStringFromNumber(ID_COUNTER.getAndIncrement());
		gpid = GlobalProcessIdentifier.SELF;
	}
	
	/**
	 *  Generates a ComponentIdentifier using a custom local ID.
	 *  
	 *  @param localid Local identifier of the component.
	 */
	public ComponentIdentifier(String localname)
	{
		this.localname = localname;
		gpid = GlobalProcessIdentifier.SELF;
	}
	
	/**
	 *  Generates a ComponentIdentifier from its elements.
	 *  
	 *  @param localid Local identifier of the component.
	 *  @param gpid The global process id.
	 */
	public ComponentIdentifier(String localname, GlobalProcessIdentifier gpid)
	{
		this.localname = localname;
		this.gpid = gpid;
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
		gpid = new GlobalProcessIdentifier(pid, host);
	}
	
	/**
	 *  Returns the local component id.
	 *  @return The local component id.
	 */
	public String getLocalName()
	{
		return localname;
	}
	
	/**
	 *  Returns the global process identifier.
	 *  @return The global process identifier.
	 */
	public GlobalProcessIdentifier getGlobalProcessIdentifier()
	{
		return gpid;
	}
	
	/**
	 *  Returns if the component runs on the local JVM.
	 *  @return True, if the component runs on the local JVM.
	 */
	public boolean isLocal()
	{
		return GlobalProcessIdentifier.SELF.equals(gpid);
	}
	
	/**
	 *  Generates a hashcode.
	 */
	public int hashCode()
	{
		return 13 * (localname.hashCode() + gpid.hashCode());
	}
	
	/**
	 *  Compares the ID.
	 */
	public boolean equals(Object obj)
	{
		if (obj instanceof ComponentIdentifier)
		{
			ComponentIdentifier other = (ComponentIdentifier) obj;
			return localname.equals(other.localname) && gpid.equals(other.gpid);
		}
		return false;
	}
	
	/**
	 *  Converts the ID to a unique String.
	 */
	public String toString()
	{
		return localname + "@" + gpid.toString();
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
