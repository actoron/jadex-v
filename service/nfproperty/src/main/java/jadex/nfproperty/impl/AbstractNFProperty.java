package jadex.nfproperty.impl;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jadex.common.MethodInfo;
import jadex.common.UnparsedExpression;
import jadex.core.IComponent;
import jadex.core.impl.Component;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.javaparser.SJavaParser;
import jadex.nfproperty.INFProperty;
import jadex.nfproperty.annotation.NFProperties;
import jadex.nfproperty.annotation.NFProperty;
import jadex.providedservice.IService;

/**
 *  A non-functional property.
 *  
 *  NOTE: Implementing classes must implement a constructor with
 *  the signature INFProperty(String name) to allow the service
 *  to initialize the property during creation.
 */
public abstract class AbstractNFProperty<T, U> implements INFProperty<T, U>
{
	/** Name of the property. */
	protected NFPropertyMetaInfo metainfo;
	
	/**
	 *  Creates the property.
	 *  @param name Name of the property.
	 */
	public AbstractNFProperty(NFPropertyMetaInfo metainfo)
	{
		this.metainfo = metainfo;
	}
	
	/**
	 *  Gets the name of the property.
	 *  @return The name of the property.
	 */
	public String getName()
	{
		return metainfo.getName();
	}
	
	/**
	 *  Returns the current value of the property.
	 *  @return The current value of the property.
	 */
	public IFuture<T> getValue()
	{
		return getValue(null);
	}

	/**
	 *  Get the metainfo.
	 *  @return The metainfo.
	 */
	public NFPropertyMetaInfo getMetaInfo()
	{
		return metainfo;
	}
	
	/**
	 *  Returns the current value of the property in a human readable form.
	 *  @return The current value of the property.
	 */
	public IFuture<String> getPrettyPrintValue()
	{
		Future<String> ret = new Future<>();
		getValue().then(v ->
		{
			ret.setResult(""+v);
		}).catchEx(ret);
		return ret;
	}
	
	/**
	 *  Create nf properties form a class with nf annotations.
	 */
	public static List<INFProperty<?, ?>> readNFProperties(Class<?> type, Component comp, IService ser, MethodInfo mi)
	{
		List<INFProperty<?, ?>> ret = null;
		
		if(type.isAnnotationPresent(NFProperties.class))
		{
			List<NFProperty> nfprops = new ArrayList<NFProperty>();
			NFProperties typenfprops = type.getAnnotation(NFProperties.class);
			if(typenfprops != null)
				nfprops.addAll((Collection<? extends NFProperty>)Arrays.asList(typenfprops.value()));
			
			for(NFProperty nfprop : nfprops)
			{
				Class<?> clazz = nfprop.value();
				INFProperty<?, ?> prop = createProperty(clazz, comp, ser, mi, SNameValue.createUnparsedExpressionsList(nfprop.parameters()));
				
				if(ret==null)
					ret = new ArrayList<INFProperty<?,?>>();
				ret.add(prop);
			}
		}
		
		return ret;
	}
	
	/**
	 *  Create a property instance from its type.
	 */
	public static INFProperty<?, ?> createProperty(Class<?> clazz, IComponent comp, IService service, MethodInfo mi, List<UnparsedExpression> params)
	{
		INFProperty<?, ?> prop = null;
		try
		{
			Constructor<?> con = clazz.getConstructor();
			prop = (INFProperty<?, ?>)con.newInstance();
		}
		catch(NoSuchMethodException e)
		{
			try
			{
				Constructor<?> con = clazz.getConstructor(new Class[]{IComponent.class});
				prop = (INFProperty<?, ?>)con.newInstance(comp);
			}
			catch(NoSuchMethodException ex)
			{
				try
				{
					Constructor<?> con = clazz.getConstructor(new Class[]{IComponent.class, IService.class, MethodInfo.class});
					prop = (INFProperty<?, ?>)con.newInstance(comp, service, mi);
				}
				catch(NoSuchMethodException ex2)
				{
					try
					{
						Constructor<?> con = clazz.getConstructor(new Class[]{IComponent.class, IService.class, MethodInfo.class, Map.class});
						
						Map<String, Object> ps = null;
						if(params!=null && params.size()>0)
						{
							ps = new HashMap<String, Object>();
							for(UnparsedExpression entry: params)
							{
								try
								{
									// Set imports to pojo package (TODO: allow more imports?) 
									String[]	imports = comp.getPojo()!=null ? new String[]{comp.getPojo().getClass().getPackage()+".*"} : null;
									Object val = SJavaParser.evaluateExpression(entry.getValue(), imports, comp.getValueProvider().getFetcher(), comp.getPojo().getClass().getClassLoader());
									ps.put(entry.getName(), val);
								}
								catch(Exception ex3)
								{
									ps.put(entry.getName(), entry.getValue());
								}
							}
						}
						
						prop = (INFProperty<?, ?>)con.newInstance(comp, service, mi, ps);
					}
					catch(NoSuchMethodException ex3)
					{
						throw new RuntimeException("No suitable constructor: "+clazz, ex3);
					}
					catch(Exception eee)
					{
						throw new RuntimeException("Property creation exception: "+clazz, eee);
					}
				}
				catch(Exception eee)
				{
					throw new RuntimeException("Property creation exception: "+clazz, eee);
				}
			}
			catch(Exception ee)
			{
				throw new RuntimeException("Property creation exception: "+clazz, ee);
			}
		}
		catch(Exception e)
		{
			throw new RuntimeException("Property creation exception: "+clazz, e);
		}
		
//		if(prop==null)
//			System.out.println("Property cannot be created: "+clazz);
//			throw new RuntimeException("Property cannot be created: "+clazz);
		
		return prop;
	}
	
	/**
	 *  Property was removed and should be disposed.
	 */
	public IFuture<Void> dispose()
	{
		return IFuture.DONE;
	}
}
