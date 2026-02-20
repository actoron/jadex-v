package jadex.llm.impl;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.eclipsesource.json.JsonValue;

import jadex.common.Tuple2;
import jadex.core.IComponent;
import jadex.core.impl.IDaemonComponent;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.llm.IMcpClientService;
import jadex.llm.IMcpTool;
import jadex.llm.McpContent;
import jadex.llm.McpContent.McpContentType;
import jadex.llm.McpResource;
import jadex.llm.McpServerInfo;
import jadex.llm.McpToolResult;
import jadex.llm.McpToolSchema;
import jadex.llm.annotation.McpTool;
import jadex.llm.jsonmapping.JsonMapper;
import jadex.providedservice.IService;
import jadex.publishservice.publish.annotation.Publish;
import jadex.requiredservice.IRequiredServiceFeature;

//@Publish(publishid="http://localhost:${port}/${app}/mcp", publishtarget=IMcpClientService.class)
@Publish(publishid="http://localhost:${port}/mcp", publishtarget=IMcpClientService.class)
public class McpClientAgent implements IMcpClientService, IDaemonComponent 
{
    protected Map<String, McpToolSchema> tools = new HashMap<>();

    private final List<File> resourceRoots = List.of(
        new File("/tmp")
    );

    @Inject
    protected IComponent agent;

    public IFuture<McpServerInfo> getClientInfo()
    {
        return new Future<>(new McpServerInfo("TestClient", "1.0", "mcp/1.0", List.of("tools", "resources", "prompts")));
    }

    @Override
    public IFuture<Collection<McpResource>> getResources() 
    {
        List<McpResource> resources = new ArrayList<>();
        for (File root : resourceRoots) 
        {
            resources.add(new McpResource(root.toURI().toString(), root.getName(), "directory"));
        }
        return new Future<>(resources);
    }

    @Override
    public IFuture<Collection<McpContent>> getResource(String uri) 
    {
        try 
        {
            List<McpContent> contents = new ArrayList<>();
            if (!uri.startsWith("file://")) 
                return new Future<>(new IllegalArgumentException("Only file:// URIs supported"));

            File f = new File(new URI(uri)).getCanonicalFile(); // Symlink + Path traversal check

            boolean allowed = false;
            for (File root : resourceRoots) 
            {
                if (f.getPath().startsWith(root.getCanonicalPath())) 
                {
                    allowed = true;
                    break;
                }
            }

            if (!allowed) 
                return new Future<>(new SecurityException("Access to resource denied: " + uri));

            String mime = Files.probeContentType(f.toPath());
            if (mime == null) 
                mime = "application/octet-stream";

            if (mime.startsWith("text/")) 
            {
                String text = Files.readString(f.toPath());
                List<McpContent> content = List.of(McpContent.text(text));
                contents.addAll(content);
            } 
            else 
            {
                byte[] data = Files.readAllBytes(f.toPath());
                String base64 = Base64.getEncoder().encodeToString(data);
                contents.add(McpContent.binary(base64));
            }

            return new Future<>(contents);
        } 
        catch (Exception e) 
        {
            return new Future<>(e);
        }
    }

    public IFuture<Collection<String>> getPrompts()
    {
        // todo: implement prompt management, for now just return empty list
        return new Future<>(List.of());
    }

    public IFuture<String> getPrompt(String name)
    {
        // todo: implement prompt management
        return new Future(new IllegalArgumentException("No such prompt: "+name));
    }

    public IFuture<Collection<McpToolSchema>> listTools()
    {
        Map<String, McpToolSchema> tools = new HashMap<>(this.tools);

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
                    McpToolSchema tool = new McpToolSchema(name, ifclass, m.getName(), ann.description(), JsonMapper.generateInputSchema(m), JsonMapper.generateOutputSchema(m));
                    tools.put(name, tool);
                }
            }
        }

        return new Future<>(tools.values());
    }

     public IFuture<Void> addTool(McpToolSchema tool)
    {
        tools.put(tool.name(), tool);
        return new Future<>((Void)null);
    }

    public IFuture<Boolean> removeTool(String toolName)
    {
        boolean removed = tools.remove(toolName) != null;
        return new Future<>(removed);
    }

    public IFuture<McpToolSchema> getTool(String name)
    {
        return new Future<>(tools.get(name));
    }

    public IFuture<McpToolResult> invokeTool(String toolname, Map<String, Object> args)
    {
        Future<McpToolResult> ret = new Future<>();

        Map<String, McpToolSchema> tools = listTools().get().stream().collect(Collectors.toMap(McpToolSchema::name, t -> t));

        McpToolSchema tool = tools.get(toolname);
        
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
                    fut.then(val -> 
                    {
                        //ret.setResult(JsonMapper.toJsonObject(val).toString();
                        ret.setResult(mapObjectToToolResult(val));
                    })
                    .catchEx(ret::setException);
                } 
                else 
                {
                    ret.setResult(mapObjectToToolResult(r));
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

    protected Object[] generateParameters(Class<?> ifclass, Method method, Map<String, Object> args)
    {
        /*Map<String, Object> inparamsmap = new LinkedHashMap<>();

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
        }*/

        Tuple2<List<Tuple2<String, String>>, Map<String, Class<?>>> pinfos = JsonMapper.getParameterInfos(method);

        Object[] ret = JsonMapper.generateInParameters(args, pinfos, method.getParameterTypes());

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

    public static McpToolResult mapObjectToToolResult(Object val) 
    {
        if (val == null) 
            return new McpToolResult(List.of(), false);

        try 
        {
            List<McpContent> contents = new ArrayList<>();

            if (val instanceof McpToolResult res) 
            {
                return res;
            } 
            else if (val instanceof McpContent res) 
            {
                contents.add(res);
            } 
            else if (val instanceof String s) 
            {
                contents.add(new McpContent(McpContentType.TEXT, s, null));
            } 
            else if (val instanceof Number n) 
            {
                contents.add(new McpContent(McpContentType.TEXT, n.toString(), null));
            } 
            else if (val instanceof Boolean b) 
            {
                contents.add(new McpContent(McpContentType.TEXT, b.toString(), null));
            }
            else if (val instanceof File f) 
            {
                contents.add(new McpContent(McpContentType.RESOURCE, null, f.toURI().toString()));
            } 
            else if (val instanceof Map<?,?> || val.getClass().isRecord() || val instanceof Collection<?> || val.getClass().isArray()) 
            {
                JsonValue jsonVal = JsonMapper.toJsonObject(val);
                contents.add(new McpContent(McpContentType.TEXT, jsonVal.toString(), null));
            } 
            else 
            {
                // Fallback: toString() in Textcontent?!
                contents.add(new McpContent(McpContentType.TEXT, val.toString(), null));
            }

            return new McpToolResult(contents, false);

        } 
        catch(Exception e) 
        {
            McpContent error = new McpContent(McpContentType.TEXT, e.getMessage(), null);
            return new McpToolResult(List.of(error), true);
        }
    }

}
