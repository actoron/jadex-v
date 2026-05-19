package jadex.publishservice.impl;

import java.lang.reflect.Method;

import jadex.common.SUtil;
import jadex.publishservice.IRequestManager;
import jadex.publishservice.impl.v2.RequestManager2;

public class RequestManagerFactory 
{
    public static Class<? extends IRequestManager> managerclass = RequestManager.class;
    //public static Class<? extends IRequestManager> managerclass = RequestManager2.class;

    public static synchronized void createInstance()
    {
        try
        {
            Method m = managerclass.getDeclaredMethod("createInstance", new Class[0]);
            m.setAccessible(true);
            m.invoke(null);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            SUtil.rethrowAsUnchecked(e);
        }
    }

    public static synchronized IRequestManager getInstance()
    {
        return getInstance(false);
    }

    public static synchronized IRequestManager getInstance(boolean create)
    {
        try
        {
            Method m = managerclass.getDeclaredMethod("getInstance", new Class<?>[]{boolean.class});
            m.setAccessible(true);
            return (IRequestManager)m.invoke(null, new Object[]{create});
        }
        catch(Exception e)
        {
            e.printStackTrace();
            SUtil.rethrowAsUnchecked(e);
            return null;
        }
    }

    

    public static void setRequestManagerClass(Class<? extends IRequestManager> clazz)
    {
        managerclass = clazz;
    }

    public static Class<?> getRequestManagerClass()
    {
        return managerclass;
    }
}
