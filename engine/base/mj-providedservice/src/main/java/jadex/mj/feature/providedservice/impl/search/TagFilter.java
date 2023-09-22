package jadex.mj.feature.providedservice.impl.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import jadex.common.SReflect;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.mj.core.IAsyncFilter;
import jadex.mj.core.IComponent;

/**
 *  Tag filter class. Allows for filtering according to a collection of tags.
 *  Includes only services that contain all the tags.
 *  Replaces variables to dynamic values and uses TagProperty.createRuntimeTags() for that.
 */
public class TagFilter<T> implements IAsyncFilter<T>
{
	/** The name of the property. */
	public static final String NAME = "tag";
	
	/** The argument constant. */
	public static final String ARGUMENT = "argument";
	
	/** The key used to store the tags in the service property map. */
	public static final String SERVICE_PROPERTY_NAME = "__service_tags__";
	
	/** The platform name tag. */
	private static final String PLATFORM_NAME_INTERNAL = "platform_name";
	public static final String PLATFORM_NAME = "\""+PLATFORM_NAME_INTERNAL+"\"";
	
	/** The Jadex version tag. */
	private static final String JADEX_VERSION_INTERNAL = "jadex_version";
	public static final String JADEX_VERSION = "\""+JADEX_VERSION_INTERNAL+"\"";
	
	/** The component. */
	protected IComponent component;
	
	/** The search tags. */
	protected Collection<String> tags;
	
	public TagFilter()
	{
		// TODO Auto-generated constructor stub
	}
	
	/**
	 *  Create a new tag filter.
	 */
	public TagFilter(IComponent component, String... tags)
	{
		this(component, tags==null? Collections.EMPTY_LIST: Arrays.asList(tags));
	}
	
	/**
	 *  Create a new tag filter.
	 */
	public TagFilter(IComponent component, Collection<String> tags)
	{
		this.component = component;
		this.tags = createRuntimeTags(tags);//, component);
	}

	/**
	 *  Filter if a service contains all the tags.
	 */
	public IFuture<Boolean> filter(T ts)
	{
		final Future<Boolean> ret = new Future<Boolean>();
		
		// todo: save and fetch tags 
		
		/*IFuture<Collection<String>> fut = component.getNFPropertyValue(((IService)ts).getServiceId(), NAME);
		fut.addResultListener(new ExceptionDelegationResultListener<Collection<String>, Boolean>(ret)
		{
			public void customResultAvailable(Collection<String> result)
			{
//				System.out.println("ser tag check: "+result);
				ret.setResult(result!=null && result.containsAll(tags));
			}
		});*/
		return ret;
	}

	/**
	 * @return the component
	 * /
	public IExternalAccess getComponent()
	{
		return component;
	}*/

	/**
	 *  Sets the component.
	 *  @param component The component to set
	 * /
	public void setComponent(IExternalAccess component)
	{
		this.component = component;
	}*/

	/**
	 * @return the tags
	 */
	public Collection<String> getTags()
	{
		return tags;
	}

	/**
	 *  Sets the tags.
	 *  @param tags The tags to set
	 */
	public void setTags(Collection<String> tags)
	{
		this.tags = tags;
	}
	
	
	/**
	 *  Create a collection of tags and replace the variable values.
	 */
	public static Collection<String> createRuntimeTags(Object vals)
	{
		Collection<String> tags = convertToCollection(vals);
		Iterator<String> it = tags.iterator();
		List<String> ret = new ArrayList<String>();
		for(int i=0; i<tags.size(); i++)
		{
			String tag = it.next();
			/*if(PLATFORM_NAME_INTERNAL.equals(tag) || PLATFORM_NAME.equals(tag))
			{
				tag = component.getId().getPlatformPrefix();
			}
			else if(JADEX_VERSION_INTERNAL.equals(tag) || JADEX_VERSION.equals(tag))
			{
				tag = VersionInfo.getInstance().getVersion();
			}*/
			ret.add(tag);
		}
		return ret;
	}
	
	/**
	 *  Convert user defined tag(s) to collection.
	 */
	protected static Collection<String> convertToCollection(Object obj)
	{
		Collection<String> ret = null;
		
		if(obj==null)
		{
			ret = Collections.emptyList();
		}
		else if(obj instanceof String)
		{
			ret = new ArrayList<String>();
			ret.add((String)obj);
		}
		else if(obj instanceof Collection)
		{
			ret = (Collection<String>)obj; 
		}
		else if(SReflect.isIterable(obj))
		{
			ret = new ArrayList<String>();
			Iterator<String> it = (Iterator)SReflect.getIterable(obj).iterator();
			while(it.hasNext())
			{
				ret.add(it.next());
			}
		}
		else
		{
			ret = new ArrayList<String>();
			ret.add(""+obj);
		}
		
		return ret;
	}
	
}