package jadex.mj.core.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import jadex.mj.core.MjComponent;

public class SComponentFactory
{
	protected static SMjFeatureProvider dummy = new SMjFeatureProvider();
	
	protected static List<IComponentCreator> finders;
	
	public static <T extends MjComponent> T	createComponent(Class<T> type, Supplier<T> creator)
	{
		List<MjFeatureProvider<Object>>	providers	= new ArrayList<>(SMjFeatureProvider.getProvidersForComponent(type).values());
		for(int i=providers.size()-1; i>=0; i--)
		{
			MjFeatureProvider<Object>	provider	= providers.get(i);
			if(provider instanceof IBootstrapping)
			{
				Supplier<T>	nextcreator	= creator;
				creator	= () -> ((IBootstrapping)provider).bootstrap(type, nextcreator);
			}
		}
		return creator.get();
	}

	public static void addComponentTypeFinder(IComponentCreator finder)
	{
		if(finders==null)
			finders = new ArrayList<IComponentCreator>();
		finders.add(finder);
	}
	
	public static Class<? extends MjComponent> findComponentType(Object pojo)
	{
		Class<? extends MjComponent> ret = null;
		for(IComponentCreator finder: finders)
		{
			if(finder.filter(finder))
			{
				ret = finder.getType();
				break;
			}
		}
		return ret;
	}
	
	public static void create(Runnable pojo)
	{
		for(IComponentCreator finder: finders)
		{
			if(finder.filter(pojo))
			{
				finder.create(pojo);
				break;
			}
		}
	}
	
	public static void create(Object pojo)
	{
		for(IComponentCreator finder: finders)
		{
			if(finder.filter(pojo))
			{
				finder.create(pojo);
				break;
			}
		}
	}
}


