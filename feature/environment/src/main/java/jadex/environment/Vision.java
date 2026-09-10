package jadex.environment;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Vision
{
	protected Set<SpaceObject> seen;
	
	protected Set<SpaceObject> unseen;
	
	protected Set<SpaceObject> disappeared;

	public Vision() 
	{
	}
	
	public Vision(Set<SpaceObject> seen, Set<SpaceObject> unseen, Set<SpaceObject> disappeared) 
	{
	    this.seen = seen;
	    this.unseen = unseen;
	    this.disappeared = disappeared;
	}

	public Set<SpaceObject> getSeen() 
	{
		return seen==null? Collections.EMPTY_SET: seen;
	}

	public void setSeen(Set<SpaceObject> seen) 
	{
		this.seen = seen;
	}
	
	public Set<SpaceObject> getUnseen() 
	{
		return unseen==null? Collections.EMPTY_SET: unseen;
	}
	
	public void addSeen(SpaceObject so)
	{
		if(seen==null)
			seen = new HashSet<SpaceObject>();
		seen.add(so);
	}

	public void setUnseen(Set<SpaceObject> unseen) 
	{
		this.unseen = unseen;
	}
	
	public void addUnseen(SpaceObject so)
	{
		if(unseen==null)
			unseen = new HashSet<SpaceObject>();
		unseen.add(so);
	}

	public Set<SpaceObject> getDisappeared() 
	{
		return disappeared==null? Collections.EMPTY_SET: disappeared;
	}

	public void setDisappeared(Set<SpaceObject> disappeared) 
	{
		this.disappeared = disappeared;
	}
	
	public void addDisappeared(SpaceObject so)
	{
		if(disappeared==null)
			disappeared = new HashSet<SpaceObject>();
		disappeared.add(so);
	}

	@Override
	public String toString() 
	{
		return "Vision [seen=" + seen + ", unseen=" + unseen + ", disappeared=" + disappeared + "]";
	}
}