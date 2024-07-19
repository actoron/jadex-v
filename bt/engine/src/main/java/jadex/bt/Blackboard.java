package jadex.bt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import jadex.common.Tuple2;

public class Blackboard 
{
	public static String TYPE_BLACKBOARD_VALUE_CHANGED = "blackboard_value_changed";
	
    protected final Map<String, Object> data = new HashMap<>();
    protected final Map<String, List<Consumer<Event>>> listeners = new HashMap<>();

    public void set(String key, Object value) 
    {
        data.put(key, value);
        notifyListeners(key, value);
    }

    public Object get(String key) 
    {
        return data.get(key);
    }

    public void addListener(String key, Consumer<Event> listener) 
    {
        listeners.computeIfAbsent(key, k -> new ArrayList<>()).add(listener);
    }
    
    public void removeListener(String key, Consumer<Object> listener)
    {
    	List<Consumer<Event>> lis = listeners.get(key);
    	if(lis!=null)
    		lis.remove(listener);
    }

    protected void notifyListeners(String key, Object value) 
    {
        if(listeners.containsKey(key)) 
        {
            for(Consumer<Event> listener : listeners.get(key)) 
            {
                listener.accept(new Event(TYPE_BLACKBOARD_VALUE_CHANGED, new Tuple2<String, Object>(key, value)));
            }
        }
    }
}
