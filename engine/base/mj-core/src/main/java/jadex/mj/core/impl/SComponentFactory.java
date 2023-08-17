package jadex.mj.core.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import jadex.mj.core.MjComponent;

public class SComponentFactory
{
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
}
