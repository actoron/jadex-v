package jadex.rules.eca.propertychange;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import jadex.common.IResultCommand;
import jadex.common.SReflect;
import jadex.future.IFuture;
import jadex.future.IResultListener;
import jadex.rules.eca.ChangeInfo;
import jadex.rules.eca.Event;
import jadex.rules.eca.EventType;
import jadex.rules.eca.IEvent;

/**
 *  Basic property change manager.
 */
public class PropertyChangeManager
{
	/** The event list. */
	protected List<IEvent> events;
	
	/** The property change listeners. */
	protected Map<Object, Map<IResultCommand<IFuture<Void>, PropertyChangeEvent>, PropertyChangeListener>> pcls;
	
	/** The argument types for property change listener adding/removal (cached for speed). */
	protected static Class<?>[]	PCL	= new Class[]{PropertyChangeListener.class};

	/** Protected Constructor to prevent direct instantiation **/
	public  PropertyChangeManager()
	{
		this.events = new ArrayList<IEvent>();
	}
		
	/**
	 *  Remove a listener from an object.
	 */
	protected void removePCL(Object object, PropertyChangeListener pcl)
	{
		if(pcl!=null)
		{
			try
			{
//				System.out.println(getTypeModel().getName()+": Deregister: "+value+", "+type);						
				// Do not use Class.getMethod (slow).
				Method	meth = SReflect.getMethod(object.getClass(), "removePropertyChangeListener", PCL);
				if(meth!=null)
					meth.invoke(object, new Object[]{pcl});
			}
			catch(IllegalAccessException e){e.printStackTrace();}
			catch(InvocationTargetException e){e.printStackTrace();}
		}
	}
	
	/**  
	 *  Add a property change listener.
	 */
	public void	addPropertyChangeListener(Object object, IResultCommand<IFuture<Void>, PropertyChangeEvent> eventadder)
	{
		if(object!=null)
		{
			// Invoke addPropertyChangeListener on value
			try
			{
				Method	meth = getAddMethod(object);
				if(meth!=null)
				{
					if(pcls==null)
						pcls = new IdentityHashMap<Object, Map<IResultCommand<IFuture<Void>, PropertyChangeEvent>, PropertyChangeListener>>(); // values may change, therefore identity hash map
					Map<IResultCommand<IFuture<Void>, PropertyChangeEvent>, PropertyChangeListener> mypcls = pcls.get(object);
					PropertyChangeListener pcl = mypcls==null? null: mypcls.get(eventadder);
					
					if(pcl==null)
					{
						pcl = createPCL(meth, eventadder);
						if(mypcls==null)
						{
							mypcls = new IdentityHashMap<IResultCommand<IFuture<Void>, PropertyChangeEvent>, PropertyChangeListener>();
							pcls.put(object, mypcls);
						}
						
						mypcls.put(eventadder, pcl);
					}
					
					meth.invoke(object, new Object[]{pcl});	
				}
			}
			catch(IllegalAccessException e){e.printStackTrace();}
			catch(InvocationTargetException e){e.printStackTrace();}
		}
	}
	
	/**
	 *  Deregister a value for observation.
	 *  if its a bean then remove the property listener.
	 */
	public void	removePropertyChangeListener(Object object, IResultCommand<IFuture<Void>, PropertyChangeEvent> eventadder)
	{
		if(object!=null)
		{
//			System.out.println("deregister ("+cnt[0]+"): "+value);
			// Stop listening for bean events.
			if(pcls!=null)
			{
				Map<IResultCommand<IFuture<Void>, PropertyChangeEvent>, PropertyChangeListener> mypcls = pcls.get(object);
				if(mypcls!=null)
				{
					if(eventadder!=null)
					{
						PropertyChangeListener pcl = mypcls.remove(eventadder);
						removePCL(object, pcl);
					}
					else
					{
						for(PropertyChangeListener pcl: mypcls.values())
						{
							removePCL(object, pcl);
						}
						mypcls.clear();
					}
					if(mypcls.size()==0)
						pcls.remove(object);
				}
			}
		}
	}
	
	/**
	 *  Add an event.
	 */
	public void addEvent(IEvent event)
	{
		events.add(event);
	}

	/**
	 *  Test if events are available.
	 *  @return True, if has events.
	 */
	public boolean hasEvents()
	{
		return events.size()>0;
	}

	/**
	 *  Remove an event.
	 *  @param index The index.
	 */
	public IEvent removeEvent(int index)
	{
		return events.remove(index);
	}
	
	/**
	 *  Get the number of events. 
	 *  @return The number of events.
	 */
	public int getSize()
	{
		return events.size();
	}
	
	// ---- Helper -----
	
	/**
	 *  Create a listener.
	 */
	protected PropertyChangeListener createPCL(Method meth, final IResultCommand<IFuture<Void>, PropertyChangeEvent> eventadder)
	{
		return new PropertyChangeListener()
		{
			public void propertyChange(PropertyChangeEvent evt)
			{
				// todo: problems:
				// - may be called on wrong thread (-> synchronizator)
				// - how to create correct event with type and value

				if(eventadder!=null)
				{
					eventadder.execute(evt).addResultListener(new IResultListener<Void>()
					{
						public void resultAvailable(Void event)
						{
//							if(event!=null)
//							{
//								addEvent(event);
//							}
						}
						
						public void exceptionOccurred(Exception exception)
						{
							System.out.println("Event creator had exception: "+exception);
						}
					});
				}
				else
				{
					Event event = new Event(new EventType(evt.getPropertyName()), new ChangeInfo<Object>(evt.getNewValue(), evt.getOldValue(), null));
					addEvent(event);
				}
			}
		};
	}

	/**
	 *  Get listener add method
	 */
	protected Method	getAddMethod(Object object)
	{
		// Do not use Class.getMethod (slow).
		Method	meth = SReflect.getMethod(object.getClass(), "addPropertyChangeListener", PCL);
		return meth;
	}
}
