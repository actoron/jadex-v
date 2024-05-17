package jadex.micro.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;

import jadex.common.IParameterGuesser;
import jadex.common.IValueFetcher;
import jadex.common.SAccess;
import jadex.common.SReflect;
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
		if("$agent".equals(name))
		{
			return self;
		}
		else if("$component".equals(name))
		{
			return self;
		}
		else if("$pojoagent".equals(name))
		{
			return self.getPojo();
		}
		else if("$cid".equals(name) || "$id".equals(name))
		{
			return self.getId();
		}
		else if("$host".equals(name))
		{
			return self.getId().getGlobalProcessIdentifier().host();
		}
		else if("$pid".equals(name))
		{
			return self.getId().getGlobalProcessIdentifier().pid();
		}
		else if("$appid".equals(name))
		{
			return self.getAppId();
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
			Object pojo = self.getPojo();
			
			Class<?> clazz = pojo.getClass();
			boolean found = false;
			Object value = null;
			
			String mname = "get"+name.substring(0,1).toUpperCase()+name.substring(1);
			Method[] getters = SReflect.getAllMethods(pojo.getClass(), mname);

			for(Method m: getters)
			{
				if(m.getParameterCount()==0)
				{
					try
					{
						value = m.invoke(pojo, new Object[0]);
						found = true;
					}
					catch(Exception e)
					{
					}
				}
			}

			if(!found)
			{
				mname = "is"+name.substring(0,1).toUpperCase()+name.substring(1);
				getters = SReflect.getAllMethods(pojo.getClass(), mname);
				
				for(Method m: getters)
				{
					if(m.getParameterCount()==0)
					{
						try
						{
							value = m.invoke(pojo, new Object[0]);
							found = true;
						}
						catch(Exception e)
						{
						}
					}
				}
			}
			
			if(!found)
			{
				Field f = SReflect.getField(clazz, name);
				if(f!=null)
				{
					try
					{
						SAccess.setAccessible(f, true);
						value = f.get(pojo);
						found = true;
					}
					catch(Exception e)
					{
					}
				}
			}
			
			if(found)
			{
				return value;
			}
			else
			{
				throw new RuntimeException("Value not found: "+name);
			}
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
