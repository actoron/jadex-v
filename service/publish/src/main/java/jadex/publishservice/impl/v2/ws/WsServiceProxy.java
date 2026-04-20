package jadex.publishservice.impl.v2.ws;

import java.lang.reflect.*;

public class WsServiceProxy implements InvocationHandler
{
    protected final WsClient client;

    protected final Class<?> servicetype;

    public WsServiceProxy(WsClient client, Class<?> serviceType)
    {
        this.client = client;
        this.servicetype = serviceType;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        Object ret = client.call(servicetype, method, args);

        return ret;
    }

    @SuppressWarnings("unchecked")
    public static <T> T create(String url, Class<T> type)
    {
        WsClient client = new WsClient(url);
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, new WsServiceProxy(client, type));
    }
}