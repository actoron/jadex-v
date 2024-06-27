package jadex.logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import jadex.common.SUtil;
import jadex.core.impl.ComponentManager;

/**
 *  A scanner that can find all system packages.
 *  A marker file '.system' is used to mark root paths.
 *  Within those root paths all subpaths are added, which
 *  contain at least one class file.
 */
public class SystemPackageScanner 
{
    protected static final String MARKER_FILE = ".system";

    public static void main(String[] args) 
    {
    	long start = System.currentTimeMillis();
        Set<String> names = getSystemPackages();
        names.forEach(System.out::println);
        long end = System.currentTimeMillis();
        
        System.out.println("Needed: "+(end-start));
    }

    public static Set<String> getSystemPackages() 
    {
        Set<String> ret = new HashSet<>();
        
        try
        {
        	List<URL> urls = SUtil.getClasspathURLs(ComponentManager.get().getClassLoader(), true);
	        
        	for(URL url: urls)
	        {
	            String protocol = url.getProtocol();

	            if(protocol.equals("file")) 
	            {
	                String path = URLDecoder.decode(url.getFile(), "UTF-8");
	                //String path = url.getPath();
	                File file = new File(path);
	                
	                if(file.isDirectory()) 
	                {
	                    findSystemPackagesInDirectory(file, "", ret);
	                } 
	                else if(path.endsWith(".jar")) 
	                {
	  	                //String jpath = path.substring(5, path.indexOf("!"));
	  	                try(JarFile jfile = new JarFile(file))
	  	                {
	  	                    findSystemPackagesInJar(jfile, ret);
	  	                }
	                }
	            } 
	            else if(protocol.equals("jar"))
	            {
	            	String path = URLDecoder.decode(url.getFile(), "UTF-8");
	            	//String path = url.getPath();
	            	int idx = path.indexOf("!");
	            	String jpath = idx!=-1? path.substring(4, idx): path;
	            	try(JarFile jfile = new JarFile(jpath))
  	                {
  	                    findSystemPackagesInJar(jfile, ret);
  	                }
	            }
	            else 
	            {
	            	System.out.println("Unknown protocol: "+protocol);
	            }
	        }
        }
        catch(IOException e)
        {
        	SUtil.rethrowAsUnchecked(e);
        }
        
        return ret;
    }

    private static void findSystemPackagesInDirectory(File dir, String pckname, Set<String> ret) 
    {
        File[] files = dir.listFiles();
        if(files == null) 
            return;
        
        boolean system = false;
        for(File file : files) 
        {
            if(file.isFile() && file.getName().equals(MARKER_FILE)) 
            {
                system = true;
                break;
            }
        }
        
        if(system) 
        {
            addPackagesFromDirectory(dir, pckname, ret);
        } 
        else 
        {
            for(File file : files) 
            {
                if(file.isDirectory()) 
                {
                    String name = pckname.isEmpty() ? file.getName() : pckname + "." + file.getName();
                    findSystemPackagesInDirectory(file, name, ret);
                }
            }
        }
    }

    private static void addPackagesFromDirectory(File dir, String pckame, Set<String> ret) 
    {
        File[] files = dir.listFiles();
        if(files == null) 
            return;
        
        for(File file : files) 
        {
            if(file.isDirectory()) 
            {
            	boolean hasclazz = false;
            	for(File f : file.listFiles()) 
            	{
            		if(f.isFile() && f.getName().endsWith(".class")) 
            		{
            			hasclazz = true;
            			break;
            		}
            	}
            	
                String name = pckame.isEmpty() ? file.getName() : pckame + "." + file.getName();
                if(hasclazz)
                	ret.add(name);
                addPackagesFromDirectory(file, name, ret);
            }
        }
    }

    private static void findSystemPackagesInJar(JarFile file, Set<String> ret) throws IOException 
    {
        Enumeration<JarEntry> entries = file.entries();
        String rootpck = null;

        while(entries.hasMoreElements()) 
        {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();

            if(name.endsWith(MARKER_FILE)) 
            {
            	rootpck = getPackageName(name);
            	break;
            }
        }

        if(rootpck!=null)
            addPackagesFromJar(file, rootpck, ret);
    }
    
    private static void addPackagesFromJar(JarFile file, String pckname, Set<String> ret) throws IOException 
    {
        Enumeration<JarEntry> entries = file.entries();
        String path = pckname.replace('.', '/');
        if(!path.isEmpty() && !path.endsWith("/")) 
            path += "/";
        
        while(entries.hasMoreElements()) 
        {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();

            if(name.startsWith(path)) 
            {
                if(entry.isDirectory()) 
                {
                	String subpckname = getPackageName(entry.getName());
                	// ret.add(subname.substring(0, subname.length() - 1));
                	if(hasClass(file, subpckname))
                	{
                		ret.add(subpckname);
                	}
                }
            }
        }
    }
    
    private static String getPackageName(String name)
    {
    	String ret = "";
    	int idx = name.lastIndexOf('/');
        if(idx > 0) 
            ret = name.substring(0, idx).replace('/', '.');
        return ret;
    }
    
    private static boolean hasClass(JarFile file, String pckname)
    {
    	boolean ret = false;
    	
    	Enumeration<JarEntry> entries = file.entries();
    	String path = pckname.replace('.', '/');

    	while(entries.hasMoreElements()) 
    	{
    		JarEntry entry = entries.nextElement();
    		String name = entry.getName();

    		if(entry.isDirectory() || !entry.getName().endsWith(".class"))
                continue;
    		
    		String ename = name.substring(0, name.lastIndexOf('/'));
            //ename = ename.replace('/', '.');
    		
    		//System.out.println("vgl: "+ename+" "+path);
    		if(ename.equals(path)) 
    		{
    			ret = true;
    			break; 
    		}
    	}
    	
    	return ret;
    }
}