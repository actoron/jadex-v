package jadex.common;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 *  Scan functionality.
 */
public class SScan 
{
	/**
	 *  Scan for classes that fulfill certain criteria as specified by the file and classfilters.
	 */
	public static Class<?>[] scanForClasses(ClassLoader classloader, IFilter filefilter, IFilter classfilter, boolean includebootpath)
	{
		return scanForClasses(SUtil.getClasspathURLs(classloader, includebootpath).toArray(new URL[0]), classloader, filefilter, classfilter);
	}
	
	/**
	 *  Scan for classes that fulfill certain criteria as specified by the file and classfilters.
	 */
	public static Class<?>[] scanForClasses(URL[] urls, ClassLoader classloader, IFilter filefilter, IFilter classfilter)
	{
		Set<Class<?>>	ret	= new HashSet<Class<?>>();
		String[] facs = scanForFiles(urls, filefilter);
		try
		{
			for(int i=0; i<facs.length; i++)
			{
				try
				{
					String	clname	= facs[i].substring(0, facs[i].length()-6).replace('/', '.');
	//				System.out.println("Found candidate: "+clname);
					Class<?>	fac	= SReflect.findClass0(clname, null, classloader);
					
					if(fac!=null && classfilter.filter(fac))
					{
						ret.add(fac);
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
					System.out.println(facs[i]);
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return ret.toArray(new Class[ret.size()]);
	}

	/**
	 *  Scan for files in a given list of urls.
	 */
	public static String[] scanForFiles(URL[] urls, IFilter<Object> filter)
	{
		Set<String>	ret	= new HashSet<String>();
		for(int i=0; i<urls.length; i++)
		{
//			System.out.println("Scanning: "+entry);
			try
			{
//				System.out.println("url: "+urls[i].toURI());
				File f = new File(urls[i].toURI());
				if(f.getName().endsWith(".jar"))
				{
					JarFile	jar = null;
					try
					{
						jar	= new JarFile(f);
						for(Enumeration<JarEntry> e=jar.entries(); e.hasMoreElements(); )
						{
							JarEntry je	= e.nextElement();
							if(filter.filter(je))	
							{
								ret.add(je.getName());
							}
						}
						jar.close();
					}
					catch(Exception e)
					{
//						System.out.println("Error opening jar: "+urls[i]+" "+e.getMessage());
					}
					finally
					{
						if(jar!=null)
						{
							jar.close();
						}
					}
				}
				else if(f.isDirectory())
				{
					scanDir(urls, f, filter, ret, new ArrayList<String>());
//					throw new UnsupportedOperationException("Currently only jar files supported: "+f);
				}
			}
			catch(Exception e)
			{
				System.out.println("scan problem with: "+urls[i]);
//				e.printStackTrace();
			}
		}
		
		return ret.toArray(new String[ret.size()]);
	}
	
	// This cache cannot really work due to a key with plain objects like filters (other filter object = new entry)
	//	protected static Map<Tuple3<Set<URL>, IFilter<Object>, IFilter<ClassInfo>>, Set<ClassInfo>> CICACHE	= Collections.synchronizedMap(new LinkedHashMap<>());
	
	/**
	 *  scanForFiles2 returns a map instead of a list of filenames.
	 *  In the map the first part is the jarpath and the set contains found entry names/path.
	 * 
	 *  Scan for files in a given list of urls.
	 *  @return Map of files matching the filter in the following format:
	 *  <jarpath>[entrypath1,entrypath2,...]
	 *  jarpath is null for directories
	 */
	public static Map<String, Set<String>> scanForFiles2(URL[] urls, IFilter<Object> filter)
	{
		Map<String, Set<String>> ret = new HashMap<>();
		Set<String>	topset = new HashSet<String>();
		
		for(int i=0; i<urls.length; i++)
		{
//			System.out.println("Scanning: "+entry);
			try
			{
//				System.out.println("url: "+urls[i].toURI());
				File f = new File(urls[i].toURI());
				if(f.getName().endsWith(".jar"))
				{
					JarFile	jar = null;
					try
					{
						jar	= new JarFile(f);
						Set<String>	set	= new HashSet<String>();
						
						for(Enumeration<JarEntry> e=jar.entries(); e.hasMoreElements(); )
						{
							JarEntry je	= e.nextElement();
							if(filter.filter(je))	
							{
								set.add(je.getName());
							}
						}
						jar.close();
					
						if(set.size()>0)
							ret.put(f.getAbsolutePath(), set);
					}
					catch(Exception e)
					{
//						System.out.println("Error opening jar: "+urls[i]+" "+e.getMessage());
					}
					finally
					{
						if(jar!=null)
						{
							jar.close();
						}
					}
				}
				else if(f.isDirectory())
				{
					scanDir2(urls, f, filter, topset, new ArrayList<String>());
//					throw new UnsupportedOperationException("Currently only jar files supported: "+f);
				}
			}
			catch(Exception e)
			{
				System.out.println("scan problem with: "+urls[i]);
//				e.printStackTrace();
			}
		}
		
		if(topset.size()>0)
			ret.put(null, topset);
		
		return ret;
	}
	

	
	/**
	 *  Scan directories.
	 */
	public static void scanDir(URL[] urls, File file, IFilter<Object> filter, Collection<String> results, List<String> donedirs)
	{
		File[] files = file.listFiles(new FileFilter()
		{
			public boolean accept(File f)
			{
				return !f.isDirectory();
			}
		});
		for(File fi: files)
		{
			//if(fi.getName().endsWith(".class") && filter.filter(fi))
			if(filter.filter(fi))
			{
				//String fn = SUtil.convertPathToPackage(fi.getAbsolutePath(), urls);
//				System.out.println("fn: "+fi.getName());
				//results.add(fn+"."+fi.getName());
				results.add(fi.getAbsolutePath());
			}
		}
		
		if(file.isDirectory())
		{
			donedirs.add(file.getAbsolutePath());
			File[] sudirs = file.listFiles(new FileFilter()
			{
				public boolean accept(File f)
				{
					return f.isDirectory();
				}
			});
			
			for(File dir: sudirs)
			{
				if(!donedirs.contains(dir.getAbsolutePath()))
				{
					scanDir(urls, dir, filter, results, donedirs);
				}
			}
		}
	}
	
	/**
	 *  Scan directories.
	 */
	public static void scanDir2(URL[] urls, File file, IFilter<Object> filter, Collection<String> results, List<String> donedirs)
	{
		File[] files = file.listFiles(new FileFilter()
		{
			public boolean accept(File f)
			{
				return !f.isDirectory();
			}
		});
		for(File fi: files)
		{
			if(filter.filter(fi))
			{
				//String fn = SUtil.convertPathToPackage(fi.getAbsolutePath(), urls);
//				System.out.println("fn: "+fi.getName());
				results.add(fi.getAbsolutePath());
			}
		}
		
		if(file.isDirectory())
		{
			donedirs.add(file.getAbsolutePath());
			File[] sudirs = file.listFiles(new FileFilter()
			{
				public boolean accept(File f)
				{
					return f.isDirectory();
				}
			});
			
			for(File dir: sudirs)
			{
				if(!donedirs.contains(dir.getAbsolutePath()))
				{
					scanDir2(urls, dir, filter, results, donedirs);
				}
			}
		}
	}

}
