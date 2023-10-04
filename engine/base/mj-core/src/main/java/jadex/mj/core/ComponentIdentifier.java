package jadex.mj.core;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import jadex.mj.core.impl.Words;

/**
 *  Identifier for components.
 */
public class ComponentIdentifier
{
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
		this(idStringFromNumber(ID_COUNTER.getAndIncrement()), PID, HOST);
	}
	
	/**
	 *  Generates a ComponentIdentifier using a custom local ID.
	 *  
	 *  @param localid Local identifier of the component.
	 */
	public ComponentIdentifier(String localname)
	{
		this(localname, PID, HOST);
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
		localname = idStringFromNumber(ID_COUNTER.getAndIncrement());
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
	 *  Generate a String id from a number, used for auto-generating.
	 *  
	 *  @param num A number.
	 *  @return A String ID.
	 */
	protected static final String idStringFromNumber(long num)
	{
		int numval = (int) (num >>> 20 & 0xFFFL);
		//numval = numval * 4253 & 0xFFF; // 4253 is prime
		
		long low20val = num & 0xFFFFFL;
		low20val = low20val * 1049639L & 0xFFFFFL; // 1049639 is prime
		
		String ret = String.join("", adjectives4[(int) ((num >>> 52) & 0x3FFL)],
									 adjectives3[(int) ((num >>> 42) & 0x3FFL)],
									 adjectives2[(int) ((num >>> 32) & 0x3FFL)],
									 adjectives1[(int) ((low20val >>> 10) & 0x3FFL)],
									 nouns[(int) (low20val & 0x3FFL)],
									 numval > 0? ("_" + Integer.toHexString(numval)) : "");
		return ret;
	}
	
	/** Cached process ID. */
	private static final long PID = ProcessHandle.current().pid();
	
	/** Cached host name. */
	private static final String HOST;
	static
	{
		String host = "UNKNOWN";
		try
		{
			// Probably needs something more clever like obtaining the main IP address.
			InetAddress localhost = InetAddress.getLocalHost();
			host = localhost.getHostName();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		System.out.println("GOT THE HOST " + host);
		HOST = host;
	}
	
	/** Counter for auto-generated local IDs */
	private static final AtomicLong ID_COUNTER = new AtomicLong();
	
	/** Adjectives for auto-generated local IDs */
	private static final String[] adjectives1;
	
	/** Adjectives for auto-generated local IDs */
	private static final String[] adjectives2;
	
	/** Adjectives for auto-generated local IDs */
	private static final String[] adjectives3;
	
	/** Adjectives for auto-generated local IDs */
	private static final String[] adjectives4;
	
	/** Nounss for auto-generated local IDs */
	private static final String[] nouns;
	
	/** Initialize words. */
	static
	{
		//long seed = 208612059;
		//Random r = new Random(seed);
		Random r = new Random();
		List<String> tmplist = new ArrayList<>(Arrays.asList(Arrays.copyOf(Words.ADJECTIVES, 1023)));
		Collections.shuffle(tmplist, r);
		tmplist.add(0, "Clever");
		adjectives1 = tmplist.toArray(new String[1024]);
		
		//seed = 265266541;
		//r = new Random(seed);
		tmplist = new ArrayList<>(Arrays.asList(Arrays.copyOf(Words.ADJECTIVES, 1023)));
		Collections.shuffle(tmplist, r);
		tmplist.add(0, "");
		adjectives2 = tmplist.toArray(new String[1024]);
		
		//seed = 786761336;
		//r = new Random(seed);
		tmplist = new ArrayList<>(Arrays.asList(Arrays.copyOf(Words.ADJECTIVES, 1023)));
		Collections.shuffle(tmplist, r);
		tmplist.add(0, "");
		adjectives3 = tmplist.toArray(new String[1024]);
		
		//seed = 384957210;
		//r = new Random(seed);
		tmplist = new ArrayList<>(Arrays.asList(Arrays.copyOf(Words.ADJECTIVES, 1023)));
		Collections.shuffle(tmplist, r);
		tmplist.add(0, "");
		adjectives4 = tmplist.toArray(new String[1024]);
		
		//seed = 292305523;
		//r = new Random(seed);
		tmplist = Arrays.asList(Arrays.copyOf(Words.NOUNS, 1024));
		Collections.shuffle(tmplist, r);
		nouns = tmplist.toArray(new String[1024]);
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
