package jadex.common;

@FunctionalInterface
public interface ITriFunction<T, U, V, R> 
{
    R apply(T t, U u, V v);
}
