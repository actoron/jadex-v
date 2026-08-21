package jadex.bding.impl;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class JsonHelper 
{
   
    public static JsonValue toJson(Object value)
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

        if(value instanceof Number number)
        {
            if(value instanceof Byte
                || value instanceof Short
                || value instanceof Integer
                || value instanceof Long)
            {
                return Json.value(number.longValue());
            }

            return Json.value(number.doubleValue());
        }

        if(value instanceof Map<?, ?> map)
        {
            JsonObject ret = new JsonObject();

            for(Map.Entry<?, ?> entry : map.entrySet())
            {
                if(!(entry.getKey() instanceof String key))
                {
                    throw new RuntimeException(
                        "Belief map contains non-string key: "
                        + entry.getKey());
                }

                ret.add(key, toJson(entry.getValue()));
            }

            return ret;
        }

        if(value instanceof Iterable<?> iterable)
        {
            JsonArray ret = new JsonArray();

            for(Object element : iterable)
                ret.add(toJson(element));

            return ret;
        }

        if(value.getClass().isArray())
        {
            JsonArray ret = new JsonArray();

            int length = Array.getLength(value);

            for(int i = 0; i < length; i++)
            {
                ret.add(toJson(Array.get(value, i)));
            }

            return ret;
        }

        return beanToJson(value);
    }

    public static JsonObject beanToJson(Object bean)
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

                    ret.add(field.getName(), toJson(field.get(bean)));
                }
                catch(Exception e)
                {
                    throw new RuntimeException("Could not convert field '" + field.getName() + "' of " + bean.getClass().getName() + " to JSON", e);
                }
            }

            clazz = clazz.getSuperclass();
        }

        return ret;
    }

    public static <T> T jsonToObject(JsonValue value, Class<T> type)
    {
        if(value == null || value.isNull())
            return null;

        if(type == JsonValue.class)
            return type.cast(value);

        if(type == String.class)
            return type.cast(value.asString());

        if(type == boolean.class || type == Boolean.class)
            return type.cast(value.asBoolean());

        if(type == byte.class || type == Byte.class)
            return type.cast((byte)value.asInt());

        if(type == short.class || type == Short.class)
            return type.cast((short)value.asInt());

        if(type == int.class || type == Integer.class)
            return type.cast(value.asInt());

        if(type == long.class || type == Long.class)
            return type.cast(value.asLong());

        if(type == float.class || type == Float.class)
            return type.cast((float)value.asDouble());

        if(type == double.class || type == Double.class)
            return type.cast(value.asDouble());

        if(type == char.class || type == Character.class)
            return type.cast(value.asString().charAt(0));

        if(type.isEnum())
        {
            @SuppressWarnings({"unchecked", "rawtypes"})
            T ret = (T)Enum.valueOf(
                (Class<? extends Enum>)type,
                value.asString());

            return ret;
        }

        if(type.isArray())
        {
            JsonArray array = value.asArray();

            Class<?> componentType = type.getComponentType();
            Object ret = Array.newInstance(componentType, array.size());

            for(int i = 0; i < array.size(); i++)
            {
                Array.set(ret, i, jsonToObject(array.get(i), componentType));
            }

            return type.cast(ret);
        }

        if(Map.class.isAssignableFrom(type))
        {
            JsonObject object = value.asObject();

            Map<String, Object> ret = new LinkedHashMap<>();

            for(String name : object.names())
            {
                ret.put(name, jsonToObject(object.get(name), Object.class));
            }

            return type.cast(ret);
        }

        if(Iterable.class.isAssignableFrom(type))
        {
            JsonArray array = value.asArray();

            List<Object> ret = new ArrayList<>();

            for(JsonValue item : array)
                ret.add(jsonToObject(item, Object.class));

            return type.cast(ret);
        }

        return jsonToBean(value.asObject(), type);
    }

    public static <T> T jsonToBean(JsonObject object, Class<T> type)
    {
        try
        {
            T bean = type.getDeclaredConstructor().newInstance();

            Class<?> clazz = type;

            while(clazz != null && clazz != Object.class)
            {
                for(Field field : clazz.getDeclaredFields())
                {
                    if(Modifier.isStatic(field.getModifiers()))
                        continue;

                    if(!object.names().contains(field.getName()))
                        continue;

                    field.setAccessible(true);

                    Object value = jsonToObject(
                        object.get(field.getName()),
                        field.getType());

                    field.set(bean, value);
                }

                clazz = clazz.getSuperclass();
            }

            return bean;
        }
        catch(Exception e)
        {
            throw new RuntimeException("Could not convert JSON to " + type.getName(), e);
        }
    }
}
