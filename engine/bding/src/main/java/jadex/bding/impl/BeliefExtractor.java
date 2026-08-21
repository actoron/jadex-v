package jadex.bding.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

import jadex.bding.annotation.Belief;
import jadex.core.IComponent;

public class BeliefExtractor
{
    /**
     * Extract all @Belief fields from the agent POJO.
     *
     * The returned snapshot contains the actual Java objects.
     */
    public static BeliefSnapshot extract(IComponent component)
    {
        Object pojo = component.getPojo();

        Map<String, Object> beliefs = new LinkedHashMap<>();

        extractFields(pojo, beliefs);

        return new BeliefSnapshot(beliefs);
    }

    /**
     * Extract @Belief fields from the POJO hierarchy.
     */
    protected static void extractFields(Object pojo, Map<String, Object> target)
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

                    target.put(field.getName(), value);
                }
                catch(Exception e)
                {
                    throw new RuntimeException(
                        "Could not extract belief '"
                        + field.getName()
                        + "' from "
                        + pojo.getClass().getName(), e);
                }
            }

            clazz = clazz.getSuperclass();
        }
    }

    /**
     * Inject a belief snapshot into the agent POJO.
     *
     * The snapshot contains actual Java objects, so no conversion
     * through JSON is necessary.
     */
    public static void inject(IComponent component, BeliefSnapshot beliefs)
    {
        Object pojo = component.getPojo();

        injectFields(pojo, beliefs.getBeliefs());
    }

    /**
     * Inject belief values into the @Belief fields of the POJO hierarchy.
     */
    protected static void injectFields(Object pojo, Map<String, Object> source)
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

                if(!source.containsKey(name))
                    continue;

                try
                {
                    field.setAccessible(true);

                    Object value = source.get(name);

                    if(value != null
                        && !isAssignable(field.getType(), value.getClass()))
                    {
                        throw new IllegalArgumentException("Value of type "+ value.getClass().getName()
                            + " cannot be assigned to belief field "+ name + " of type " + field.getType().getName());
                    }

                    field.set(pojo, value);
                }
                catch(Exception e)
                {
                    throw new RuntimeException("Could not inject belief '"+ name+ "' into "+ pojo.getClass().getName(), e);
                }
            }

            clazz = clazz.getSuperclass();
        }
    }

    /**
     * Check Java assignment compatibility including primitive types.
     */
    protected static boolean isAssignable(Class<?> target, Class<?> source)
    {
        if(target.isAssignableFrom(source))
            return true;

        if(!target.isPrimitive())
            return false;

        return (target == boolean.class && source == Boolean.class)
            || (target == byte.class && source == Byte.class)
            || (target == short.class && source == Short.class)
            || (target == int.class && source == Integer.class)
            || (target == long.class && source == Long.class)
            || (target == float.class && source == Float.class)
            || (target == double.class && source == Double.class)
            || (target == char.class && source == Character.class);
    }
}