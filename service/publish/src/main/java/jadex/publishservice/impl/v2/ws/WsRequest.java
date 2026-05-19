package jadex.publishservice.impl.v2.ws;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.core.IComponentManager;
import jadex.publishservice.impl.v2.JsonMapper;
import jadex.publishservice.impl.v2.Request;
import jadex.publishservice.impl.v2.TransportType;
import jadex.publishservice.impl.v2.ws.WsRequestMapper.InvocationType;

public class WsRequest extends Request
{
    protected IWsSession session;

    protected JsonObject rawjson;

    public WsRequest(String rawrequest, IWsSession session)
    {
        super(rawrequest);
        this.session = session;
    }

    public JsonObject getJsonObject()
    {
        if(rawjson==null)
            this.rawjson = Json.parse((String)getRawRequest()).asObject();
        return rawjson;
    }

    @Override
    public String extractSessionId()
    {
        return session.getId();
    }

    @Override
    public String extractCallId()
    {
        String id = JsonMapper.getString(getJsonObject(), "callid", false);
        if(id==null)
            id = SUtil.createUniqueId("_newcallid");
        return id;
    }

    public TransportType extractTransportType()
    {
        return TransportType.WS;
    }

    public IWsSession getSession() 
    {
        return session;
    }

    public String getServiceName()
    {
        return JsonMapper.getString(getJsonObject(), "servicename", false);
    }

    public Class<?> getServiceType()
    {
        Class<?> ret = null;
        String type = JsonMapper.getString(getJsonObject(), "servicetype", false);
        if(type!=null && type.length()>0)
            ret = SReflect.classForName0(type, IComponentManager.get().getClassLoader());
        return ret;
    }

    public String getServiceId()
    {
        String sid = JsonMapper.getString(getJsonObject(), "serviceid", false);
        return sid;
    }

    public InvocationType getInvocationType()
    {
        InvocationType ret = null;
        String itype = JsonMapper.getString(getJsonObject(), "invocationtype", false);
        if(itype!=null && itype.length()>0)
            ret = InvocationType.fromString(itype);
        return ret;
    }

    public String getResourcePath()
    {
        return JsonMapper.getString(getJsonObject(), "path", false);
    }

    public String getMethod()
    {
        return JsonMapper.getString(getJsonObject(), "method", false);
    }

    public String getInfoUrl()
    {
        return JsonMapper.getString(getJsonObject(), "infourl", false);
    }

    public Boolean getTerminate()
    {
        JsonValue value = getJsonObject().get("terminate");
        return value==null? null: value.asBoolean();
    }

    public String getException()
    {
        return JsonMapper.getString(getJsonObject(), "exception", false);
    }

    public Method getServiceMethod() 
    {
        String methodName = getMethod();
        Class<?> serviceType = getServiceType();
        
        if (serviceType == null || methodName == null || methodName.isEmpty()) 
            throw new RuntimeException("Service type or method name not specified");

        // Alle Methoden mit dem Namen sammeln
        Method[] methods = serviceType.getMethods();
        Method candidate = null;
        int paramCount = -1;

        for (Method m : methods) 
        {
            if (m.getName().equals(methodName)) 
            {
                if (candidate == null) 
                {
                    candidate = m;
                    paramCount = m.getParameterCount();
                } 
                else if (m.getParameterCount() == paramCount) 
                {
                    throw new RuntimeException("Multiple methods with name '" + methodName + "' and parameter count " + paramCount);
                }
            }
        }

        if (candidate == null) 
            throw new RuntimeException("No method found with name '" + methodName + "' in " + serviceType.getName());

        return candidate;
    }

    public Map<String, Object> getParameters() 
    {
        JsonObject json = getJsonObject().get("parameters") != null 
                        ? getJsonObject().get("parameters").asObject() 
                        : null;
        if (json == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> map = new HashMap<>();
        for (String key : json.names()) 
        {
            JsonValue value = json.get(key);
            if(value.isNumber()) 
            {
                map.put(key, value.asDouble()); 
            } 
            else if(value.isString()) 
            {
                map.put(key, value.asString());
            } 
            else if(value.isBoolean()) 
            {
                map.put(key, value.asBoolean());
            } 
            else if(value.isNull()) 
            {
                map.put(key, null);
            } 
            else 
            {
                map.put(key, value.toString()); 
            }
        }

        return map;
    }

    public Object[] getServiceParameters(Method method)
    {
        return mapWsParameters(this, method);
    } 

    public static Object[] mapWsParameters(WsRequest req, Method method) 
    {
        Map<String,Object> params = req.getParameters(); 
        Class<?>[] types = method.getParameterTypes();
        
        Object[] result = new Object[types.length];

        for (int i = 0; i < types.length; i++) 
        {
            String key = String.valueOf(i); // primitive index keys "0", "1", ...
            
            Object val = params.get(key);
            
            if (val != null) 
            {
                // primitive/simple conversion
                result[i] = JsonMapper.convertParameter(Collections.emptyList(), val, types[i]);
            } 
            else 
            {
                if (types[i] == boolean.class) result[i] = false;
                else if (types[i] == char.class) result[i] = '\0';
                else if (types[i].isPrimitive()) result[i] = 0;
            }
        }
        
        return result;
    }
}