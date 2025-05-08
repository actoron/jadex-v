package jadex.bt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import jadex.bt.nodes.Node;
import jadex.core.IComponent;

public class BTCache 
{
    private static final Map<Class<?>, Node<IComponent>> cache = new ConcurrentHashMap<>();

    /**
     * Create or get the cached tree. Builder must not access instance variables of agent.
     * 
     * @param type The pojo type.
     * @param builder The builder. 
     * @return The tree.
     */
    public static Node<IComponent> createOrGet(Class<?> type, Supplier<Node<IComponent>> builder) 
    {
        return cache.computeIfAbsent(type, k -> builder.get());
    }
}