package jadex.llm.impl;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import jadex.common.Tuple2;
import jadex.core.IComponent;
import jadex.core.impl.IDaemonComponent;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.llm.IMcpClientService;
import jadex.llm.IMcpTool;
import jadex.llm.ToolSchema;
import jadex.llm.annotation.McpTool;
import jadex.llm.jsonmapping.JsonMapper;
import jadex.providedservice.IService;
import jadex.requiredservice.IRequiredServiceFeature;

public class McpClientAgent implements IMcpClientService, IDaemonComponent 
{
    protected Map<String, ToolSchema> tools = new HashMap<>();

    @Inject
    protected IComponent agent;

    public IFuture<Collection<ToolSchema>> listTools()
    {
        Map<String, ToolSchema> tools = new HashMap<>(this.tools);

        Collection<IMcpTool> mcpifaces = agent.getFeature(IRequiredServiceFeature.class).getLocalServices(IMcpTool.class);
        for(IMcpTool t: mcpifaces)
        {
            Class<?> ifclass = ((IService)t).getServiceId().getServiceType().getType(getClass().getClassLoader());
            for(Method m: ifclass.getMethods())
            {
                if(m.isAnnotationPresent(McpTool.class))
                {
                    McpTool ann = m.getAnnotation(McpTool.class);
                    String name = ifclass.getName()+"."+m.getName();
                    ToolSchema tool = new ToolSchema(name, ifclass, m.getName(), ann.description(), JsonMapper.generateInputSchema(m), JsonMapper.generateOutputSchema(m));
                    tools.put(name, tool);
                }
            }
        }

        return new Future<>(tools.values());
    }

    public IFuture<Void> addTool(ToolSchema tool)
    {
        tools.put(tool.name(), tool);
        return new Future<>((Void)null);
    }

    public IFuture<Boolean> removeTool(String toolName)
    {
        boolean removed = tools.remove(toolName) != null;
        return new Future<>(removed);
    }

    public IFuture<ToolSchema> getTool(String name)
    {
        return new Future<>(tools.get(name));
    }

    public IFuture<String> invokeTool(String toolname, String argsstr) 
    {
        Future<String> ret = new Future<>();

        JsonObject args = Json.parse(argsstr).asObject();

        // find tools by scanning local services for methods annotated with @McpTool

        Collection<IMcpTool> mcpifaces = agent.getFeature(IRequiredServiceFeature.class).getLocalServices(IMcpTool.class);
        for(IMcpTool t: mcpifaces)
        {
            Class<?> ifclass = ((IService)t).getServiceId().getServiceType().getType(getClass().getClassLoader());
            for(Method m: ifclass.getMethods())
            {
                if(m.isAnnotationPresent(McpTool.class))
                {
                    McpTool ann = m.getAnnotation(McpTool.class);
                    String name = ifclass.getName()+"."+m.getName();
                    ToolSchema tool = new ToolSchema(name, ifclass, m.getName(), ann.description(), JsonMapper.generateInputSchema(m), JsonMapper.generateOutputSchema(m));
                    tools.put(name, tool);
                }
            }
        }

        ToolSchema tool = tools.get(toolname);
        
        if(tool==null)
        {
            ret.setException(new IllegalArgumentException("No such tool registered: "+toolname));
        }
        else
        {
            Class<?> ifclass = tool.service();
            Object service = agent.getFeature(IRequiredServiceFeature.class).getLocalService(ifclass); // todo: not only local scope

            if(service==null)
                throw new IllegalArgumentException("No such service registered: "+ifclass.getName());

            List<Method> methods = Arrays.stream(ifclass.getMethods())
                .filter(m -> m.getName().equals(tool.methodName()))
                .filter(m -> m.getParameterCount() == args.size())
                .collect(Collectors.toList());

            Method method = null;

            if(methods.size()==0)
            {
                ret.setException(new IllegalArgumentException("No such method: " + tool.methodName() + " in " + ifclass.getName()));
                return ret;
            }

            if(methods.size()==1)
            {
                method = methods.get(0);
            }
            else
            {
                // find by types?!
                ret.setException(new IllegalArgumentException("Ambiguous method call: "+method+" in interface "+ifclass.getName()));
                return ret;
            }

            Object[] params = generateParameters(ifclass, method, args);

            try 
            {
                Object r = method.invoke(service, params);
                if(r instanceof IFuture<?> fut) 
                {
                    fut.then(val -> ret.setResult(JsonMapper.toJsonObject(val).toString()))
                        .catchEx(ret::setException);
                } 
                else 
                {
                    ret.setResult(JsonMapper.toJsonObject(r).toString());
                }
            }
            catch(Exception e)
            {
                ret.setException(e);
                return ret;
            }
            
        }

        return ret;
    }

    protected Object[] generateParameters(Class<?> ifclass, Method method, JsonObject args)
    {
        Map<String, Object> inparamsmap = new LinkedHashMap<>();

        for(String key : args.names()) 
        {
            Object value = args.get(key);

            if (value instanceof JsonValue jsonVal) 
            {
                if (jsonVal.isNull()) 
                {
                    value = null;
                } 
                else if (jsonVal.isNumber()) 
                {
                    value = jsonVal.asInt(); // oder asLong/asDouble je nach Zieltyp
                } 
                else if (jsonVal.isBoolean()) 
                {
                    value = jsonVal.asBoolean();
                } 
                else if(jsonVal.isString()) 
                {
                    value = jsonVal.asString(); // !!! no extra quotes, just the raw string
                }
                else 
                {
                    value = jsonVal.toString();
                }
            }

            //System.out.println("Param: " + key + " -> " + value);
            inparamsmap.put(key, value);
        }

        Tuple2<List<Tuple2<String, String>>, Map<String, Class<?>>> pinfos = JsonMapper.getParameterInfos(method);

        Object[] ret = JsonMapper.generateInParameters(inparamsmap, pinfos, method.getParameterTypes());

        Class<?>[] types = method.getParameterTypes();
        for (int i = 0; i < ret.length; i++) 
        {
            if (ret[i] != null && !types[i].isInstance(ret[i])) 
            {
                ret[i] = JsonMapper.convertParameter(ret[i], types[i]);
            }
        }

        return ret;
    }
}
