package jadex.publishservice.impl.v2.invocation;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.providedservice.IService;
import jadex.publishservice.IRequestManager.PublishContext;
import jadex.publishservice.impl.MappingEvaluator;
import jadex.publishservice.impl.MappingEvaluator.MappingInfo;
import jadex.publishservice.impl.MappingEvaluator.MappingInfo.HttpMethod;
import jadex.publishservice.impl.v2.Request;
import jadex.publishservice.publish.PathManager;
import jakarta.ws.rs.core.MediaType;

public class ServiceInfoInvocation extends Invocation
{
	/** Some basic media types for service invocations. */
	public static final List<String> PARAMETER_MEDIATYPES = Arrays.asList(new String[]{MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML});

    protected String baseuri;

    protected PathManager<MappingInfo> pm;

    public ServiceInfoInvocation(Request request, PublishContext context, String baseuri) 
    {
        super(request, context);
        this.baseuri = baseuri;
    }

    public IFuture<InvocationResult> invoke()
    {
	    return new Future<>(new InvocationResult(getServiceInfo(getContext().service(), this.baseuri, (PathManager<MappingInfo>)getContext().mapping())).addMetaInfo("content-type", MediaType.TEXT_HTML));
    }

    public String getServiceInfo(Object service, String baseuri, PathManager<MappingInfo> mappings)
	{
		StringBuffer ret = new StringBuffer();

		try
		{
			String functionsjs = loadFunctionJS();
			String stylecss = loadStyleCSS();

			ret.append("<html>");
			ret.append("\n");
			ret.append("<head>");
			ret.append("\n");
			ret.append("<style type=\"text/css\">>");
			ret.append(stylecss);
			ret.append("</style>");
			ret.append("\n");
			ret.append("<script type=\"text/javascript\">");
			ret.append(functionsjs);
			ret.append("</script>");
			ret.append("\n");
			ret.append("<script src=\"jadex.js\" type=\"text/javascript\"/></script>");
			ret.append("</head>");
			ret.append("\n");
			ret.append("<body>");
			ret.append("\n");

			ret.append("<div class=\"header\">");
			ret.append("\n");
			ret.append("<h1>");// Service Info for: ");
			String ifacename = ((IService)service).getServiceId().getServiceType().getTypeName();
			ret.append(SReflect.getUnqualifiedTypeName(ifacename));
			ret.append("</h1>");
			ret.append("\n");
			ret.append("</div>");
			ret.append("\n");

			ret.append("<div class=\"middle\">");
			ret.append("\n");

			// Class<?> clazz = service.getClass();
			// List<Method> methods = new ArrayList<Method>();
			// while(!clazz.equals(Object.class))
			// {
			// List<Method> l = SUtil.arrayToList(clazz.getDeclaredMethods());
			// methods.addAll(l);
			// clazz = clazz.getSuperclass();
			// }

			// Collections.sort(mappings, new MethodComparator());

			if(mappings != null)
			{
				for(MappingInfo mi : mappings.getElements())
				{
					Method method = mi.getMethod();
					HttpMethod restmethod = mi.getHttpMethod() != null ? mi.getHttpMethod() : MappingEvaluator.guessRestType(method);

					String path = mi.getPath() != null ? mi.getPath() : method.getName();
					List<String> consumed = mi.getConsumedMediaTypes();
					List<String> produced = mi.getProducedMediaTypes();

					// Use defaults if nothing is given
					if(consumed == null)
						consumed = PARAMETER_MEDIATYPES;
					if(produced == null)
						produced = PARAMETER_MEDIATYPES;

					Class< ? >[] ptypes = method.getParameterTypes();
					String[] pnames = new String[ptypes.length];
//					java.lang.annotation.Annotation[][] pannos = method.getParameterAnnotations();

					// Find parameter names
					for(int p = 0; p < ptypes.length; p++)
					{
//						for(int a = 0; a < pannos[p].length; a++)
//						{
//							if(pannos[p][a] instanceof ParameterInfo)
//							{
//								pnames[p] = ((ParameterInfo)pannos[p][a]).value();
//							}
//						}

						if(pnames[p] == null)
						{
							pnames[p] = "arg" + p;
						}
					}

					ret.append("<div class=\"method\">");
					ret.append("\n");

					ret.append("<div class=\"methodname\">");
					// ret.append("<i><b>");
					ret.append(method.getName());
					// ret.append("</b></i>");

					ret.append("(");
					if(ptypes != null && ptypes.length > 0)
					{
						for(int j = 0; j < ptypes.length; j++)
						{
							ret.append(SReflect.getUnqualifiedClassName(ptypes[j]));
							ret.append(" ");
							ret.append(pnames[j]);
							if(j + 1 < ptypes.length)
								ret.append(", ");
						}
					}
					ret.append(")");
					ret.append("</div>");
					ret.append("\n");
					// ret.append("</br>");

					ret.append("<div class=\"restproperties\">");
					ret.append("<div id=\"httpmethod\">").append(restmethod).append("</div>");

					if(consumed != null && consumed.size() > 0)
					{
						ret.append("<i>");

						if(consumed != PARAMETER_MEDIATYPES)
						{
							ret.append("Consumes: ");
						}
						else
						{
							ret.append("Consumes [not declared by the service]: ");
						}
						ret.append("</i>");
						for(int j = 0; j < consumed.size(); j++)
						{
							ret.append(consumed.get(j));
							if(j + 1 < consumed.size())
								ret.append(" ,");
						}
						ret.append(" ");
					}

					if(produced != null && produced.size() > 0)
					{
						ret.append("<i>");
						if(produced != PARAMETER_MEDIATYPES)
						{
							ret.append("Produces: ");
						}
						else
						{
							ret.append("Produces [not declared by the service]: ");
						}
						ret.append("</i>");
						for(int j = 0; j < produced.size(); j++)
						{
							ret.append(produced.get(j));
							if(j + 1 < produced.size())
								ret.append(" ,");
						}
						ret.append(" ");
					}
					// ret.append("</br>");
					ret.append("</div>");
					ret.append("\n");

					// String link = baseuri.toString();
					// if(path!=null) // Todo: cannot be null!?
					// link = link+"/"+path;
					String link = path; // Do not use absolute URL to allow
										// reverse proxying

					// System.out.println("path: "+link);

					// if(ptypes.length>0)
					// {
					ret.append("<div class=\"servicelink\">");
					ret.append(link);
					ret.append("</div>");
					ret.append("\n");

					// For post set the media type of the arguments.
					ret.append("<form class=\"arguments\" action=\"").append(link).append("\" method=\"").append(restmethod).append("\" enctype=\"multipart/form-data\" ");

					// if(restmethod.equals(HttpMethod.POST))
					ret.append("onSubmit=\"return extract(this)\"");
					ret.append(">");
					ret.append("\n");

					for(int j = 0; j < ptypes.length; j++)
					{
						ret.append(pnames[j]).append(": ");
						ret.append("<input name=\"").append(pnames[j]).append("\" type=\"text\" />");
						// .append(" accept=\"").append(cons[0]).append("\"
						// />");
					}

					ret.append("<select name=\"mediatype\">");
					if(consumed != null && consumed.size() > 0)
					{
						// ret.append("<select name=\"mediatype\">");
						for(int j = 0; j < consumed.size(); j++)
						{
							// todo: hmm? what about others?
							if(!MediaType.MULTIPART_FORM_DATA.equals(consumed.get(j)) && !MediaType.APPLICATION_FORM_URLENCODED.equals(consumed.get(j)))
							{
								ret.append("<option>").append(consumed.get(j)).append("</option>");
							}
						}
					}
					else
					{
						ret.append("<option>").append(MediaType.TEXT_PLAIN).append("</option>");
					}
					ret.append("</select>");
					ret.append("\n");

					ret.append("<input type=\"submit\" value=\"invoke\"/>");
					ret.append("</form>");
					ret.append("\n");
					// }
					// else
					// {
					// ret.append("<div class=\"servicelink\">");
					// ret.append("<a
					// href=\"").append(link).append("\">").append(link).append("</a>");
					// ret.append("</div>");
					// ret.append("\n");
					// }

					ret.append("</div>");
					ret.append("\n");
				}
			}

			ret.append("</div>");
			ret.append("\n");

			ret.append("<div id=\"result\"></div>");

			ret.append("<div class=\"powered stripes\"> <span class=\"powered\">powered by</span> <span class=\"jadex\">");
			// Add Jadex version header, if enabled
			/*if(Boolean.TRUE.equals(Starter.getPlatformArgument(component.getId(), "showversion")))
			{
				ret.append(VersionInfo.getInstance());
			}
			else
			{*/
				ret.append("Actoron");				
			//}
			ret.append("</span> <a class=\"jadexurl\" href=\"http://www.actoron.com\">http://www.actoron.com</a> </div>\n");
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		ret.append("</body>\n</html>\n");

		return ret.toString();
	}

    /**
	 *  Load functions.js
	 *  @return The text from the file.
	 */
	public String loadFunctionJS()
	{
		String functionsjs;

		Scanner sc = null;
		try
		{
			InputStream is = SUtil.getResource0(ResourceInvocation.getPath()+"functions.js", getClassLoader());
			sc = new Scanner(is);
			functionsjs = sc.useDelimiter("\\A").next();
			// System.out.println(functionsjs);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		finally
		{
			if(sc != null)
			{
				sc.close();
			}
		}

		return functionsjs;
	}

	/**
	 *  Load a css style sheet.
	 *  @return The text from the file.
	 */
	public String loadStyleCSS()
	{
		String stylecss;

		Scanner sc = null;
		try
		{

			InputStream is = SUtil.getResource0(ResourceInvocation.getPath()+"style.css", getClassLoader());
			sc = new Scanner(is);
			stylecss = sc.useDelimiter("\\A").next();

			//String stripes = SUtil.loadBinary(getPath()+"jadex_stripes.png");
			//stylecss = stylecss.replace("$stripes", stripes);

			// System.out.println(functionsjs);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		finally
		{
			if(sc != null)
			{
				sc.close();
			}
		}

		return stylecss;
	}
}
