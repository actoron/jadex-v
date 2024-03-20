package jadex.micro.messagequeue;

import jadex.core.ComponentIdentifier;

/**
 *  Simple message struct.
 */
public class Event
{
	//-------- attributes --------
	
	/** The type. */
	protected String type;
	
	/** The value. */
	protected Object value;
	
	/** The source. */
	protected ComponentIdentifier source;

	//-------- constructors --------

	/**
	 *  Create a new event.
	 */
	public Event()
	{
	}

	/**
	 *  Create a new event.
	 */
	public Event(String type, Object value, ComponentIdentifier source)
	{
		this.type = type;
		this.value = value;
		this.source = source;
	}

	//-------- methods --------
	
	/**
	 *  Get the type.
	 *  @return The type.
	 */
	public String getType()
	{
		return type;
	}

	/**
	 *  Set the type.
	 *  @param type The type to set.
	 */
	public void setType(String type)
	{
		this.type = type;
	}

	/**
	 *  Get the value.
	 *  @return The value.
	 */
	public Object getValue()
	{
		return value;
	}

	/**
	 *  Set the value.
	 *  @param value The value to set.
	 */
	public void setValue(Object value)
	{
		this.value = value;
	}

	/**
	 *  Get the source.
	 *  @return The source.
	 */
	public ComponentIdentifier getSource()
	{
		return source;
	}

	/**
	 *  Set the source.
	 *  @param source The source to set.
	 */
	public void setSource(ComponentIdentifier source)
	{
		this.source = source;
	}

	/**
	 *  Get the string representation.
	 */
	public String toString()
	{
		return "Event(type=" + type + ", value=" + value + ", source="+ source + ")";
	}
	
	
}
