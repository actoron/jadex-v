package jadex.core;

public interface IComponentListener 
{
	public default void componentAdded(ComponentIdentifier cid)
	{
	}
	
	public default void componentRemoved(ComponentIdentifier cid)
	{
	}
	
	public default void lastComponentRemoved(ComponentIdentifier cid)
	{
	}
	
	public default void lastComponentRemoved(ComponentIdentifier cid, String appid)
	{
	}
}
