package jadex.mj.micro.impl;

import java.util.Collections;

import jadex.common.IParameterGuesser;
import jadex.common.IValueFetcher;
import jadex.common.SimpleParameterGuesser;
import jadex.mj.micro.MjMicroAgent;
import jadex.mj.model.IMjModelFeature;
import jadex.mj.model.IParameterGuesserProvider;
import jadex.mj.model.modelinfo.IModelInfo;

public class MjMicroModelFeature implements IMjModelFeature, IParameterGuesserProvider, IValueFetcher
{
	protected MjMicroAgent	self;
	protected IParameterGuesser	guesser;
	
	public MjMicroModelFeature(MjMicroAgent self)
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
		return self.getModel();
	}

	@Override
	public IValueFetcher getFetcher()
	{
		return this;
	}

}
