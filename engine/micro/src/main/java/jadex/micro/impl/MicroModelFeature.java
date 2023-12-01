package jadex.micro.impl;

import java.util.Collections;

import jadex.common.IParameterGuesser;
import jadex.common.IValueFetcher;
import jadex.common.SimpleParameterGuesser;
import jadex.micro.MicroAgent;
import jadex.model.IModelFeature;
import jadex.model.IParameterGuesserProvider;
import jadex.model.impl.IInternalModelFeature;
import jadex.model.modelinfo.IModelInfo;

public class MicroModelFeature implements IModelFeature, IInternalModelFeature, IParameterGuesserProvider, IValueFetcher
{
	protected MicroAgent self;
	protected IModelInfo model;
	protected IParameterGuesser guesser;
	
	public MicroModelFeature(MicroAgent self)
	{
		this.self	= self;
	}

	@Override
	public IParameterGuesser getParameterGuesser()
	{
		if(guesser==null)
			guesser	= new SimpleParameterGuesser(new SimpleParameterGuesser(Collections.singleton(self)), Collections.singleton(self.getPojo()));
			//guesser	= new SimpleParameterGuesser(super.getParameterGuesser(), Collections.singleton(pojoagent));
		return guesser;
	}
	
	/**
	 *  Add $pojoagent to fetcher.
	 */
	public Object fetchValue(String name)
	{
		if("$pojoagent".equals(name))
		{
			return self.getPojo();
		}
		/*else if(name!=null && name.startsWith(IPlatformConfiguration.PLATFORMARGS))
		{
			String valname = name.length()>13? name.substring(14): null;
			return valname==null? Starter.getPlatformValue(getComponent().getId(), IPlatformConfiguration.PLATFORMARGS): Starter.getPlatformValue(getComponent().getId(), valname);
		}*/
		/*else if(Starter.hasPlatformValue(getComponent().getId(), name))
		{
			return Starter.getPlatformValue(getComponent().getId(), name);
		}*/
		else
		{
			throw new RuntimeException("Value not found: "+name);
		}
	}

	@Override
	public IModelInfo getModel()
	{
		return model;
	}
	
	@Override
	public void setModel(IModelInfo model)
	{
		this.model = model;
	}

	@Override
	public IValueFetcher getFetcher()
	{
		return this;
	}

}
