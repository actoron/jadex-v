package jadex.publishservice.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import jadex.common.SReflect;
import jadex.common.Tuple2;
import jadex.future.Future;
import jadex.providedservice.IServiceIdentifier;
import jadex.publishservice.impl.MappingEvaluator.MappingInfo.HttpMethod;
import jadex.publishservice.publish.PathManager;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

public class MappingEvaluator 
{
    /**
	 *  Mapping information for a single method.
	 */
	public static class MappingInfo
	{
		public enum HttpMethod
		{
			GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH
		}

        protected static final Map<Class<? extends Annotation>, HttpMethod> HTTP_METHODS = Map.of(
            GET.class, HttpMethod.GET,
            POST.class, HttpMethod.POST,
            PUT.class, HttpMethod.PUT,
            DELETE.class, HttpMethod.DELETE,
            OPTIONS.class, HttpMethod.OPTIONS,
            HEAD.class, HttpMethod.HEAD,
            PATCH.class, HttpMethod.PATCH
        );

		/** The http method. */
		protected HttpMethod httpmethod;

		/** The target method. */
		protected Method method;

		/** The url path. */
		protected String path;

		/** The accepted media types for the response. */
		protected List<String> producedtypes = null;

		/** The accepted media types for consumption. */
		protected List<String> consumedtypes = null;

		/**
		 * Create a new mapping info.
		 */
		public MappingInfo()
		{
		}

		/**
		 * Create a new mapping info.
		 */
		public MappingInfo(HttpMethod httpmethod, Method method, String path)
		{
			this.httpmethod = httpmethod;
			this.method = method;
			this.path = path;

            if(httpmethod==null)
                throw new RuntimeException("Http method must not be null for mapping / method: "+method);
		}

		/**
		 * Get the httpMethod.
		 * 
		 * @return The httpMethod
		 */
		public HttpMethod getHttpMethod()
		{
			return httpmethod;
		}

		/**
		 * Set the httpMethod.
		 * 
		 * @param httpMethod The httpMethod to set
		 */
		public void setHttpMethod(HttpMethod httpMethod)
		{
			this.httpmethod = httpMethod;
		}

		/**
		 * Get the method.
		 * 
		 * @return The method
		 */
		public Method getMethod()
		{
			return method;
		}

		/**
		 * Set the method.
		 * 
		 * @param method The method to set
		 */
		public void setMethod(Method method)
		{
			this.method = method;
		}

		/**
		 * Get the path.
		 * 
		 * @return The path
		 */
		public String getPath()
		{
			return path;
		}

		/**
		 * Set the path.
		 * 
		 * @param path The path to set
		 */
		public void setPath(String path)
		{
			this.path = path;
		}

		/**
		 * Test if has no settings.
		 */
		public boolean isEmpty()
		{
			return path == null && method == null && httpmethod == null;
		}
		
		/**
		 *  Get the declared parameter names (via annotation ParameterInfo).
		 *  @return The parameter names. 
		 */
		public List<String> getParameterNames()
		{
			List<String> ret = new ArrayList<String>();
			Annotation[][] anns = method.getParameterAnnotations();
			for(Annotation[] ans: anns)
			{
				for(Annotation an: ans)
				{
					/*if(an instanceof ParameterInfo)
					{
						ParameterInfo p = (ParameterInfo)an;
						String name = p.value();
						ret.add(name);
						break;
					}
					else*/ if(an instanceof QueryParam)
					{
						QueryParam p = (QueryParam)an;
						String name = p.value();
						ret.add(name);
						break;
					}
					else if(an instanceof PathParam)
					{
						PathParam p = (PathParam)an;
						String name = p.value();
						ret.add(name);
						break;
					}
					else if(an instanceof FormParam)
					{
						FormParam p = (FormParam)an;
						String name = p.value();
						ret.add(name);
						break;
					}
				}
			}
			return ret;
		}
		
		/**
		 *  Get the consumed media types.
		 *  @return The types.
		 */
		//public void addConsumedMediaTypes()
		public List<String> getConsumedMediaTypes()
		{
            if(consumedtypes == null)
                consumedtypes = new ArrayList<String>();
            
			if(method.isAnnotationPresent(Consumes.class))
			{
				Consumes con = (Consumes)method.getAnnotation(Consumes.class);
				for(String type : con.value())
				{
					consumedtypes.add(type);
				}
			}
			return consumedtypes;
		}
		
		/**
		 *  Get the produced media types.
		 *  @return The types.
		 */
		public List<String> getProducedMediaTypes()
		{
            if(producedtypes == null)
                producedtypes = new ArrayList<String>();

			if(method.isAnnotationPresent(Produces.class))
			{
				Produces prod = (Produces)method.getAnnotation(Produces.class);
				for(String type : prod.value())
				{
					producedtypes.add(type);
				}
			}
			return producedtypes;
		}
	}

    /**
	 *  Evaluate the service interface and generate mappings. Return a
	 *  multicollection in which for each path name the possible methods are
	 *  contained (can be more than one due to different parameters).
	 */
	public static PathManager<MappingInfo> evaluateMapping(IServiceIdentifier sid, PublishInfo pi, ClassLoader cl)
	{
		Future<PathManager<MappingInfo>> reta = new Future<>();

        Class<?> mapcl = pi.getMapping() == null ? null : pi.getMapping().getType(cl);
        if(mapcl == null)
            mapcl = sid.getServiceType().getType(cl);

        PathManager<MappingInfo> ret = new PathManager<MappingInfo>();
        PathManager<MappingInfo> natret = new PathManager<MappingInfo>();
        List<MappingInfo> natrest = new ArrayList<>();

        for(Method m : SReflect.getAllMethods(mapcl))
        {
            MappingInfo mi = new MappingInfo();

            // Natural mapping using simply all declared methods
            MappingInfo mi2 = new MappingInfo(guessRestType(m), m, m.getName());
 
            MappingInfo.HTTP_METHODS.forEach((annotation, method) -> 
            {
                if (m.isAnnotationPresent(annotation)) 
                {
                    mi.setHttpMethod(method);
                }
            });
            
            if(m.isAnnotationPresent(Path.class))
            {
                Path path = m.getAnnotation(Path.class);
                mi.setPath(path.value());
                mi2.setPath(path.value());
            }
            else if(!mi.isEmpty())
            {
                // default rest path is method name
                mi.setPath(m.getName());
            }

            if(!mi.isEmpty())
            {
                //mi.addConsumedMediaTypes();
                //mi.addProducedMediaTypes();
                mi.setMethod(m);
                ret.addPathElement(mi.getPath(), mi);
                //ret.add(mi.getPath(), mi);
            }
            else
            {
                natrest.add(mi2);
            }            

            natret.addPathElement(mi2.getPath(), mi2);
        }

        /*if(pi.isAutoMapping() && ret.size() > 0)
        {
           natrest.forEach(mi -> 
           {
               ret.addPathElement(mi.getPath(), mi);
           });
        }*/

        if (pi.isAutoMapping() && ret.size() > 0)
        {
            natrest.forEach(mi ->
            {
                // no double mapping of methods
                boolean alreadymapped = ret.getElements().stream()
                    .anyMatch(r -> r.getMethod().equals(mi.getMethod()));

                // no double mapping of paths
                boolean pathexists = ret.contains(mi.getPath());

                if(!alreadymapped && !pathexists)
                {
                    ret.addPathElement(mi.getPath(), mi);
                    System.out.println("Automapped method for service: " + sid + " -> " + mi.getMethod().getName()+" "+mi.getHttpMethod());
                }
            });
        }

        reta.setResult(ret.size() > 0 ? ret : natret);

        //System.out.println("evaluated mapping for service: "+sid+" -> "+(ret.size() > 0 ? ret : natret));

        return ret.size() > 0 ? ret : natret;
	}

    /**
	 *  Guess the http type (GET, POST, PUT, DELETE, ...) of a method.
	 * 
	 *  @param method The method.
	 *  @return The rs annotation of the method type to use
	 * /
	public HttpMethod guessRestType(Method method)
	{
		// Retrieve = GET hasret || name.startsWith("get")
		// Update = POST hasparams || name.startsWith("set")
		// Create = PUT hasret || name.startsWith("create")
		// Delete = DELETE name.startsWith("delete") || name.startsWith("remove")

		HttpMethod ret = HttpMethod.GET;

		Class<?> rettype = SReflect.unwrapGenericType(method.getGenericReturnType());
		Class<?>[] paramtypes = method.getParameterTypes();

		boolean hasparams = paramtypes.length > 0;
		boolean hasret = rettype != null && !rettype.equals(Void.class) && !rettype.equals(void.class);
        String name = method.getName();

        if (name.startsWith("get") || name.startsWith("find")) 
            ret = HttpMethod.GET;
        else if (name.startsWith("create") || name.startsWith("add")) 
            ret = HttpMethod.POST;
        else if (name.startsWith("update") || name.startsWith("set")) 
            ret = HttpMethod.PUT;
        else if (name.startsWith("delete") || name.startsWith("remove")) 
            ret = HttpMethod.DELETE;
        else if (hasret && !hasparams) 
            ret = HttpMethod.GET;
        else if (!hasret && hasparams) 
            ret = HttpMethod.PUT;

		// System.out.println("rest-type: "+ret.getName()+" "+method.getName()+"
		// "+hasparams+" "+hasret);

		return ret;
		// return GET.class;
	}*/

    public static HttpMethod guessRestType(Method method) 
    {
		HttpMethod ret = HttpMethod.GET; 

		try
{
        String name = method.getName().toLowerCase(Locale.ROOT);

        Class<?> rettype = method.getReturnType(); //SReflect.unwrapGenericType(method.getGenericReturnType());
        Class<?>[] paramtypes = method.getParameterTypes();

        boolean hasParams = paramtypes.length > 0;
        boolean hasReturn = rettype != null && !rettype.equals(Void.class) && !rettype.equals(void.class);

        if (startsWithAny(name, "get", "find", "list", "load", "fetch", "all", "count", "exists", "check")) 
        {
            ret = HttpMethod.GET;
        }
        else if (startsWithAny(name, "create", "add", "submit", "upload", "send")) 
        {
            ret = HttpMethod.POST;
        }
        else if (startsWithAny(name, "update", "set", "enable", "disable", "toggle", "replace")) 
        {
            ret = HttpMethod.PUT;
        }
        else if (startsWithAny(name, "patch", "partial")) 
        {
            ret = HttpMethod.PATCH;
        }
        else if (startsWithAny(name, "delete", "remove", "clear", "purge")) 
        {
            ret = HttpMethod.DELETE;
        }
        else if (startsWithAny(name, "process", "run", "execute", "trigger", "invoke")) 
        {
            ret = HttpMethod.POST;
        }
        else if (hasReturn && !hasParams) 
        {
            ret = HttpMethod.GET;
        }
        else if (!hasReturn && hasParams) 
        {
            ret = HttpMethod.PUT;
        }
	}
	catch(Exception e)
	{
		e.printStackTrace();
	}

        return ret;
    }

    /**
     *  Check if begins with any of the given prefixes.
     *  @param name The name.
     *  @param prefixes The prefixes.
     *  @return True, if starts with any.
     */
    private static boolean startsWithAny(String name, String... prefixes) 
    {
        for (String prefix : prefixes) 
        {
            if (name.startsWith(prefix)) 
            {
                return true;
            }
        }
        return false;
    }

    /**
	 *  Test if a method has parameters that are all convertible from string.
	 * 
	 *  @param method The method.
	 *  @param rettype The return types (possibly unwrapped from future type).
	 *  @param paramtypes The parameter types.
	 *  @return True, if is convertible.
	 */
	public boolean hasStringConvertableParameters(Method method, Class<?> rettype, Class<?>[] paramtypes)
	{
		boolean ret = true;

		for(int i = 0; i < paramtypes.length && ret; i++)
		{
			ret = SReflect.isStringConvertableType(paramtypes[i]);
		}

		return ret;
	}

}
