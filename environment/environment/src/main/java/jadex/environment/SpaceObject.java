package jadex.environment;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import jadex.common.SReflect;
import jadex.math.IVector2;
import jadex.math.Vector2Double;

public class SpaceObject 
{
	private static final Map<Class<?>, List<MethodHandle>> UPDATE_METHOD_CACHE = new ConcurrentHashMap<>();
	
	protected PropertyChangeSupport pcs;
	
	protected IVector2 position;

	protected String id;
	
	public SpaceObject() 
	{
	}
	
	public SpaceObject(IVector2 position) 
	{
		this.position = position;
	}

	public IVector2 getPosition() 
	{
		return position;
	}

	public void setPosition(IVector2 position) 
	{
		this.position = position;
		//System.out.println("pos is: "+id+" "+position);
	}

	public String getId() 
	{
		return id;
	}

	public void setId(String id) 
	{
		this.id = id;
	}
	
	public String getType()
	{
		return SReflect.getUnqualifiedClassName(this.getClass());
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
		SpaceObject other = (SpaceObject) obj;
		return Objects.equals(id, other.id);
	}

	@Override
	public String toString() 
	{
		return getClass().getSimpleName()+" [position=" + position + ", id=" + id + "]";
	}
	
	public SpaceObject copy()
	{
		SpaceObject ret = new SpaceObject(new Vector2Double(this.getPosition()));
		ret.setId(this.getId());
		return ret;
	}
	
	public final void updateFrom(SpaceObject source)
    {
        this.position = source.getPosition();
        this.id = source.getId();

        invokeAllOnUpdateFrom(source);

        fireObjectChange();
    }
	
	private void invokeAllOnUpdateFrom(SpaceObject source)
    {
        try 
        {
            for (MethodHandle mh : getUpdateMethods(this.getClass())) 
            {
                mh.invoke(this, source);
            }
        } 
        catch (Throwable e) 
        {
            throw new RuntimeException("Error invoking onUpdateFrom()", e);
        }
    }
	
	private static List<MethodHandle> getUpdateMethods(Class<?> clazz)
	{
	    return UPDATE_METHOD_CACHE.computeIfAbsent(clazz, key -> {
	        List<MethodHandle> handles = new ArrayList<>();

	        while (key != null && SpaceObject.class.isAssignableFrom(key))
	        {
	            try 
	            {
	                MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(key, MethodHandles.lookup()); // Fix: Dynamischer Lookup pro Klasse
	                
	                MethodHandle mh = lookup.findSpecial(
	                    key, "onUpdateFrom", 
	                    MethodType.methodType(void.class, SpaceObject.class),
	                    key
	                );
	                handles.add(mh);
	            } 
	            catch (NoSuchMethodException | IllegalAccessException ignored) 
	            {
	                // Falls die Klasse `onUpdateFrom()` nicht überschreibt, ignorieren
	            }
	            key = key.getSuperclass();
	        }

	        Collections.reverse(handles); // Basisklassen zuerst aufrufen
	        return handles;
	    });
	}
	
	protected void onUpdateFrom(SpaceObject source) 
    {
    }
	
	public void fireObjectChange()
	{
		if(pcs!=null)
			pcs.firePropertyChange("general", null, null);
	}
	
	public void addPropertyChangeListener(PropertyChangeListener listener) 
	{
		if(pcs==null)
			pcs = new PropertyChangeSupport(this);
		pcs.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) 
	{
		if(pcs==null)
			pcs = new PropertyChangeSupport(this);
		pcs.removePropertyChangeListener(listener);
	}
	
	/**
	 *  Override to print debug information on duplicate move task.
	 */
	public void debug()
	{
	}
}
