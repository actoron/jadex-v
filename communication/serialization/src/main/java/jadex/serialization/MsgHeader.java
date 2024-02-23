package jadex.serialization;

import java.util.HashMap;
import java.util.Map;

import jadex.core.ComponentIdentifier;

/**
 *   Message header with message meta information.
 *
 */
public class MsgHeader implements IMsgHeader
{
	
	/** Map containing properties. */
	protected Map<String, Object> properties;
	
	/**
	 *  Creates the header.
	 */
	public MsgHeader()
	{
	}
	
	/**
	 *  Gets the sender of the message.
	 * 
	 *  @return The sender.
	 */
	public ComponentIdentifier getSender()
	{
		return (ComponentIdentifier)getProperty(IMsgHeader.SENDER);
	}
	
	/**
	 *  Gets the receiver of the message.
	 * 
	 *  @return The receiver.
	 */
	public ComponentIdentifier getReceiver()
	{
		return (ComponentIdentifier)getProperty(IMsgHeader.RECEIVER);
	}
	
	/**
	 *  Gets a property stored in the header.
	 *  
	 *  @param propertyname The name of the property.
	 *  @return Property value.
	 */
	public Object getProperty(String propertyname)
	{
		return properties.get(propertyname);
	}

	/**
	 *  Gets the properties.
	 *
	 *  @return The properties map.
	 */
	public Map<String, Object> getProperties()
	{
		return properties;
	}

	/**
	 *  Sets the properties.
	 *
	 *  @param properties The properties map.
	 */
	public void setProperties(Map<String, Object> properties)
	{
		this.properties = properties;
	}
	
	/**
	 *  Adds a header property to the header.
	 *  
	 *  @param propname The property name.
	 *  @param propval The property value.
	 */
	public void addProperty(String propname, Object propval)
	{
		if (properties == null)
			properties = new HashMap<String, Object>();
		properties.put(propname, propval);
	}

	/**
	 *  Get the string rep.
	 */
	public String toString()
	{
		return "MsgHeader(properties=" + properties + ")";
	}
	
}
