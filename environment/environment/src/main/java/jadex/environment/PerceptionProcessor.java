package jadex.environment;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class PerceptionProcessor 
{
    public enum PerceptionState 
    {
        SEEN, DISAPPEARED, UNSEEN, CHANGED
    }

    private final Map<PerceptionState, Map<Class<? extends SpaceObject>, Consumer<SpaceObject>>> handlers = new HashMap<>();

    public PerceptionProcessor() 
    {
        for (PerceptionState state : PerceptionState.values()) 
            handlers.put(state, new HashMap<>());
    }

    @SafeVarargs
    public final <T extends SpaceObject> PerceptionProcessor registerHandler(Class<T> type, Consumer<T> handler, PerceptionState... states)
    {
        for (PerceptionState state : states) 
            handlers.get(state).put(type, obj -> handler.accept(type.cast(obj)));
        return this;
    }
    

    public void handleObject(PerceptionState state, SpaceObject obj) 
    {
    	findBestHandler(state, obj.getClass()).accept(obj);                               
    }
    
    public void handleEvent(EnvironmentEvent e) 
    {
        if (e instanceof VisionEvent) 
        {
            VisionEvent ve = (VisionEvent) e;
            ve.getVision().getSeen().forEach(obj -> handleObject(PerceptionState.SEEN, obj));
            ve.getVision().getDisappeared().forEach(obj -> handleObject(PerceptionState.DISAPPEARED, obj));
            ve.getVision().getUnseen().forEach(obj -> handleObject(PerceptionState.UNSEEN, obj));
        } 
        else if (e instanceof SpaceObjectsEvent) 
        {
            SpaceObjectsEvent soe = (SpaceObjectsEvent) e;
            soe.getObjects().forEach(obj -> handleObject(PerceptionState.CHANGED, obj));
        }
    }
    
    private Consumer<SpaceObject> findBestHandler(PerceptionState state, Class<?> clazz) 
    {
    	Map<Class<? extends SpaceObject>, Consumer<SpaceObject>> shandlers = handlers.getOrDefault(state, Collections.emptyMap());
        while (clazz != null) 
        {
            Consumer<SpaceObject> handler = shandlers.get(clazz);
            if (handler != null) 
                return handler;
            else
            	System.out.println("No handler found for: "+clazz);
            clazz = clazz.getSuperclass(); 
        }
        return o -> {};
    }
    
    public <T extends SpaceObject> void manage(Class<T> clazz, Collection<T> coll)
    {
    	manage(clazz, coll, 
    		obj -> findAndUpdateOrAdd(obj, coll), 
    		obj -> coll.remove(obj),
    		obj -> {if(obj.getPosition()==null) {coll.remove(obj); 
    			//System.out.println("removing: "+obj+" from "+coll);
    		}}
    	);
    	
    	//registerHandler(clazz, obj -> findAndUpdateOrAdd(obj, coll), PerceptionState.SEEN, PerceptionState.CHANGED);
        //registerHandler(clazz, obj -> coll.remove(obj), PerceptionState.DISAPPEARED);
        //registerHandler(clazz, obj -> {if(obj.getPosition()==null) coll.remove(obj);}, PerceptionState.UNSEEN);
    }
    
    public <T extends SpaceObject> void manage(Class<T> clazz, Collection<T> coll, Consumer<T> seenhandler, Consumer<T> dishandler, Consumer<T> unseenhandler)
    {
    	registerHandler(clazz, seenhandler, PerceptionState.SEEN, PerceptionState.CHANGED);
        registerHandler(clazz, dishandler, PerceptionState.DISAPPEARED);
        registerHandler(clazz, unseenhandler, PerceptionState.UNSEEN);
    }
    
    public static void findAndUpdateOrAdd(SpaceObject obj, Collection<?> coll)
	{
		Collection<SpaceObject> sos = (Collection<SpaceObject>)coll;
		boolean found = false;
		    
	    for (SpaceObject item : sos) 
	    {
	        if (item.equals(obj)) 
	        {
	        	item.updateFrom(obj);
	            //spaceObjects.remove(item);
	            //spaceObjects.add(obj);
	            found = true;
	            break; 
	        }
	    }
	    
	    if (!found) 
	        sos.add(obj);
	}
}