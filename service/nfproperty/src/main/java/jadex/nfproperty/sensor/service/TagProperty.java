package jadex.nfproperty.sensor.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jadex.common.MethodInfo;
import jadex.common.SReflect;
import jadex.common.transformation.BeanIntrospectorFactory;
import jadex.common.transformation.traverser.BeanProperty;
import jadex.common.transformation.traverser.IBeanIntrospector;
import jadex.core.IComponent;
import jadex.core.IExternalAccess;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.javaparser.SJavaParser;
import jadex.model.IModelFeature;
import jadex.nfproperty.impl.AbstractNFProperty;
import jadex.nfproperty.impl.NFPropertyMetaInfo;
import jadex.providedservice.IService;

/**
 *  Tagging a service with a string for searching specifically
 *  tagged services.
 *  
 *  Allows tagging with single tags and tag collections.
 *  a) allows for tagging via creation parameters
 *  b) allows for tagging by referencing an argument (which is read to get the tags)
 */
public class TagProperty extends AbstractNFProperty<Collection<String>, Void>
{
	/** The name of the property. */
	public static final String NAME = "tag";
	
	/** The argument constant. */
	public static final String ARGUMENT = "argument";
	
	/** The key used to store the tags in the service property map. */
	public static final String SERVICE_PROPERTY_NAME = "__service_tags__";
	
	/** The host name tag. */
	private static final String HOST_INTERNAL = "$host";
	public static final String HOST = "\""+HOST_INTERNAL+"\"";
	
	/** The pid tag. */
	private static final String PID_INTERNAL = "$pid";
	public static final String PID = "\""+PID_INTERNAL+"\"";
	
	/** The app id tag. */
	private static final String APPID_INTERNAL = "$appid";
	public static final String APPID = "\""+APPID_INTERNAL+"\"";
	
	///** The Jadex version tag. */
	//private static final String JADEX_VERSION_INTERNAL = "jadex_version";
	//public static final String JADEX_VERSION = "\""+JADEX_VERSION_INTERNAL+"\"";
	
	/** The component. */
	protected IComponent component;
	
	/** The parameters. */
	protected Map<String, Object> params;
	
	/**
	 *  Creates the property.
	 */
	public TagProperty(IComponent comp, IService service, MethodInfo method, Map<String, Object> params)
	{
		super(new NFPropertyMetaInfo(NAME, String.class, Void.class, false));
		this.component = comp;
		this.params = params;
		
		System.out.println("tag prop created: "+comp.getId()+" "+service.getServiceId()+" "+method);
	}
	
	/**
	 *  Returns the current value of the property, performs unit conversion if necessary.
	 *  @param unit Unit of the returned value.
	 *  @return The current value of the property.
	 */
	public IFuture<Collection<String>> getValue(Void unit)
	{
		Future<Collection<String>> ret = new Future<Collection<String>>();
		IBeanIntrospector insp = BeanIntrospectorFactory.get().getBeanIntrospector();
		
		boolean found = false;
		if(params!=null)
		{
			Collection<String> tags = null;
			Collection<String> tags2 = null;
			
			// get values directyl from init parameters under TAG
			if(params.containsKey(NAME))
			{
				Object vals = params.get(NAME);
				tags = createRuntimeTags(vals, component.getExternalAccess());
				found = true;
			}
			
			// get values from component args under name specified in ARGUMENT
			if(params.containsKey(ARGUMENT) && component.getPojo()!=null)
			{
				Map<String, BeanProperty> props = insp.getBeanProperties(component.getPojo().getClass(), true, true);
				BeanProperty prop = props.get(ARGUMENT);
				if(prop!=null)
				{
					tags2 = convertToCollection(prop.getPropertyValue(component.getPojo()));
					
					if(tags==null)
					{
						tags = tags2;
					}
					else if(tags2!=null)
					{
						tags.addAll(tags2);
					}
					found = true;
				}
			}
			
			// get values directyl from init parameters under TAG_0, TAG_1 ... (from @Tags)
			if(params.containsKey(NAME+"_0"))
			{
				List<Object> vals = new ArrayList<>();
				for(int i=0; params.containsKey(NAME+"_"+i); i++)
				{
					Object v = params.get(NAME+"_"+i);
					String val = v instanceof String? (String)v: ""+v;
					String cond = (String)params.get(NAME+"_include_"+i);
					if(cond!=null && cond.length()>0)
					{
						try
						{
							IModelFeature mf = component.getFeature(IModelFeature.class);
							Object c = SJavaParser.evaluateExpression(cond, mf.getModel().getAllImports(), component.getValueProvider().getFetcher(), ComponentManager.get().getClassLoader());
							if(c instanceof Boolean && ((Boolean)c).booleanValue())
								vals.add(val);
						}
						catch(Exception e)
						{
							e.printStackTrace();
						}
					}
					else
					{
						vals.add(val);
					}
				}
				
				Collection<String> ts = createRuntimeTags(vals, component.getExternalAccess());
				if(tags==null)
					tags = ts;
				else
					tags.addAll(ts);
				found = true;
			}
			
			if(found)
				ret.setResult(tags);
		}
		
		// directly search argument "tag"
		if(!found)
		{
			Map<String, BeanProperty> props = insp.getBeanProperties(component.getPojo().getClass(), true, true);
			if(props.containsKey(NAME))
			{
				BeanProperty prop = props.get(NAME);
				Collection<String> tags = createRuntimeTags(prop.getPropertyValue(component.getPojo()), component.getExternalAccess());
				ret.setResult(tags);
				found = true;
			}
		}
		
		if(!found)
			ret.setException(new RuntimeException("Could not evaluate tag value, no hint given (value or argument name)"));
		
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
	
	/**
	 *  Create a collection of tags and replace the variable values.
	 */
	public static Collection<String> createRuntimeTags(Object vals, IExternalAccess component)
	{
		Collection<String> tags = convertToCollection(vals);
		Iterator<String> it = tags.iterator();
		List<String> ret = new ArrayList<String>();
		for(int i=0; i<tags.size(); i++)
		{
			String tag = it.next();
			if(HOST_INTERNAL.equals(tag) || HOST.equals(tag))
			{
				tag = component.getId().getGlobalProcessIdentifier().host();
			}
			else if(PID.equals(tag) || PID.equals(tag))
			{
				tag = ""+component.getId().getGlobalProcessIdentifier().pid();
			}
			else if(APPID.equals(tag) || APPID.equals(tag))
			{
				tag = component.getAppId();
			}
			ret.add(tag);
		}
		return ret;
	}
	
	/**
	 *  Check if it is a reserved tag.
	 *  @param tag The tag.
	 *  @return True if is reserved.
	 */
	public static void checkReservedTags(String[] tags)
	{
		for(String tag: tags)
		{
			checkReservedTag(tag);
		}
	}
	
	/**
	 *  Check if it is a reserved tag.
	 *  @param tag The tag.
	 *  @return True if is reserved.
	 */
	public static void checkReservedTag(String tag)
	{
		if(HOST_INTERNAL.equals(tag) || HOST.equals(tag)
			|| PID_INTERNAL.equals(tag) || PID.equals(tag) 
			|| APPID_INTERNAL.equals(tag) || APPID.equals(tag)
		)
		{
			throw new IllegalArgumentException("Tag name is reserved.");
		}
	}
}