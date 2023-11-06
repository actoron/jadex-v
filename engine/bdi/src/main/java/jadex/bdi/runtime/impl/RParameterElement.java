package jadex.bdi.runtime.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jadex.bdi.model.MConfigParameterElement;
import jadex.bdi.model.MParameter;
import jadex.bdi.model.MParameterElement;
import jadex.bdi.model.MParameter.EvaluationMode;
import jadex.bdi.runtime.ChangeEvent;
import jadex.bdi.runtime.IParameter;
import jadex.bdi.runtime.IParameterElement;
import jadex.bdi.runtime.IParameterSet;
import jadex.bdi.runtime.wrappers.EventPublisher;
import jadex.bdi.runtime.wrappers.ListWrapper;
import jadex.common.IValueFetcher;
import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.common.UnparsedExpression;
import jadex.execution.IExecutionFeature;
import jadex.javaparser.IMapAccess;
import jadex.javaparser.SJavaParser;
import jadex.javaparser.SimpleValueFetcher;
import jadex.micro.MicroAgent;
import jadex.model.IModelFeature;
import jadex.rules.eca.EventType;

/**
 *  Base element for elements with parameters such as:
 *  - message event
 *  - internal event
 *  - goal
 *  - plan
 */
public abstract class RParameterElement extends RElement implements IParameterElement, IMapAccess
{
	/** The parameters. */
	protected Map<String, IParameter> parameters;
	
	/** The parameter sets. */
	protected Map<String, IParameterSet> parametersets;
	
	/**
	 *  Create a new parameter element.
	 */
	public RParameterElement(MParameterElement melement, Map<String, Object> vals, MConfigParameterElement config)
	{
		super(melement);
		initParameters(vals, config);
	}
	
	/**
	 *  Create the parameters from model spec.
	 */
	public void initParameters(Map<String, Object> vals, MConfigParameterElement config)
	{
		List<MParameter> mparams = getModelElement()!=null ? ((MParameterElement)getModelElement()).getParameters() : null;
		if(mparams!=null)
		{
			for(MParameter mparam: mparams)
			{
				if(!mparam.isMulti(((MicroAgent)getAgent()).getClassLoader()))
				{
					if(vals!=null && (vals.containsKey(mparam.getName()) || vals.containsKey(SUtil.snakeToCamelCase(mparam.getName()))) && MParameter.EvaluationMode.STATIC.equals(mparam.getEvaluationMode()))
					{
						addParameter(createParameter(mparam, mparam.getName(), vals.containsKey(mparam.getName()) ? vals.get(mparam.getName()) : vals.get(SUtil.snakeToCamelCase(mparam.getName()))));
					}
					else
					{
						addParameter(createParameter(mparam, mparam.getName(), config!=null ? config.getParameter(mparam.getName()) : null));
					}
				}
				else
				{
					if(vals!=null && (vals.containsKey(mparam.getName()) || vals.containsKey(SUtil.snakeToCamelCase(mparam.getName()))) && MParameter.EvaluationMode.STATIC.equals(mparam.getEvaluationMode()))
					{
						addParameterSet(createParameterSet(mparam, mparam.getName(), vals.containsKey(mparam.getName()) ? vals.get(mparam.getName()) : vals.get(SUtil.snakeToCamelCase(mparam.getName()))));
					}
					else
					{
						addParameterSet(createParameterSet(mparam, mparam.getName(), config!=null ? config.getParameters(mparam.getName()) : null));
					}
				}
			}
		}
	}
	
	/**
	 *  Wrap the fetcher to include the element itself.
	 */
	public SimpleValueFetcher wrapFetcher(final IValueFetcher fetcher)
	{
		SimpleValueFetcher	ret	= new SimpleValueFetcher(fetcher);
		ret.setValue(getFetcherName(), this);
		return ret;
	}
	
	/**
	 *  Get the name of the element in the fetcher (e.g. $goal).
	 *  @return The element name in the fetcher name.
	 */
	public abstract String getFetcherName();
	
	/**
	 *  Test if parameter writes are currently allowed.
	 *  @throws Exception when write not ok.
	 */
	public void	testWriteOK(MParameter mparam)
	{
		// Nop. overriden for processable elements.
	}
	
	/**
	 * 
	 */
	public IParameter createParameter(MParameter modelelement, String name, UnparsedExpression inival)
	{
		return new RParameter(modelelement, name, inival, getModelElement().getName());
	}
	
	/**
	 * 
	 */
	public IParameter createParameter(MParameter modelelement, String name, Object value)
	{
		return new RParameter(modelelement, name, value, getModelElement().getName());
	}
	
	/**
	 * 
	 */
	public IParameterSet createParameterSet(MParameter modelelement, String name, List<UnparsedExpression> inivals)
	{
		return new RParameterSet(modelelement, name, inivals, getModelElement().getName());
	}
	
	/**
	 * 
	 */
	public IParameterSet createParameterSet(MParameter modelelement, String name, Object values)
	{
		return new RParameterSet(modelelement, name, values, getModelElement().getName());
	}
	
	/**
	 *  Add a parameter.
	 *  @param param The parameter.
	 */
	public void addParameter(IParameter param)
	{
		if(parameters==null)
			parameters = new HashMap<String, IParameter>();
		parameters.put(param.getName(), param);
	}
	
	/**
	 *  Add a parameterset.
	 *  @param paramset The parameterset.
	 */
	public void addParameterSet(IParameterSet paramset)
	{
		if(parametersets==null)
			parametersets = new HashMap<String, IParameterSet>();
		parametersets.put(paramset.getName(), paramset);
	}
	
	/**
	 *  Get all parameters.
	 *  @return All parameters.
	 */
	public IParameter[]	getParameters()
	{
		return parameters==null? new IParameter[0]: parameters.values().toArray(new IParameter[parameters.size()]);
	}

	/**
	 *  Get all parameter sets.
	 *  @return All parameter sets.
	 */
	public IParameterSet[]	getParameterSets()
	{
		return parametersets==null? new IParameterSet[0]: parametersets.values().toArray(new IParameterSet[parametersets.size()]);
	}

	/**
	 *  Get the parameter element.
	 *  @param name The name.
	 *  @return The param.
	 */
	public IParameter getParameter(String name)
	{
		if(parameters==null || !parameters.containsKey(name))
			throw new RuntimeException("Parameter not found: "+name);
		return parameters.get(name);
	}

	/**
	 *  Get the parameter set element.
 	 *  @param name The name.
	 *  @return The param set.
	 */
	public IParameterSet getParameterSet(String name)
	{
		if(parametersets==null || !parametersets.containsKey(name))
			throw new RuntimeException("Parameterset not found: "+name);
		return parametersets.get(name);
	}

	/**
	 *  Has the element a parameter element.
	 *  @param name The name.
	 *  @return True, if it has the parameter.
	 */
	public boolean hasParameter(String name)
	{
		return parameters==null? false: parameters.containsKey(name);
	}

	/**
	 *  Has the element a parameter set element.
	 *  @param name The name.
	 *  @return True, if it has the parameter set.
	 */
	public boolean hasParameterSet(String name)
	{
		return parametersets==null? false: parametersets.containsKey(name);
	}

	/**
	 *  Get an object from the map.
	 *  @param key The key
	 *  @return The value.
	 */
	public Object get(Object key)
	{
		String name = (String)key;
		Object ret = null;
		if(hasParameter(name))
		{
			ret = getParameter(name).getValue();
		}
		else if(hasParameterSet(name))
		{
			ret = getParameterSet(name).getValues();
		}
		else
		{
			throw new RuntimeException("Unknown parameter/set: "+name);
		}
		return ret;
	}
	

	/**
	 *  Check if the element is currently part of the agent's reasoning.
	 *  E.g. the bases are always adopted and all of their contents such as goals, plans and beliefs.
	 */
	public abstract boolean	isAdopted();
	
	/**
	 * 
	 */
	public class RParameter extends RElement implements IParameter
	{
		/** The name. */
		protected String name;
		
		/** The value. */
		protected Object value;

		/** The initial value expression (only for push evaluation mode). */
		protected UnparsedExpression inival;
		
		/** The publisher. */
		protected EventPublisher publisher;
		
		/**
		 *  Create a new parameter.
		 *  @param modelelement The model element.
		 *  @param name The name.
		 */
		public RParameter(MParameter modelelement, String name, String pename)
		{
			super(modelelement);
			this.name = name!=null? name: modelelement.getName();
			this.publisher = new EventPublisher(getAgent(), new EventType(ChangeEvent.VALUECHANGED, pename, getName()), (MParameter)getModelElement());
		}
		
		/**
		 *  Create a new parameter.
		 *  @param modelelement The model element.
		 *  @param name The name.
		 */
		public RParameter(MParameter modelelement, String name, UnparsedExpression inival, String pename)
		{
			super(modelelement);
			this.name = name!=null? name: modelelement.getName();
			this.publisher = new EventPublisher(getAgent(), new EventType(ChangeEvent.VALUECHANGED, pename, getName()), (MParameter)getModelElement());
			
			if(modelelement!=null && modelelement.getEvaluationMode()==EvaluationMode.PULL)
			{
				this.inival	= inival;
			}
			
			setValue(evaluateValue(inival));
		}
		
		/**
		 *  Create a new parameter.
		 *  @param modelelement The model element.
		 *  @param name The name.
		 */
		public RParameter(MParameter modelelement, String name, Object value, String pename)
		{
			super(modelelement);
			this.name = name!=null? name: modelelement.getName();
			// RParameterElement.this.getName()
			this.publisher = new EventPublisher(getAgent(), new EventType(ChangeEvent.VALUECHANGED, pename, getName()), (MParameter)getModelElement());
			
			setValue(value);
		}

		/**
		 *  Get the name.
		 *  @return The name
		 */
		public String getName()
		{
			return name;
		}
		
		/**
		 *  Set a value of a parameter.
		 *  @param value The new value.
		 */
		public void setValue(Object value)
		{
			testWriteOK((MParameter)getModelElement());
			internalSetValue(value);
		}
		
		/**
		 *  Set the value without check.
		 */
		protected void internalSetValue(Object value)
		{
			if(value!=null && getModelElement()!=null)
			{
				Class<?>	clazz	= ((MParameter)getModelElement()).getClazz().getType(((MicroAgent)getAgent()).getClassLoader(), getAgent().getFeature(IModelFeature.class).getModel().getAllImports());
				if(!SReflect.isSupertype(clazz, value.getClass()))
				{
					throw new IllegalArgumentException("Incompatible value for parameter "+getName()+": "+value);
				}
				value	= SReflect.convertWrappedValue(value, clazz);
			}
			
			Object oldvalue = this.value;
			this.value = value;
			publisher.entryChanged(oldvalue, value, -1);
		}
		
		/**
		 *  Update the dynamic value for push or update rate implementation.
		 */
		public void	updateDynamicValue()
		{
			internalSetValue(evaluateValue(inival));
		}

		/**
		 *  Get the value of a parameter.
		 *  @return The value.
		 */
		public Object	getValue()
		{
			return ((MParameter)getModelElement()).getEvaluationMode()==EvaluationMode.PULL ? evaluateValue(inival) : value;
		}
		
		/**
		 *  Evaluate the (initial or default or pull) value.
		 */
		protected Object evaluateValue(UnparsedExpression inival)
		{
			UnparsedExpression uexp = inival!=null ? inival : getModelElement()!=null ? ((MParameter)getModelElement()).getDefaultValue() : null;
			return uexp!=null ? SJavaParser.parseExpression(uexp, getAgent().getFeature(IModelFeature.class).getModel().getAllImports(), ((MicroAgent)getAgent()).getClassLoader()).getValue(
				wrapFetcher(IExecutionFeature.get().getComponent().getFeature(IModelFeature.class).getFetcher())) : null;
		}
		
		/**
		 *  Test if this parameter has a default value.
		 */
		protected boolean hasDefaultValue()
		{
			return getModelElement()!=null && ((MParameter)getModelElement()).getDefaultValue()!=null;
		}
	}

	/**
	 * 
	 */
	public class RParameterSet extends RElement implements IParameterSet
	{
		/** The name. */
		protected String name;
		
		/** The value. */
		protected List<Object> values;
		
		/** The initial values expression(s) (only for push evaluation mode). */
		protected List<UnparsedExpression> inivals;
		
		/**
		 *  Create a new parameter.
		 *  @param modelelement The model element.
		 *  @param name The name.
		 */
		public RParameterSet(MParameter modelelement, String name, String pename)
		{
			super(modelelement);
			this.name = name!=null?name: modelelement.getName();
		}
		
		/**
		 *  Create a new parameter.
		 *  @param modelelement The model element.
		 *  @param name The name.
		 */
		public RParameterSet(MParameter modelelement, String name, Object vals, String pename)
		{
			super(modelelement);
			
			this.name = name!=null?name: modelelement.getName();
			
			List<Object> inivals = new ArrayList<Object>();
			if(vals!=null)
			{
				Iterator<?>	it	= SReflect.getIterator(vals);
				while(it.hasNext())
				{
					inivals.add(it.next());
				}
			}
			
			setValues(new ListWrapper<Object>(vals!=null? inivals: new ArrayList<Object>(), getAgent(),
				new EventType(ChangeEvent.VALUEADDED, pename, getName()), 
				new EventType(ChangeEvent.VALUEREMOVED, pename, getName()), 
				new EventType(ChangeEvent.VALUECHANGED, pename, getName()), getModelElement()));
		}
		
		/**
		 *  Create a new parameter.
		 *  @param modelelement The model element.
		 *  @param name The name.
		 */
		public RParameterSet(MParameter modelelement, String name,List<UnparsedExpression> inivals, String pename)
		{
			super(modelelement);
			this.name = name!=null?name: modelelement.getName();
			if(modelelement!=null && modelelement.getEvaluationMode()==EvaluationMode.PULL)
			{
				this.inivals	= inivals;
			}
			
			setValues(new ListWrapper<Object>(evaluateValues(inivals), getAgent(), 
				new EventType(ChangeEvent.VALUEADDED, pename, getName()), 
				new EventType(ChangeEvent.VALUEREMOVED, pename, getName()), 
				new EventType(ChangeEvent.VALUECHANGED, pename, getName()), getModelElement()));
		}

		/**
		 *  Evaluate the default values.
		 */
		protected List<Object> evaluateValues(List<UnparsedExpression> inivals)
		{
			MParameter mparam = (MParameter)getModelElement();
			List<Object> tmpvalues = new ArrayList<Object>();
			if(inivals==null && mparam!=null)
			{
				if(mparam.getDefaultValue()!=null)
				{
					inivals	= Collections.singletonList(mparam.getDefaultValue());
				}
				else
				{
					inivals	= mparam.getDefaultValues();
				}
			}
			
			if(inivals!=null)
			{
				if(inivals.size()==1)
				{
					Object	tmpvalue	= SJavaParser.parseExpression(inivals.get(0), getAgent().getFeature(IModelFeature.class).getModel().getAllImports(), ((MicroAgent)getAgent()).getClassLoader()).getValue(
						wrapFetcher(IExecutionFeature.get().getComponent().getFeature(IModelFeature.class).getFetcher()));
					if(tmpvalue!=null && getClazz()!=null && SReflect.isSupertype(getClazz(), tmpvalue.getClass()))
					{
						tmpvalues.add(tmpvalue);
					}
					else
					{
						for(Object tmp: SReflect.getIterable(tmpvalue))
						{
							tmpvalues.add(tmp);
						}
					}
				}
				else 
				{
					for(UnparsedExpression uexp: inivals)
					{
						tmpvalues.add(SJavaParser.parseExpression(uexp, ((MicroAgent)getAgent()).getModel().getAllImports(), ((MicroAgent)getAgent()).getClassLoader()).getValue(
							wrapFetcher(IExecutionFeature.get().getComponent().getFeature(IModelFeature.class).getFetcher())));
					}
				}
			}
			
			return tmpvalues;
		}
		
		/**
		 *  Get the class of a value.
		 */
		protected Class<?> getClazz()
		{
			MParameter mparam = (MParameter)getModelElement();
			return mparam.getClazz().getType(((MicroAgent)getAgent()).getClassLoader(), ((MicroAgent)getAgent()).getModel().getAllImports());
		}
		
		/**
		 *  Get the name.
		 *  @return The name
		 */
		public String getName()
		{
			return name;
		}
		
		/**
		 *  Add a value to a parameter set.
		 *  @param value The new value.
		 */
		public void addValue(Object value)
		{
			testWriteOK((MParameter)getModelElement());
			
			if(value!=null && getModelElement()!=null)
			{
				Class<?>	clazz	= ((MParameter)getModelElement()).getClazz().getType(((MicroAgent)getAgent()).getClassLoader(), getAgent().getFeature(IModelFeature.class).getModel().getAllImports());
				if(!SReflect.isSupertype(clazz, value.getClass()))
				{
					throw new IllegalArgumentException("Incompatible value for parameter set "+getName()+": "+value);
				}
				value	= SReflect.convertWrappedValue(value, clazz);
			}
			
			internalAddValue(value);
		}
		
		/**
		 *  Add a value.
		 */
		protected void internalAddValue(Object value)
		{
			internalGetValues().add(value);			
		}

		/**
		 *  Remove a value to a parameter set.
		 *  @param value The new value.
		 */
		public void removeValue(Object value)
		{
			testWriteOK((MParameter)getModelElement());
			
			internalRemoveValue(value);
		}
		
		/**
		 *  Remove a value.
		 */
		protected void internalRemoveValue(Object value)
		{
			internalGetValues().remove(value);			
		}

		/**
		 *  Add values to a parameter set.
		 */
		public void addValues(Object[] values)
		{
			if(values!=null)
			{
				for(Object value: values)
				{
					addValue(value);
				}
			}
		}

		/**
		 *  Remove all values from a parameter set.
		 */
		public void removeValues()
		{
			testWriteOK((MParameter)getModelElement());
			
			internalRemoveValues();
		}
		
		/**
		 *  Remove all values.
		 */
		protected void internalRemoveValues()
		{
			internalGetValues().clear();
		}

		/**
		 *  Get a value equal to the given object.
		 *  @param oldval The old value.
		 */
//		public Object	getValue(Object oldval);

		/**
		 *  Test if a value is contained in a parameter.
		 *  @param value The value to test.
		 *  @return True, if value is contained.
		 */
		public boolean containsValue(Object value)
		{
			return internalGetValues().contains(value);
		}

		/**
		 *  Get the values of a parameterset.
		 *  @return The values.
		 */
		public Object[]	getValues()
		{
			return getValues(((MParameter)getModelElement()).getType(((MicroAgent)getAgent()).getClassLoader()));
		}
		
		/**
		 *  Update the dynamic values for push or update rate implementation.
		 */
		public void	updateDynamicValues()
		{
			internalSetValues(evaluateValues(inivals));
		}

		/**
		 *  Get the values of a parameterset.
		 *  @return The values.
		 */
		protected Object[]	getValues(Class<?> type)
		{
			Object ret;
			List<Object> vals = internalGetValues();
			
			int size = vals==null? 0: vals.size();
			ret = type!=null? ret = Array.newInstance(SReflect.getWrappedType(type), size): new Object[size];
			
			if(vals!=null)
				System.arraycopy(vals.toArray(new Object[vals.size()]), 0, ret, 0, vals.size());
			
			return (Object[])ret;
		}

		/**
		 *  Get the number of values currently
		 *  contained in this set.
		 *  @return The values count.
		 */
		public int size()
		{
			return internalGetValues().size();
		}

		/**
		 *  The values to set.
		 *  @param values The values to set
		 */
		// Internal method, overridden for message event.
		protected void setValues(List<Object> values)
		{
			testWriteOK((MParameter)getModelElement());
			
			internalSetValues(values);
		}
		
		/**
		 *  The values to set.
		 *  @param values The values to set
		 */
		protected void internalSetValues(List<Object> values)
		{
			this.values = values;
		}
		
		/**
		 * 
		 */
		protected List<Object> internalGetValues()
		{
			// In case of push the last saved/evaluated value is returned
			return MParameter.EvaluationMode.PULL.equals(((MParameter)getModelElement()).getEvaluationMode())? evaluateValues(inivals): values;
		}
	}
	
	/**
	 *  Get the element type (i.e. the name declared in the ADF).
	 *  @return The element type.
	 */
	public String getType()
	{
		return getModelElement().getElementName();
	}

}
