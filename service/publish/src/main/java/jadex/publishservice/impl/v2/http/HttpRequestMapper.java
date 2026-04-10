package jadex.publishservice.impl.v2.http;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.common.Tuple2;
import jadex.common.transformation.traverser.ITraverseProcessor;
import jadex.core.impl.ComponentManager;
import jadex.publishservice.IRequestManager.PublishContext;
import jadex.publishservice.impl.MappingEvaluator.MappingInfo;
import jadex.publishservice.impl.v2.JsonMapper;
import jadex.publishservice.impl.v2.Request;
import jadex.publishservice.impl.v2.IRequestMapper;
import jadex.publishservice.impl.v2.invocation.Invocation;
import jadex.publishservice.impl.v2.invocation.InvocationResult;
import jadex.publishservice.impl.v2.invocation.ResourceInvocation;
import jadex.publishservice.impl.v2.invocation.ServiceInfoInvocation;
import jadex.publishservice.impl.v2.invocation.ServiceInvocation;
import jadex.publishservice.publish.PathManager;
import jadex.publishservice.publish.annotation.ParametersMapper;
import jadex.publishservice.publish.mapper.IParameterMapper2;
import jadex.publishservice.publish.mapper.IValueMapper;
import jadex.transformation.jsonserializer.JsonTraverser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

public class HttpRequestMapper implements IRequestMapper
{
    /** Finished result marker. */
	public static final String FINISHED	= "__finished__";
	
	/** URL parameter random request. */
	public static final String RANDOM = "__random";
	
	/** URL parameter type request. */
	public static final String CONTENTTYPE = "contenttype";
	
	/** URL parameter accept request. */
	public static final String ACCEPT = "__accept__";

    @Override
    public boolean canHandle(Request req) 
    {
        return req.getRawRequest() instanceof HttpServletRequest;
    }

    @Override
    public Invocation map(Request req, PublishContext context) 
    {
        Invocation ret = null;

        HttpServletRequest request = (HttpServletRequest)req.getRawRequest();
        PathManager<MappingInfo> pm = (PathManager<MappingInfo>)context.mapping();

        String methodname = request.getPathInfo();
	
        if(methodname != null && methodname.startsWith("/"))
            methodname = methodname.substring(1);
        if(methodname != null && methodname.endsWith("()"))
            methodname = methodname.substring(0, methodname.length() - 2);
        if(methodname != null && methodname.endsWith("/"))
            methodname = methodname.substring(0, methodname.length() - 1);
        final String fmn = methodname;

        //if(methodname!=null && request.toString().indexOf("efault")!=-1)
        //	System.out.println("INVOKE: " + methodname);
        
        Collection<MappingInfo> mis = pm!=null? pm.getElementsForPath(methodname): new ArrayList<MappingInfo>();

        // try to find with full path instead of method name
        if(mis==null || mis.size()==0)
        {
            String path = request.getRequestURI(); 
            String query = request.getQueryString(); 

            if (path.startsWith("/")) 
                path = path.substring(1);

            if (query != null && !query.isEmpty()) 
                path += "?" + query;

            System.out.println(path);
            mis = pm!=null? pm.getElementsForPath(path): new ArrayList<MappingInfo>();
        }
        
        //System.out.println("handleRequest: "+request.getPathInfo()+" "+methodname+" "+mis.size()+" "+bindings);

        if(methodname.endsWith(".js"))
        {
            //ri.addResultType("application/javascript");
            //loadResource(methodname).then(js -> writeResponse(ri.setStatus(Response.Status.OK.getStatusCode()).setFinished(true).setResult(js)));
            ret = new ResourceInvocation(req, methodname);
        }
        else if(methodname.endsWith(".css"))
        {
            //ri.addResultType("text/css");
            //loadResource(methodname).then(css -> writeResponse(ri.setStatus(Response.Status.OK.getStatusCode()).setFinished(true).setResult(css)));
            ret = new ResourceInvocation(req, methodname);
        }
        /*else if(methodname.endsWith(".html"))
        {
            ri.addResultType("text/html");
            loadHTML(methodname).then(html -> writeResponse(ri.setStatus(Response.Status.OK.getStatusCode()).setFinished(true).setResult(html)));
        }
        else if(methodname.endsWith(".js.map"))
        {
            ri.addResultType("application/javascript");
            loadJS(methodname).then(js -> writeResponse(ri.setStatus(Response.Status.OK.getStatusCode()).setFinished(true).setResult(js)));
        }*/
        else if(mis!=null && mis.size()>0)
        {
            //handleMethodCall(service, methodname, mis, bindings, callid, fsessionid, ri);

            List<Map<String, String>> bindings = mis.stream().map(x -> pm.getBindingsForPath(fmn)).collect(Collectors.toList());

            mis = mis.stream().filter(mi -> mi.getHttpMethod() != null
				&& mi.getHttpMethod().toString().equalsIgnoreCase(request.getMethod())).collect(Collectors.toList());

            if(mis.size()==0)
            {
                Exception e = new IllegalArgumentException("HTTP method "+request.getMethod()+" not allowed");
                //writeResponse(ri.setStatus(Response.Status.BAD_REQUEST.getStatusCode()).setFinished(true).setException(e));
                ret = new Invocation(req, context, new InvocationResult(e));
            }
            else
            {
                // convert and map parameters
                Tuple2<MappingInfo, Object[]> tup = mapParameters(req, mis, bindings);
                MappingInfo mi = tup.getFirstEntity();
                Object[] params = tup.getSecondEntity();
                ret = new ServiceInvocation(req, context, mi.getMethod(), params);
            }
        }
        else
        {
            // setup SSE if requested via header
            String ah = request.getHeader("Accept");
            if(ah!=null && ah.toLowerCase().indexOf(MediaType.SERVER_SENT_EVENTS)!=-1)
            {
                // todo: call connection manager! 
                
                System.out.println("sse connection saved: "+request.getAsyncContext()+" "+request.getSession().getId());
                
                //sendDelayedSSEEvents(getSession(fsessionid, true));
            }
            else if(context.service()!=null)
            {
                ret = new ServiceInfoInvocation(req, context, getServletUrl(request));
            }
            else
            {
                ret = new Invocation(req, context, new InvocationResult(new IllegalArgumentException("No service found for: "+methodname)));
                System.out.println("No service found for: "+methodname);
            }
        }

        return ret;
    }

    /**
	 * Get the servlet base url.
	 * 
	 * @param req The request.
	 * @return The servlet base url.
	 */
	public static String getServletUrl(HttpServletRequest req)
	{
		StringBuffer url = new StringBuffer(getServletHost(req));
		String cp = req.getContextPath(); // deploy directory
		String serp = req.getServletPath(); // name of servlet

		if(cp != null)
			url.append(cp);
		if(serp != null)
			url.append(serp);

		return url.toString();
	}

    /**
	 * Get the servlet base url.
	 * 
	 * @param req The request.
	 * @return The servlet base url.
	 */
	public static String getServletHost(HttpServletRequest req)
	{
		StringBuffer url = new StringBuffer();
		String scheme = req.getScheme();
		int port = req.getServerPort();

		url.append(scheme);
		url.append("://");
		url.append(req.getServerName());
		if(("http".equals(scheme) && port != 80) || ("https".equals(scheme) && port != 443))
		{
			url.append(':');
			url.append(req.getServerPort());
		}

		return url.toString();
	}

    /**
     * Map the incoming uri/post/multipart parameters to the service target parameter types.
     */
    protected Tuple2<MappingInfo, Object[]> mapParameters(Request req, 
        Collection<MappingInfo> mis, List<Map<String, String>> bindings) 
    {
        HttpServletRequest request = (HttpServletRequest) req.getRawRequest();

        // Extract raw data
        Map<String,Object> inparams = extractRawParameters(request);
        Tuple2<String, List<String>> meta = extractRequestMeta(request); // Request content-type + accepted response types

        // find method and binding
        Tuple2<MappingInfo, Map<String,String>> methodRes = findMethod(mis, bindings, inparams);
        MappingInfo mi = methodRes.getFirstEntity();
        Map<String,String> binding = methodRes.getSecondEntity();

        // add path parameters from binding to inparams
        if(binding != null && !binding.isEmpty())
            inparams.putAll(binding);

        // acceptable media types for input
        List<String> acctypes = new ArrayList<>(mi.getConsumedMediaTypes());
        if(acctypes == null || acctypes.size()==0)
            acctypes = meta.getSecondEntity();
        else
            acctypes.retainAll(meta.getSecondEntity());
        // Body-parsing & mapping
        Object[] params = mapToMethodParameters(request, mi, inparams, acctypes, meta.getFirstEntity());

        return new Tuple2<>(mi, params);
    }

    protected Map<String,Object> extractRawParameters(HttpServletRequest request) 
    {
        Map<String,Object> inparamsmap = new LinkedHashMap<>();

        // Query-Parameter
        if(request.getQueryString() != null)
            inparamsmap.putAll(splitQueryString(request.getQueryString()));

        // Multipart form-data
        try
        {
            if(request.getContentType()!=null &&
                request.getContentType().startsWith(MediaType.MULTIPART_FORM_DATA) &&
                request.getParts().size() > 0) 
            {
                for(Part part : request.getParts()) 
                {
                    byte[] data = SUtil.readStream(part.getInputStream());
                    String mime = SUtil.guessContentTypeByBytes(data);

                    if(mime != null && (mime.contains("application") || mime.contains("image") || mime.contains("audio"))) 
                    {
                        addEntry(inparamsmap, part.getName(), data);
                    } 
                    else 
                    {
                        addEntry(inparamsmap, part.getName(), new String(data, StandardCharsets.UTF_8));
                    }
                }
            }
        }
        catch(Exception e)
        {
            throw new RuntimeException("Unable to parse multipart form data", e);
        }

        // read body as raw bytes
        byte[] bodyBytes = null;
        try (InputStream is = request.getInputStream()) 
        {
            if(is != null)
                bodyBytes = SUtil.readStream(is);
        } 
        catch(Exception e)
        {
            // ignore
        }

        if(bodyBytes != null && bodyBytes.length > 0) 
            inparamsmap.put("bodyBytes", bodyBytes); 

        inparamsmap.remove(RANDOM);

        return inparamsmap;
    }

    protected Tuple2<String, List<String>> extractRequestMeta(HttpServletRequest request) 
    {
        String contentType = request.getHeader("Content-Type");
        if(contentType == null)
            contentType = request.getHeader("Accept");
        List<String> acceptedTypes = parseMimetypes(contentType);
        return new Tuple2<>(contentType, acceptedTypes);
    }

    public Tuple2<MappingInfo, Map<String, String>> findMethod(Collection<MappingInfo> mis, 
        List<Map<String,String>> bindings, Map<String,Object> inparamsmap)
    {
        // Find correct method using paramter count
        MappingInfo mi = null;
        Map<String, String> binding = null;
        if(mis.size() == 1)
        {
            mi = mis.iterator().next();
            binding = bindings.get(0);
        }
        else
        {
            List<Object[]> matches1 = new ArrayList<>(); // by parameter names
            List<Object[]> matches2 = new ArrayList<>(); // by parameter cnt
            
            int psize = inparamsmap.size();
            Iterator<Map<String, String>> bit = bindings.iterator();
            for(MappingInfo tst : mis)
            {
                Map<String, String> b = bit.next();
                List<String> declaredparamnames = tst.getParameterNames();
                Set<String> paramnames = inparamsmap.keySet();
                
                if(declaredparamnames.size()>0 && declaredparamnames.containsAll(paramnames))
                {
                    matches1.add(new Object[]{tst, b, paramnames.size()});
                }
                
                if(psize+b.size() == tst.getMethod().getParameterTypes().length)
                {
                    matches2.add(new Object[]{tst, b, psize+b.size()});
                }
            }
            
            if(matches1.size()>0)
            {
                matches1.sort((x,y) -> ((Integer)y[2]).intValue()-((Integer)x[2]).intValue());
                Object[] res = matches1.get(0);
                mi = (MappingInfo)res[0];
                binding = (Map<String, String>)res[1];
            }
            else if(matches2.size()>0)
            {
                matches2.sort((x,y) -> ((Integer)y[2]).intValue()-((Integer)x[2]).intValue());
                Object[] res = matches2.get(0);
                mi = (MappingInfo)res[0];
                binding = (Map<String, String>)res[1];
            }
        }

        if(mi == null)
            throw new RuntimeException("No method mapping found.");
        
        // Add path infos from binding to inparamsmap
        // if(binding!=null && binding.size()>0)
        //    inparamsmap.putAll(binding);

        //Method method = mi.getMethod();
        return new Tuple2<>(mi, binding);
    }

    protected Object[] mapToMethodParameters(HttpServletRequest request, MappingInfo mi, 
        Map<String,Object> inparamsmap, List<String> acctypes, String ct)
    {
        Method method = mi.getMethod();
        Class<?>[] types = method.getParameterTypes();

        // is a @FormParam parameter used by the user?
        Tuple2<List<Tuple2<String, String>>, Map<String, Class<?>>> pinfos = getParameterInfos(method);
        boolean hasformparam = false;
        for(Tuple2<String, String> pinfo: pinfos.getFirstEntity())
        {
            hasformparam = "form".equals(pinfo.getFirstEntity());
            if(hasformparam)
                break;
        }

        Object[] targetparams = null;

        // body parsing can only be done now because it depends on the method 
        // (e.g. for @ParametersMapper) and acceptable media types, so we need 
        // to extract it here to have it available for mapping

        byte[] bytes = (byte[])inparamsmap.remove("bodyBytes");

        // TODO Hack for raw JSON to avoid converting later?
        boolean rawjson = false;
        if(bytes!=null && bytes.length>0)
        {
            String mime = SUtil.guessContentTypeByBytes(bytes);
            if(mime!=null && (mime.indexOf("application")!=-1 || mime.indexOf("image")!=-1 || mime.indexOf("audio")!=-1))
            {
                // add as raw byte[] if mimetype is binary 
                // add under what name?!
                addEntry(inparamsmap, "body", bytes);
            }
            else
            {
                String str = new String(bytes, SUtil.UTF8);
                
                if(ct!=null && ct.trim().startsWith("application/x-www-form-urlencoded"))
                {
//					System.out.println(str);
                    Map<String, Object> vals = splitQueryString(str);
                    inparamsmap.putAll(vals);
                }
                else if(ct!=null && (ct.trim().startsWith("application/json") || ct.trim().startsWith("test/plain")))
                {
                    // if only one target argument
                    if(types.length == 1)
                    {
                        // if is json object
                        if(str.trim().startsWith("{"))
                        {
                            if (String.class.equals(types[0]))
                            {
                                // Return raw JSON to parameter
                                rawjson = true;
                                inparamsmap.put("0", str);
                            }
                            else
                            {
                                // Map as first argument
                                Object arg0 = convertJsonValue(str, types[0], getClassLoader(), true);
                                inparamsmap.put("0", arg0);
                            }
                        }
                        else if(str.trim().startsWith("\""))
                        {
                            // try to directly convert to target type
                            Object arg0 = JsonTraverser.objectFromString(str, getClassLoader(), null, types[0], null);
                            inparamsmap.put("0", arg0);
                        }
                    }
                    // multiple arguments
                    else
                    {
                        // Array of objects as arguments
                        JsonValue args = Json.parse(str);
                        
                        if(args instanceof JsonArray)
                        {
                            JsonArray array = (JsonArray)args;
                            for(int i = 0; i < array.size(); i++)
                            {
                                inparamsmap.put(""+i, convertJsonValue(array.get(i).toString(), types[i], getClassLoader(), false));
                            }
                        }
                        else if(args instanceof JsonObject)
                        {
                            JsonObject jobj = (JsonObject)args;
                            if(hasformparam)
                            {
                                Map<String, Class<?>> typesmap = pinfos.getSecondEntity();
                                // put all contained objects in the params map
                                int[] i = new int[1];
                                final Map<String, Object> finparamsmap = inparamsmap;
                                jobj.forEach((com.eclipsesource.json.JsonObject.Member x)->
                                {
                                    i[0]++;
                                    Class<?> type = typesmap.get(x.getName());
                                    if(type!=null)
                                    {
                                        Object val = convertJsonValue(x.getValue().toString(), type, getClassLoader(), false);
                                        finparamsmap.put(x.getName(), val);
                                    }
                                    else
                                    {
                                        System.out.println("Ignoring argument with no type: "+x.getName());
                                        //throw new RuntimeException("Unable to determine argument type: "+x.getName());
                                    }
                                });
                            }
                            else
                            {
                                inparamsmap.putAll((Map)convertJsonValue(str, Map.class, getClassLoader(), false));
                            }
                        }
                    }
                }
                else
                {
                    throw new RuntimeException("Content type not supported for body: "+ct);
                }
            }
        }

        // From here the parameter array 'inparams' is built using
        // a) the map with possibly named input values 'inparamsmap'
        // b) the parameter annotations describing where each parameter comes from 'pinfos'
        
        // if(sr.size()>0)
        // System.out.println("found acceptable in types: "+sr);
        // if(sr.size()==0)
        // System.out.println("found no acceptable in types.");

        if(method.isAnnotationPresent(ParametersMapper.class))
        {
            // System.out.println("foundmapper");
            ParametersMapper mm = method.getAnnotation(ParametersMapper.class);
            if(!mm.automapping())
            {
                Class<?> pclazz = mm.value();
                Object mapper;
                if(!Object.class.equals(pclazz))
                {
                    try
                    {
                        mapper = pclazz.getDeclaredConstructor().newInstance();
                    }
                    catch(Exception e)
                    {
                        throw new RuntimeException("Unable to instantiate parameter mapper: "+pclazz.getName(), e);
                    }
                }
                else
                {
                    throw new RuntimeException("No valid mapper class specified in ParametersMapper annotation.");
                }
                
                if(mapper instanceof IValueMapper)
                {
                    // The order of in parameters is corrected with respect to the
                    // target parameter order
                    Object[] inparams = JsonMapper.generateInParameters(inparamsmap, pinfos, types);
                    for(int i = 0; i < inparams.length; i++)
                    {
                        if(inparams[i] instanceof String)
                            inparams[i] = JsonMapper.convertParameter(acctypes, (String)inparams[i], types[i]);
                    }

                    targetparams = new Object[inparams.length];
                    for(int i=0; i<targetparams.length; i++)
                    {
                        try
                        {
                            targetparams[i] = ((IValueMapper)mapper).convertValue(inparams[i]);
                        }
                        catch(Exception e)
                        {
                            throw new RuntimeException("Unable to convert parameter value: "+inparams[i], e);
                        }
                    }
                }
                else if(mapper instanceof IParameterMapper2)
                {
                    try
                    {
                        targetparams = ((IParameterMapper2)mapper).convertParameters(inparamsmap, pinfos, request);
                    }
                    catch(Exception e)
                    {
                        throw new RuntimeException("Unable to convert parameters with mapper: "+mapper.getClass().getName(), e);
                    }
                }
                else
                {
                    throw new RuntimeException("Mapper does not implement IParameterMapper/2");
                }
            }
            else
            {
                // System.out.println("automapping detected");
                Class<?>[] ts = method.getParameterTypes();
                targetparams = new Object[ts.length];
                if(ts.length == 1 && inparamsmap != null)
                {
                    if(SReflect.isSupertype(ts[0], Map.class))
                    {
                        targetparams[0] = inparamsmap;
                        ((Map)targetparams[0]).putAll(extractCallerValues(request));
                    }
                }
            }
        }
        
        // Natural auto map if there are in parameters
        // Mappers can return null to not handle the mapping and let default mapping being applied
        if(targetparams==null)
        {
            targetparams = new Object[types.length];

            Object[] inparams = JsonMapper.generateInParameters(inparamsmap, pinfos, types);
            if (!rawjson)
            {
                for (int i = 0; i < inparams.length; i++)
                {
                    inparams[i] = JsonMapper.convertParameter(acctypes, inparams[i], types[i]);
                }
            }
            
            for(int i = 0; i < targetparams.length && i < inparams.length; i++)
            {
                targetparams[i] = inparams[i];
            }
        }
        
        // Type check parameters and convert
        for(int i = 0; i < targetparams.length; i++)
        {
            Object p = targetparams[i];
            Object v = null;

            if(p != null)
            {
                // Only convert if necessary to keep transformed values from mapper
                if(!SReflect.isSupertype(types[i], p.getClass()))
                {
                    if(types[i].isArray())
                    {
                        // fill in collection
                        if(p instanceof Collection)
                        {
                            Collection<Object> col = (Collection<Object>)p;
                            Object ar = Array.newInstance(types[i].getComponentType(), col.size());
                            v = ar;
                            Iterator<Object> it = col.iterator();
                            for(int j = 0; j < col.size(); j++)
                            {
                                Object a = JsonMapper.convertParameter(acctypes, it.next(), types[i].getComponentType());
                                if(a != null)
                                    Array.set(ar, j, a);
                            }
                        }
                        // varargs support -> convert matching single value
                        // to singleton array
                        else if(SReflect.isSupertype(types[i].getComponentType(), p.getClass()))
                        {
                            v = Array.newInstance(types[i].getComponentType(), 1);
                            Array.set(targetparams[i], 0, p);
                        }
                    }
                    else
                    {
                        v = JsonMapper.convertParameter(acctypes, p, types[i]);
                    }
                }

                if(v != null)
                {
                    targetparams[i] = v;
                }
            }
        }
        
        // Add default values for basic types
        for(int i = 0; i < targetparams.length; i++)
        {
            if(targetparams[i] == null)
            {
                if(types[i].equals(boolean.class))
                {
                    targetparams[i] = Boolean.FALSE;
                }
                else if(types[i].equals(char.class))
                {
                    targetparams[i] = Character.valueOf((char)0);
                }
                else if(SReflect.getWrappedType(types[i]) != types[i]) // Number type
                {
                    targetparams[i] = Integer.valueOf(0);
                }
            }
        }

        return targetparams;
    }

    /**
	 * Map the incoming uri/post/multipart parameters to the service target
	 * parameter types.
	 * /
	protected Tuple2<MappingInfo, Object[]> mapParameters(Request req, Collection<MappingInfo> mis, List<Map<String, String>> bindings)
	{
		try
		{
            HttpServletRequest request = (HttpServletRequest)req.getRawRequest();

			Object[] targetparams = null;

			Map<String, Object> inparamsmap = new LinkedHashMap<>();
			
			String ct = request.getHeader("Content-Type");
			if(ct == null)
				ct = request.getHeader("Accept");

			// parameters for query string (must be parsed to keep order) and
			// posted form data not for multi-part
			if(request.getQueryString() != null)
				inparamsmap.putAll(splitQueryString(request.getQueryString()));

			// Read multi-part form data
			if(request.getContentType() != null && request.getContentType().startsWith(MediaType.MULTIPART_FORM_DATA) && request.getParts().size() > 0)
			{
				for(Part part : request.getParts())
				{
					//System.out.println("content-type: "+part.getContentType());
					byte[] data = SUtil.readStream(part.getInputStream());
					String mime = SUtil.guessContentTypeByBytes(data);
					if(mime!=null && (mime.indexOf("application")!=-1 || mime.indexOf("image")!=-1 || mime.indexOf("audio")!=-1))
					{
						// add as raw byte[] if mimetype is binary 
						addEntry(inparamsmap, part.getName(), data);
					}
					else
					{
						// add as text if other mimetype
						addEntry(inparamsmap, part.getName(), new String(data, StandardCharsets.UTF_8));
					}
				}
			}
			
			// Hack, removes internal random id used to avoid browser cache
			inparamsmap.remove(RANDOM);
			// For GET requests attribute 'contenttype' are added
			Object cs = inparamsmap.remove(CONTENTTYPE); // why not accept
			
			//if(cs!=null)
			//	System.out.println("found contenttype: "+cs);
					
			// Find correct method using paramter count
			MappingInfo mi = null;
			Map<String, String> binding = null;
			if(mis.size() == 1)
			{
				mi = mis.iterator().next();
				binding = bindings.get(0);
			}
			else
			{
				List<Object[]> matches1 = new ArrayList<>(); // by parameter names
				List<Object[]> matches2 = new ArrayList<>(); // by parameter cnt
				
				int psize = inparamsmap.size();
				Iterator<Map<String, String>> bit = bindings.iterator();
				for(MappingInfo tst : mis)
				{
					Map<String, String> b = bit.next();
					List<String> declaredparamnames = tst.getParameterNames();
					Set<String> paramnames = inparamsmap.keySet();
					
					if(declaredparamnames.size()>0 && declaredparamnames.containsAll(paramnames))
					{
						matches1.add(new Object[]{tst, b, paramnames.size()});
					}
					
					if(psize+b.size() == tst.getMethod().getParameterTypes().length)
					{
						matches2.add(new Object[]{tst, b, psize+b.size()});
					}
				}
				
				if(matches1.size()>0)
				{
					matches1.sort((x,y) -> ((Integer)y[2]).intValue()-((Integer)x[2]).intValue());
					Object[] res = matches1.get(0);
					mi = (MappingInfo)res[0];
					binding = (Map<String, String>)res[1];
				}
				else if(matches2.size()>0)
				{
					matches2.sort((x,y) -> ((Integer)y[2]).intValue()-((Integer)x[2]).intValue());
					Object[] res = matches2.get(0);
					mi = (MappingInfo)res[0];
					binding = (Map<String, String>)res[1];
				}
			}

			if(mi == null)
				throw new RuntimeException("No method mapping found.");
			
			// Add path infos from binding to inparamsmap
			if(binding!=null && binding.size()>0)
				inparamsmap.putAll(binding);

			Method method = mi.getMethod();
			
			// target method types
			Class<?>[] types = method.getParameterTypes();
			
			// acceptable media types for input
			//String mts = request.getHeader("Content-Type");
			List<String> cl = parseMimetypes(ct);
			addMimeTypes(cs, cl);

			List<String> sr = new ArrayList<String>(mi.getConsumedMediaTypes());
			if(sr == null || sr.size() == 0)
			{
				sr = cl;
			}
			else
			{
				sr.retainAll(cl);
			}

			Tuple2<List<Tuple2<String, String>>, Map<String, Class<?>>> pinfos = getParameterInfos(method);
			
			// is a @FormParam parameter used by the user?
			boolean hasformparam = false;
			for(Tuple2<String, String> pinfo: pinfos.getFirstEntity())
			{
				hasformparam = "form".equals(pinfo.getFirstEntity());
				if(hasformparam)
					break;
			}
			
			// Read parameter from stream (message body)
			//if((inparams == null || inparams.length == 0) && types.length > 0 && ct != null && (ct.trim().startsWith("application/json") || ct.trim().startsWith("test/plain")))
			//{
			byte[] bytes = null;
			try
			{
				// Nano can throw exception here :-(
				InputStream is = request.getInputStream();
				if(is!=null)
					bytes = SUtil.readStream(is);
			}
			catch(Exception e)
			{
			}

			// TODO Hack for raw JSON to avoid converting later?
			boolean rawjson = false;
			if(bytes!=null && bytes.length>0)
			{
				String mime = SUtil.guessContentTypeByBytes(bytes);
				if(mime!=null && (mime.indexOf("application")!=-1 || mime.indexOf("image")!=-1 || mime.indexOf("audio")!=-1))
				{
					// add as raw byte[] if mimetype is binary 
					// add under what name?!
					addEntry(inparamsmap, "body", bytes);
				}
				else
				{
					String str = new String(bytes, SUtil.UTF8);
					
					if(ct!=null && ct.trim().startsWith("application/x-www-form-urlencoded"))
					{
	//					System.out.println(str);
						Map<String, Object> vals = splitQueryString(str);
						inparamsmap.putAll(vals);
					}
					else if(ct!=null && (ct.trim().startsWith("application/json") || ct.trim().startsWith("test/plain")))
					{
						// if only one target argument
						if(types.length == 1)
						{
							// if is json object
							if(str.trim().startsWith("{"))
							{
								if (String.class.equals(types[0]))
								{
									// Return raw JSON to parameter
									rawjson = true;
									inparamsmap.put("0", str);
								}
								else
								{
									// Map as first argument
									Object arg0 = convertJsonValue(str, types[0], getClassLoader(), true);
									inparamsmap.put("0", arg0);
								}
							}
							else if(str.trim().startsWith("\""))
							{
								// try to directly convert to target type
								Object arg0 = JsonTraverser.objectFromString(str, getClassLoader(), null, types[0], null);
								inparamsmap.put("0", arg0);
							}
						}
						// multiple arguments
						else
						{
							// Array of objects as arguments
							JsonValue args = Json.parse(str);
							
							if(args instanceof JsonArray)
							{
								JsonArray array = (JsonArray)args;
								for(int i = 0; i < array.size(); i++)
								{
									inparamsmap.put(""+i, convertJsonValue(array.get(i).toString(), types[i], getClassLoader(), false));
								}
							}
							else if(args instanceof JsonObject)
							{
								JsonObject jobj = (JsonObject)args;
								if(hasformparam)
								{
									Map<String, Class<?>> typesmap = pinfos.getSecondEntity();
									// put all contained objects in the params map
									int[] i = new int[1];
									final Map<String, Object> finparamsmap = inparamsmap;
									jobj.forEach((com.eclipsesource.json.JsonObject.Member x)->
									{
										i[0]++;
										Class<?> type = typesmap.get(x.getName());
										if(type!=null)
										{
											Object val = convertJsonValue(x.getValue().toString(), type, getClassLoader(), false);
											finparamsmap.put(x.getName(), val);
										}
										else
										{
											System.out.println("Ignoring argument with no type: "+x.getName());
											//throw new RuntimeException("Unable to determine argument type: "+x.getName());
										}
									});
								}
								else
								{
									inparamsmap.putAll((Map)convertJsonValue(str, Map.class, getClassLoader(), false));
								}
							}
						}
					}
					else
					{
						throw new RuntimeException("Content type not supported for body: "+ct);
					}
				}
			}
			
			// From here the parameter array 'inparams' is built using
			// a) the map with possibly named input values 'inparamsmap'
			// b) the parameter annotations describing where each parameter comes from 'pinfos'
			
			// if(sr.size()>0)
			// System.out.println("found acceptable in types: "+sr);
			// if(sr.size()==0)
			// System.out.println("found no acceptable in types.");

			if(method.isAnnotationPresent(ParametersMapper.class))
			{
				// System.out.println("foundmapper");
				ParametersMapper mm = method.getAnnotation(ParametersMapper.class);
				if(!mm.automapping())
				{
					Class<?> pclazz = mm.value();
					Object mapper;
					if(!Object.class.equals(pclazz))
					{
						mapper = pclazz.getDeclaredConstructor().newInstance();
					}
					else
					{
						throw new RuntimeException("No valid mapper class specified in ParametersMapper annotation.");
					}
					
					if(mapper instanceof IValueMapper)
					{
						// The order of in parameters is corrected with respect to the
						// target parameter order
						Object[] inparams = JsonMapper.generateInParameters(inparamsmap, pinfos, types);
						for(int i = 0; i < inparams.length; i++)
						{
							if(inparams[i] instanceof String)
								inparams[i] = JsonMapper.convertParameter(sr, (String)inparams[i], types[i]);
						}

						targetparams = new Object[inparams.length];
						for(int i=0; i<targetparams.length; i++)
						{
							targetparams[i] = ((IValueMapper)mapper).convertValue(inparams[i]);
						}
					}
					else if(mapper instanceof IParameterMapper2)
					{
						targetparams = ((IParameterMapper2)mapper).convertParameters(inparamsmap, pinfos, request);
					}
					else
					{
						throw new RuntimeException("Mapper does not implement IParameterMapper/2");
					}
				}
				else
				{
					// System.out.println("automapping detected");
					Class<?>[] ts = method.getParameterTypes();
					targetparams = new Object[ts.length];
					if(ts.length == 1 && inparamsmap != null)
					{
						if(SReflect.isSupertype(ts[0], Map.class))
						{
							targetparams[0] = inparamsmap;
							((Map)targetparams[0]).putAll(extractCallerValues(request));
						}
					}
				}
			}
			
			// Natural auto map if there are in parameters
			// Mappers can return null to not handle the mapping and let default mapping being applied
            if(targetparams==null)
			{
				targetparams = new Object[types.length];

				Object[] inparams = JsonMapper.generateInParameters(inparamsmap, pinfos, types);
				if (!rawjson)
				{
					for (int i = 0; i < inparams.length; i++)
					{
						inparams[i] = JsonMapper.convertParameter(sr, inparams[i], types[i]);
					}
				}
				
				for(int i = 0; i < targetparams.length && i < inparams.length; i++)
				{
					targetparams[i] = inparams[i];
				}
			}
			
			// Type check parameters and convert
			for(int i = 0; i < targetparams.length; i++)
			{
				Object p = targetparams[i];
				Object v = null;

				if(p != null)
				{
					// Only convert if necessary to keep transformed values from mapper
					if(!SReflect.isSupertype(types[i], p.getClass()))
					{
						if(types[i].isArray())
						{
							// fill in collection
							if(p instanceof Collection)
							{
								Collection<Object> col = (Collection<Object>)p;
								Object ar = Array.newInstance(types[i].getComponentType(), col.size());
								v = ar;
								Iterator<Object> it = col.iterator();
								for(int j = 0; j < col.size(); j++)
								{
									Object a = JsonMapper.convertParameter(sr, it.next(), types[i].getComponentType());
									if(a != null)
										Array.set(ar, j, a);
								}
							}
							// varargs support -> convert matching single value
							// to singleton array
							else if(SReflect.isSupertype(types[i].getComponentType(), p.getClass()))
							{
								v = Array.newInstance(types[i].getComponentType(), 1);
								Array.set(targetparams[i], 0, p);
							}
						}
						else
						{
							v = JsonMapper.convertParameter(sr, p, types[i]);
						}
					}

					if(v != null)
					{
						targetparams[i] = v;
					}
				}
			}
			
			// Add default values for basic types
			for(int i = 0; i < targetparams.length; i++)
			{
				if(targetparams[i] == null)
				{
					if(types[i].equals(boolean.class))
					{
						targetparams[i] = Boolean.FALSE;
					}
					else if(types[i].equals(char.class))
					{
						targetparams[i] = Character.valueOf((char)0);
					}
					else if(SReflect.getWrappedType(types[i]) != types[i]) // Number type
					{
						targetparams[i] = Integer.valueOf(0);
					}
				}
			}

			return new Tuple2<MappingInfo, Object[]>(mi, targetparams);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}*/

    /**
	 * Split the query and save the order.
	 */
	public static Map<String, Object> splitQueryString(String query)
	{
		Map<String, Object> ret = new LinkedHashMap<String, Object>();

		String[] pairs = query.split("&");
		Map<String, Set<Tuple2<Integer, String>>> compacted = new HashMap<>();

		for(String pair : pairs)
		{
            int idx = pair.indexOf("=");
            String key = null;
            String val = null;
            try
            {
			    
			    key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
			    val = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
            }
            catch(Exception e)
            {
                continue;
            }

			idx = key.indexOf("_");
			boolean added = false;
			if(idx != -1)
			{
				String p = key.substring(idx + 1);
				try
				{
					int pos = Integer.parseInt(p);
					String ckey = key.substring(0, idx);
					Set<Tuple2<Integer, String>> col = compacted.get(ckey);
					if(col == null)
					{
						col = new TreeSet<>(new Comparator<Tuple2<Integer, String>>()
						{
							public int compare(Tuple2<Integer, String> o1, Tuple2<Integer, String> o2)
							{
								return o1.getFirstEntity() - o2.getFirstEntity();
							}
						});
						compacted.put(ckey, col);
					}
					added = true;
					col.add(new Tuple2<Integer, String>(pos, val));
				}
				catch(Exception e)
				{
				}
			}
			if(!added)
			{
				addEntry(ret, key, val);
			}
		}

		compacted.entrySet().stream().forEach(e -> 
		{
			TreeSet<Tuple2<Integer, String>> vals = (TreeSet<Tuple2<Integer, String>>)e.getValue();
			Tuple2<Integer, String> lastval = vals.last();
			
			String[] res = new String[lastval.getFirstEntity()+1];
			
			vals.stream().forEach(t -> res[t.getFirstEntity()] = t.getSecondEntity());
			
			List<String> data = Arrays.asList(res);
			
			// does not create empty slots in case of args_0, args_3, args_4
			//List<String> data = e.getValue().stream().map(a -> a.getSecondEntity()).collect(Collectors.toList());
			
			addEntry(ret, e.getKey(), data);
		});

		return ret;
	}
	
	/**
	 * @param ret
	 * @param key
	 * @param val
	 */
	protected static void addEntry(Map<String, Object> ret, String key, Object val)
	{
		if(ret.containsKey(key))
		{
			Object v = ret.get(key);
			if(v instanceof String)
			{
				List<String> col = new ArrayList<>();
				col.add((String)v);
				if(val instanceof Collection)
					col.addAll((Collection)val);
				else
					col.add((String)val);
				ret.put(key, col);
			}
			else if(v instanceof Collection)
			{
				Collection<String> col = (Collection<String>)v;
				if(val instanceof Collection)
					col.addAll((Collection)val);
				else
					col.add((String)val);
			}
		}
		else
		{
			ret.put(key, val);
		}
	}

    /**
	 * todo: make statically accessible Copied from Jadex ForwardFilter
	 */
	public static List<String> parseMimetypes(String mts)
	{
		// List<String> mimetypes = null;
		List<String> mimetypes = new ArrayList<String>();
		if(mts != null)
		{
			// mimetypes = new ArrayList<String>();
			StringTokenizer stok = new StringTokenizer(mts, ",");
			while(stok.hasMoreTokens())
			{
				String tok = stok.nextToken();
				StringTokenizer substok = new StringTokenizer(tok, ";");
				String mt = substok.nextToken();
				String charset = null;
				while(substok.hasMoreTokens())
				{
					String subtok = substok.nextToken().trim();
					if(subtok.startsWith("charset"))
					{
						charset = "; " + subtok;
						break;
					}
				}
				if(mimetypes == null)
				{
					mimetypes = new ArrayList<String>();
				}
				mimetypes.add(mt + (charset != null ? charset : ""));
			}
		}
		return mimetypes;
	}

    public static void addMimeTypes(Object cs, List<String> types)
	{
		if(cs instanceof Collection)
		{
			for(String c : (Collection<String>)cs)
			{
				if(!types.contains(c))
					types.add(c);
			}
		}
		else if(cs instanceof String)
		{
			String c = (String)cs;
			if(!types.contains(c))
				types.add(c);
		}
	}

    /**
	 *  Get metainfo about parameters from the target method via annotations.
	 */
	public Tuple2<List<Tuple2<String, String>>, Map<String, Class<?>>> getParameterInfos(Method method)
	{
		List<Tuple2<String, String>> ret = new ArrayList<>();
		Map<String, Class<?>> targettypes = new HashMap<>();
		
		Annotation[][] anns = method.getParameterAnnotations();
		Class<?>[] types = method.getParameterTypes();
		
		for(int i=0; i<anns.length; i++)
		{
			boolean done = false;
			for(Annotation ann: anns[i])
			{
				if(ann instanceof PathParam)
				{
					PathParam pp = (PathParam)ann;
					String name = pp.value();
					ret.add(new Tuple2<String, String>("path", name));
					targettypes.put(name, types[i]);
					done = true;
					break;
				}
				else if(ann instanceof QueryParam)
				{
					QueryParam qp = (QueryParam)ann;
					String name = qp.value();
					ret.add(new Tuple2<String, String>("query", name));
					targettypes.put(name, types[i]);
					done = true;
					break;
				}
				else if(ann instanceof FormParam)
				{
					FormParam qp = (FormParam)ann;
					String name = qp.value();
					ret.add(new Tuple2<String, String>("form", name));
					targettypes.put(name, types[i]);
					done = true;
					break;
				}
//				else if(ann instanceof ParameterInfo)
//				{
//					ParameterInfo qp = (ParameterInfo)ann;
//					String name = qp.value();
//					ret.add(new Tuple2<String, String>("name", name));
//					targettypes.put(name, types[i]);
//					done = true;
//					break;
//				}
				/*else
				{
					String name = ""+i;
					ret.add(new Tuple2<String, String>("no", name));
					targettypes.put(name, types[i]);
				}*/
	        }
			if(!done) //if(anns[i].length==0)
			{
				String name = ""+i;
				ret.add(new Tuple2<String, String>("no", name));
				targettypes.put(name, types[i]);
			}
		}
		
		return new Tuple2<>(ret, targettypes);
	}

    /**
	 *  Convert a json string to a java object.
	 *  @param val The json string.
	 *  @param type The target class.
	 *  @param cl The classloader.
	 *  @param tomap Flag, if a (nested) map should be read (only possible if type is map too).
	 */
	public static Object convertJsonValue(String val, Class<?> type, ClassLoader cl, boolean tomap)
	{
		List<ITraverseProcessor> procs = null;

		if(tomap && SReflect.isSupertype(Map.class, type))
			procs = JsonTraverser.nestedreadprocs;
		return JsonTraverser.objectFromString(val.toString(), cl, null, type, procs);
	}
	
	/**
	 * Convert a (string) parameter
	 * 
	 * @param val
	 * @param target
	 * @return
	 * /
	public Object convertParameter(Object val, Class<?> target)
	{
		Object ret = null;

		IStringConverter conv = PublishJsonSerializer.getBasicTypeSerializer();

		if(val != null && SReflect.isSupertype(target, val.getClass()))
		{
			ret = val;
		}
		else if(val instanceof String && ((String)val).length() > 0 && conv!=null && conv.isSupportedType(target))
		{
			try
			{
				ret = conv.convertString((String)val, target, getClassLoader(), null);
			}
			catch(Exception e)
			{
			}
		}

		return ret;
	}*/

	/**
	 * Convert a parameter string to an object if is json or xml.
	 * 
	 * @param sr The media types.
	 * @param val The string value.
	 * @return The decoded object.
	 * /
	protected Object convertParameter(List<String> sr, Object val, Class<?> targetclazz)
	{
		Object ret = val;
		boolean done = false;

		if(val instanceof String)
		{
			if(sr != null && (sr.contains(MediaType.APPLICATION_JSON) || sr.contains(MediaType.WILDCARD)))
			{
				try
				{
					ret = jsonser.convertString((String)val, targetclazz, getClassLoader(), null);
					//ret = JsonTraverser.objectFromByteArray(val.getBytes(SUtil.UTF8), component.getClassLoader(), (IErrorReporter)null, null, targetclazz);
					// ret = JsonTraverser.objectFromByteArray(val.getBytes(),
					// component.getClassLoader(), (IErrorReporter)null);
					done = true;
				}
				catch(Exception e)
				{
					//e.printStackTrace();
				}
			}
	
			
			// todo?
			
			/*if(!done && sr != null && (sr.contains(MediaType.APPLICATION_XML) || sr.contains(MediaType.WILDCARD)))
			{
				try
				{
					ret = binser.decode(((String)val).getBytes(StandardCharsets.UTF_8), getClassLoader(), null, null, null);
					//ret = JavaReader.objectFromByteArray(val.getBytes(), component.getClassLoader(), null);
					done = true;
				}
				catch(Exception e)
				{
				}
			}* /
		}
				
		if(!done)
		{
			ret = convertParameter(val, targetclazz);
		}

		return ret;
	}*/

    /**
	 * Extract caller values like ip and browser.
	 * 
	 * @param request The requrest.
	 * @param vals The values.
	 */
	public Map<String, String> extractCallerValues(Object request)
	{
		Map<String, String> ret = new HashMap<String, String>();

		// add request to map as internal parameter
		// cannot put request into map because map is cloned via service call
		if(request != null)
		{
			// if(request instanceof Request)
			// {
			// Request greq = (Request)request;
			// ret.put("ip", greq.getRemoteAddr());
			// ret.put("browser", greq.getHeader("User-Agent"));
			// ret.put("querystring", greq.getQueryString());
			// }
			// else
			if(request instanceof HttpServletRequest)
			{
				HttpServletRequest sreq = (HttpServletRequest)request;
				ret.put("ip", sreq.getRemoteAddr());
				ret.put("browser", sreq.getHeader("User-Agent"));
				ret.put("querystring", sreq.getQueryString());
			}
		}

		return ret;
	}


    public static ClassLoader getClassLoader()
    {
        return ComponentManager.get().getClassLoader();
    }
}
