package jadex.autostart;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.System.Logger.Level;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import jadex.core.IComponentManager;

public class AutostartFeature implements IAutostartFeature
{
	public AutostartFeature()
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
		
		for (String clname : names) 
		{
			try
			{
				Class<?> clazz = Class.forName(clname, false, cl);
				IComponentManager.get().create(clazz.getDeclaredConstructor().newInstance()); // wait for component handle in loop?
			}
			catch(Exception e)
			{
				System.getLogger(this.getClass().getName()).log(Level.ERROR, "Could not create autostart component: "+clname+" "+e.getMessage());
				e.printStackTrace();
			}
		}
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
}
