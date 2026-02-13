package jadex.autostart;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.System.Logger.Level;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import jadex.common.IValueFetcher;
import jadex.common.SReflect;
import jadex.common.transformation.BasicTypeConverter;
import jadex.core.IComponentManager;
import jadex.core.impl.ILifecycle;
import jadex.javaparser.SJavaParser;

public class AutostartFeature implements IAutostartFeature, ILifecycle
{
	private static BasicTypeConverter converter = new BasicTypeConverter();
	
	static IValueFetcher fetcher = new IValueFetcher() 
	{
		@Override
		public Object fetchValue(String name) 
		{
			Object ret = null;
			String key = name;

		    // ${var} → var
		    if (key.startsWith("${") && key.endsWith("}")) 
		        key = key.substring(2, key.length() - 1);
		    
		    // $var → var
		    else if (key.startsWith("$")) 
		        key = key.substring(1);

		    ret = System.getProperty(key);
		    if (ret == null) 
		    	ret = System.getenv(key);

		    //System.out.println("found: "+key+"="+ret);
		    
		    return ret; 
		}
	};
	
	public AutostartFeature()
	{

	}

	protected static String emptyToNull(String s) 
	{
	    return (s == null || s.trim().isEmpty()) ? null : s.trim();
	}

	protected static Object[] parseExpressions(String exprs) throws Exception 
	{
		String[] exprar = exprs.split(",");
		Object[] args = new Object[exprar.length];
		for (int i = 0; i < exprar.length; i++) 
		{
			if (exprar[i].trim().isEmpty()) 
				continue;
			try
			{
				args[i] = SJavaParser.evaluateExpression(exprar[i].trim(), fetcher);
			} 
			catch (Exception e) 
			{
				System.getLogger(AutostartFeature.class.getName()).log(Level.ERROR, 
					"Error parsing expression: " + exprar[i] + " " + e.getMessage());
			}
		}
		return args;
	}

	private static Object createInstanceWithArgs(Class<?> clazz, Object[] args, IValueFetcher fetcher) throws Exception 
	{
	    for (Constructor<?> ctor : clazz.getDeclaredConstructors()) 
	    {
	        if (ctor.getParameterCount() == args.length) 
	        {
	            Class<?>[] paramTypes = ctor.getParameterTypes();
	            Object[] converted = new Object[args.length];

	            for (int i = 0; i < args.length; i++) 
	            {
	                Object arg = args[i];

	                if (arg instanceof String && !paramTypes[i].isAssignableFrom(String.class)) 
	                {
	                    try 
	                    {
	                        arg = convertParameter(arg, paramTypes[i]);
	                    } 
	                    catch (Exception e) 
	                    {
	                        throw new IllegalArgumentException("Could not convert arg[" + i + "] to " + paramTypes[i], e);
	                    }
	                }

	                converted[i] = arg;
	            }
	            return ctor.newInstance(converted);
	        }
	    }
	    throw new NoSuchMethodException("No matching constructor found for " + clazz.getName());
	}
	
	/**
	 * Convert a (string) parameter
	 * 
	 * @param val
	 * @param target
	 * @return
	 */
	public static Object convertParameter(Object val, Class<?> target)
	{
		Object ret = null;

		if(val != null && SReflect.isSupertype(target, val.getClass()))
		{
			ret = val;
		}
		else if(val instanceof String && ((String)val).length() > 0 && converter.isSupportedType(target))
		{
			try
			{
				ret = converter.convertString((String)val, target, null);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}

		return ret;
	}
	
	public List<String> findServiceClassNames(Class<?> iface)  
	{
		List<String> names = new ArrayList<>();
		try
		{
		    String resource = "META-INF/services/" + iface.getName();
		    Enumeration<URL> urls = IComponentManager.get().getClassLoader().getResources(resource);
	
		    while (urls.hasMoreElements()) 
		    {
		        URL url = urls.nextElement();
		        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) 
		        {
		            reader.lines().map(String::trim).filter(line -> !line.isEmpty() && !line.startsWith("#")).forEach(names::add);
		        }
		    }
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	    return names;
	}

	@Override
	public void init()
	{
		//System.out.println("created: "+this);

		ClassLoader cl = IComponentManager.get().getClassLoader();

		// init auto startup components

		//Iterator<IAutostart> it = ServiceLoader.load(IAutostart.class, cl).iterator();

		List<String> names = findServiceClassNames(IAutostartManual.class);
		List<String> aunames = findServiceClassNames(IAutostartGenerated.class);
		names.addAll(aunames);

		System.getLogger(this.getClass().getName()).log(Level.INFO, "Found autostart agents: "+names.size()+" "+names);
		//System.out.println("found autostart agents: "+names);

		for (String entry : names)
		{
			try
			{
				String localName = null;
				String argsString = null;
				String className = null;

				// Split max. in 3 parts: localName|args|className

				String[] parts = entry.split("\\|", -1);
				if (parts.length == 1)
				{
					className = parts[0].trim();
				}
				else if (parts.length == 2)
				{
					localName = emptyToNull(parts[0]);
					className = parts[1].trim();
				}
				else if (parts.length >= 3)
				{
					localName = emptyToNull(parts[0]);
					argsString = emptyToNull(parts[1]);
					className = parts[2].trim();
				}

				Class<?> clazz = Class.forName(className, false, cl);

				Object instance;
				if (argsString != null)
				{
					Object[] args = parseExpressions(argsString);
					instance = createInstanceWithArgs(clazz, args, fetcher);
				}
				else
				{
					instance = clazz.getDeclaredConstructor().newInstance();
				}

				IComponentManager.get().create(instance, localName).get();
			}
			catch (Exception e)
			{
				System.getLogger(getClass().getName()).log(Level.ERROR, "Could not create autostart component: " + entry + " " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	@Override
	public void cleanup()
	{

	}
}
