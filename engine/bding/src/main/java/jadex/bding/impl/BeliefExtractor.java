package jadex.bding.impl;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import jadex.bding.annotation.Belief;
import jadex.core.IComponent;

public class BeliefExtractor
{
    /**
     * Extract all @Belief fields from the agent POJO.
     */
    public static JsonObject extract(IComponent component)
    {
        Object pojo = component.getPojo();

        JsonObject ret = new JsonObject();

        extractFields(pojo, ret);

        return ret;
    }

    /**
     * Recursively extract @Belief fields from the POJO hierarchy.
     */
    protected static void extractFields(Object pojo, JsonObject target)
    {
        if(pojo == null)
            return;

        Class<?> clazz = pojo.getClass();

        while(clazz != null && clazz != Object.class)
        {
            for(Field field : clazz.getDeclaredFields())
            {
                if(!field.isAnnotationPresent(Belief.class))
                    continue;

                if(Modifier.isStatic(field.getModifiers()))
                    continue;

                try
                {
                    field.setAccessible(true);

                    Object value = field.get(pojo);

                    target.add(field.getName(), toJson(value));
                }
                catch(Exception e)
                {
                    throw new RuntimeException("Could not extract belief '"+field.getName()+"' from "+pojo.getClass().getName(), e);
                }
            }

            clazz = clazz.getSuperclass();
        }
    }

    /**
     * Convert a Java value into a minimal-json value.
     */
    protected static JsonValue toJson(Object value)
    {
        if(value == null)
            return Json.NULL;

        if(value instanceof JsonValue json)
            return json;

        if(value instanceof String str)
            return Json.value(str);

        if(value instanceof Character character)
            return Json.value(character.toString());

        if(value instanceof Boolean bool)
            return Json.value(bool);

        if(value instanceof Integer number)
            return Json.value(number);

        if(value instanceof Double number)
            return Json.value(number);

        if(value instanceof Float number)
            return Json.value(number);

        if(value instanceof Map<?, ?> map)
        {
            JsonObject ret = new JsonObject();

            for(Map.Entry<?, ?> entry : map.entrySet())
            {
                if(!(entry.getKey() instanceof String key))
                {
                    throw new RuntimeException(
                        "Belief map contains non-string key: "
                        +entry.getKey());
                }

                ret.add(key, toJson(entry.getValue()));
            }

            return ret;
        }

        if(value instanceof Collection<?> collection)
        {
            JsonArray ret = new JsonArray();

            for(Object element : collection)
            {
                ret.add(toJson(element));
            }

            return ret;
        }

        if(value.getClass().isArray())
        {
            JsonArray ret = new JsonArray();

            int length = Array.getLength(value);

            for(int i=0; i<length; i++)
            {
                ret.add(toJson(Array.get(value, i)));
            }

            return ret;
        }

        return beanToJson(value);
    }

    /**
     * Convert a simple Java bean / POJO into a JSON object.
     *
     * All non-static fields are included.
     */
    protected static JsonObject beanToJson(Object bean)
    {
        JsonObject ret = new JsonObject();

        Class<?> clazz = bean.getClass();

        while(clazz != null && clazz != Object.class)
        {
            for(Field field : clazz.getDeclaredFields())
            {
                if(Modifier.isStatic(field.getModifiers()))
                    continue;

                try
                {
                    field.setAccessible(true);

                    Object value = field.get(bean);

                    ret.add(field.getName(), toJson(value));
                }
                catch(Exception e)
                {
                    throw new RuntimeException("Could not convert field '"+field.getName()+"' of "+bean.getClass().getName()+" to JSON", e);
                }
            }

            clazz = clazz.getSuperclass();
        }

        return ret;
    }

    public static void inject(IComponent component, JsonObject beliefs)
    {
        Object pojo = component.getPojo();

        injectFields(pojo, beliefs);
    }

    protected static void injectFields(Object pojo, JsonObject source)
    {
        if(pojo == null)
            return;

        Class<?> clazz = pojo.getClass();

        while(clazz != null && clazz != Object.class)
        {
            for(Field field : clazz.getDeclaredFields())
            {
                if(!field.isAnnotationPresent(Belief.class))
                    continue;

                if(Modifier.isStatic(field.getModifiers()))
                    continue;

                String name = field.getName();

                if(!source.names().contains(name))
                    continue;

                try
                {
                    field.setAccessible(true);

                    JsonValue value = source.get(name);

                    Object converted = fromJson(value, field.getType());

                    field.set(pojo, converted);
                }
                catch(Exception e)
                {
                    throw new RuntimeException(
                        "Could not inject belief '" + name
                        + "' into " + pojo.getClass().getName(), e);
                }
            }

            clazz = clazz.getSuperclass();
        }
    }

    protected static Object fromJson(JsonValue value, Class<?> type)
    {
        if(value.isNull())
            return null;

        if(type == String.class)
            return value.asString();

        if(type == boolean.class || type == Boolean.class)
            return value.asBoolean();

        if(type == int.class || type == Integer.class)
            return value.asInt();

        if(type == long.class || type == Long.class)
            return value.asLong();

        if(type == double.class || type == Double.class)
            return value.asDouble();

        if(type == float.class || type == Float.class)
            return (float)value.asDouble();

        if(type == JsonValue.class)
            return value;

        throw new RuntimeException(
            "Unsupported belief type: " + type.getName());
    }
}