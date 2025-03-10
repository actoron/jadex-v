package jadex.environment;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
}