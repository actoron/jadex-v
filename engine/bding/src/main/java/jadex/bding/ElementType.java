package jadex.bding;

public enum ElementType
{
    STRING(String.class),
    INTEGER(Integer.class),
    NUMBER(Double.class),
    BOOLEAN(Boolean.class),
    OBJECT(Object.class);

    private final Class<?> javatype;

    ElementType(Class<?> javatype)
    {
        this.javatype = javatype;
    }

    public Class<?> getJavaType()
    {
        return javatype;
    }

    public static ElementType fromString(String type)
    {
        return switch(type.toUpperCase())
        {
            case "STRING"  -> STRING;
            case "INTEGER" -> INTEGER;
            case "NUMBER",
                 "DOUBLE"  -> NUMBER;
            case "BOOLEAN" -> BOOLEAN;
            case "OBJECT"  -> OBJECT;
            default -> throw new IllegalArgumentException("Unknown element type: " + type);
        };
    }

    public static ElementType fromJavaClass(Class<?> type)
    {
        if(type == String.class || type == Character.class || type == char.class)
        {
            return STRING;
        }
        else if(type == Integer.class || type == int.class
            || type == Long.class || type == long.class
            || type == Short.class || type == short.class
            || type == Byte.class || type == byte.class)
        {
            return INTEGER;
        }
        else if(type == Double.class || type == double.class
            || type == Float.class || type == float.class)
        {
            return NUMBER;
        }
        else if(type == Boolean.class || type == boolean.class)
        {
            return BOOLEAN;
        }
        else
        {
            return OBJECT;
        }
    }
}