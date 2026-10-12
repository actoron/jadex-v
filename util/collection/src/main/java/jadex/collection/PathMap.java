package jadex.collection;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A map that supports dotted hierarchical keys.
 *
 * Example:
 *
 *     map.put("plan.userInput", "hello");
 *     map.put("goal.secretPerson", "Einstein");
 *     map.put("loop.gameLoop.counter", 3);
 *
 * Internally this creates:
 *
 *     plan -> { userInput -> "hello" }
 *     goal -> { secretPerson -> "Einstein" }
 *     loop -> { gameLoop -> { counter -> 3 } }
 *
 * Both hierarchical and path-based access are supported:
 *
 *     map.get("plan.userInput")
 *     map.get("plan")
 */
public class PathMap extends LinkedHashMap<String, Object>
{
    public PathMap()
    {
    }

    public PathMap(Map<String, ?> values)
    {
        putAll(values);
    }

    @Override
    public Object put(String key, Object value)
    {
        if(key == null)
            throw new NullPointerException("key");

        if(key.isEmpty())
            return super.put(key, value);

        int dot = key.indexOf('.');

        if(dot < 0)
            return putLocal(key, value);

        String head = key.substring(0, dot);
        String tail = key.substring(dot + 1);

        if(tail.isEmpty())
            throw new IllegalArgumentException("Invalid nested key: " + key);

        Object child = super.get(head);

        if(child == null)
        {
            child = new PathMap();
            super.put(head, child);
        }
        else if(!(child instanceof Map))
        {
            throw new IllegalArgumentException("Cannot create nested key '" + key + "': '" + head + "' already contains a value.");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> childMap = (Map<String, Object>)child;

        if(childMap instanceof NestedMap)
            childMap.put(tail, value);
        else
            putNested(childMap, tail, value);

        return value;
    }

    protected Object putLocal(String key, Object value)
    {
        Object old = super.get(key);

        if(old instanceof Map && !(value instanceof Map))
            throw new IllegalArgumentException("Cannot replace nested map '" + key + "' with a value.");

        if(old != null && !(old instanceof Map) && value instanceof Map)
            throw new IllegalArgumentException("Cannot replace value '" + key + "' with a nested map.");

        return super.put(key, value);
    }

    protected void putNested(Map<String, Object> map, String key, Object value)
    {
        int dot = key.indexOf('.');

        if(dot < 0)
        {
            Object old = map.get(key);

            if(old instanceof Map && !(value instanceof Map))
                throw new IllegalArgumentException(
                    "Cannot replace nested map '" + key + "' with a value.");

            if(old != null && !(old instanceof Map) && value instanceof Map)
                throw new IllegalArgumentException(
                    "Cannot replace value '" + key + "' with a nested map.");

            map.put(key, value);
            return;
        }

        String head = key.substring(0, dot);
        String tail = key.substring(dot + 1);

        Object child = map.get(head);

        if(child == null)
        {
            child = new PathMap();
            map.put(head, child);
        }
        else if(!(child instanceof Map))
        {
            throw new IllegalArgumentException("Cannot create nested key '" + key + "': '" + head + "' already contains a value.");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> childMap = (Map<String, Object>)child;

        putNested(childMap, tail, value);
    }

    @Override
    public Object get(Object key)
    {
        if(!(key instanceof String))
            return super.get(key);

        String path = (String)key;

        if(path.isEmpty() || path.indexOf('.') < 0)
            return super.get(path);

        String[] parts = path.split("\\.");

        Object current = super.get(parts[0]);

        for(int i = 1; i < parts.length && current != null; i++)
        {
            if(!(current instanceof Map))
                return null;

            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>)current;

            current = map.get(parts[i]);
        }

        return current;
    }

    @Override
    public boolean containsKey(Object key)
    {
        if(!(key instanceof String))
            return super.containsKey(key);

        String path = (String)key;

        if(path.isEmpty() || path.indexOf('.') < 0)
            return super.containsKey(path);

        String[] parts = path.split("\\.");

        Object current = super.get(parts[0]);

        for(int i = 1; i < parts.length && current != null; i++)
        {
            if(!(current instanceof Map))
                return false;

            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>)current;

            if(i == parts.length - 1)
                return map.containsKey(parts[i]);

            current = map.get(parts[i]);
        }

        return current != null;
    }

    @Override
    public Object remove(Object key)
    {
        if(!(key instanceof String))
            return super.remove(key);

        String path = (String)key;

        if(path.isEmpty() || path.indexOf('.') < 0)
            return super.remove(path);

        String[] parts = path.split("\\.");

        Object current = super.get(parts[0]);

        if(current == null)
            return null;

        for(int i = 1; i < parts.length - 1; i++)
        {
            if(!(current instanceof Map))
                return null;

            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>)current;

            current = map.get(parts[i]);
        }

        if(!(current instanceof Map))
            return null;

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>)current;

        return map.remove(parts[parts.length - 1]);
    }
}