package jadex.publishservicejetty.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jadex.common.ClassInfo;
import jadex.core.IComponentManager;
import jadex.providedservice.IService;
import jadex.providedservice.IServiceIdentifier;
import jadex.publishservice.impl.PublishInfo;

public class WsServiceRegistry
{
    protected Map<String, Set<IService>> servicesByName = new HashMap<>();

    protected Map<Class<?>, Set<IService>> servicesByType = new HashMap<>();

    protected Map<String, IService> servicesById = new HashMap<>();

    protected Map<IServiceIdentifier, PublishInfo> serviceInfos = new HashMap<>();

    public synchronized void add(String name, IService service)
    {
        IServiceIdentifier sid = service.getServiceId();
        ClassLoader cl = IComponentManager.get().getClassLoader();

        servicesByName.computeIfAbsent(name, k -> new HashSet<>()).add(service);
        servicesById.put(sid.toString(), service);

        servicesByType.computeIfAbsent(sid.getServiceType().getType(cl), k -> new HashSet<>()).add(service);
        
        for(ClassInfo stype: sid.getServiceSuperTypes())
            servicesByType.computeIfAbsent(stype.getType(cl), k -> new HashSet<>()).add(service);
    }

    public synchronized void addServiceInfo(IServiceIdentifier sid, PublishInfo info)
    {
        PublishInfo oldpi = serviceInfos.put(sid, info);
        if(oldpi!=null)
            System.out.println("New publish info for: "+sid+": "+oldpi+" -> "+info);
    }

    public synchronized void removeServiceInfo(IServiceIdentifier sid)
    {
        serviceInfos.remove(sid);
    }

    public synchronized PublishInfo getServiceInfo(IServiceIdentifier sid) 
    {
        return serviceInfos.get(sid);
    }

    public synchronized void remove(IServiceIdentifier sid)
    {
        IService service = servicesById.remove(sid.toString());
        if(service != null)
        {
            servicesByName.values().forEach(list -> list.remove(service));
            servicesByType.values().forEach(list -> list.remove(service));
        }
    }

    public synchronized Collection<IService> getByName(String name)
    {
        return servicesByName.get(name);
    }

    public synchronized Collection<IService> getByType(Class<?> type)
    {
        return servicesByType.get(type);
    }

    public synchronized IService getById(String id)
    {
        return servicesById.get(id);
    }
}