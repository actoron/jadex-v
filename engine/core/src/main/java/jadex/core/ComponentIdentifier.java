package jadex.core;

import java.util.concurrent.atomic.AtomicLong;

import jadex.common.SUtil;
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
	
	/** The process/application -local name. */
	private String localname;
	
	/** The application id, if any.*/
	private String appid;
	
	/** Represents the globally identifiable process */
	private GlobalProcessIdentifier gpid;
	
	/**
	 *  Auto-generates a ComponentIdentifier.
	 *  @param app	The application the component belongs to,
	 *  			or null if the component is not part of an application.
	 */
	public ComponentIdentifier(Application app)
	{
		this(app, ComponentManager.get().isComponentIdNumberMode()? ""+ID_COUNTER.getAndIncrement(): gen.idStringFromNumber(ID_COUNTER.getAndIncrement()));
	}
	
	/**
	 *  Generates a ComponentIdentifier using a custom local ID.
	 *  
	 *  @param app	The application the component belongs to,
	 *  			or null if the component is not part of an application.
	 *  @param localname	Local identifier of the component.
	 */
	public ComponentIdentifier(Application app, String localname)
	{
		this(app, localname, GlobalProcessIdentifier.getSelf());
	}
	
	/**
	 *  Generates a ComponentIdentifier from its elements.
	 *  
	 *  @param app	The application the component belongs to,
	 *  			or null if the component is not part of an application.
	 *  @param localname	Local identifier of the component.
	 *  @param gpid The global process id.
	 */
	public ComponentIdentifier(Application app, String localname, GlobalProcessIdentifier gpid)
	{
		this.localname = localname;
		this.appid = app != null ? app.getId() : null;
		this.gpid = gpid;
	}
	
	/**
	 *  Generates a ComponentIdentifier from its elements.
	 *  
	 *  @param appid	The application id of the application the component belongs to,
	 *  				or null if the component is not part of an application.
	 *  @param localname	Local identifier of the component.
	 *  @param pid Process ID of the process on the host running the component
	 *  @param host Host running the process that is running the component
	 */
	public ComponentIdentifier(String appid, String localname, String pid, String host)
	{
		this.localname = localname;
		this.appid = appid;
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
	 *  Returns the application id.
	 *  @return The application id or null if the component is not part of an application.
	 */
	public String getAppId()
	{
		return appid;
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
		return GlobalProcessIdentifier.getSelf().equals(gpid);
	}
	
	/**
	 *  Generates a hashcode.
	 */
	public int hashCode()
	{
		return 13 * ((localname != null ? localname.hashCode() : 0) + (appid!=null ? appid.hashCode() : 0) + gpid.hashCode());
	}
	
	/**
	 *  Compares the ID.
	 */
	public boolean equals(Object obj)
	{
		if (obj instanceof ComponentIdentifier)
		{
			ComponentIdentifier other = (ComponentIdentifier) obj;
			return SUtil.equals(localname, other.localname) && SUtil.equals(appid, other.appid) && gpid.equals(other.gpid);
		}
		return false;
	}
	
	/**
	 *  Converts the ID to a unique String.
	 */
	public String toString()
	{
		return localname + (appid != null ? "@"+appid : "") + "@" + gpid.toString();
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
			return new ComponentIdentifier(null, "null".equals(splitstr[0]) ? null : splitstr[0], splitstr[1], splitstr[2]);
		}
		if (splitstr.length == 4)
		{
			return new ComponentIdentifier(splitstr[1], "null".equals(splitstr[0]) ? null : splitstr[0], splitstr[2], splitstr[3]);
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
		{
			ComponentIdentifier	cid	= new ComponentIdentifier(new Application("test"));
			System.out.println(cid +", "+fromString(cid.toString()));
		}
	}
}
