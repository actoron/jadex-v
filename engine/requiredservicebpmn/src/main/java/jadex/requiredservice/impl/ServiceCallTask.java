package jadex.requiredservice.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import jadex.bpmn.model.IModelContainer;
import jadex.bpmn.model.MActivity;
import jadex.bpmn.model.MParameter;
import jadex.bpmn.model.task.ITask;
import jadex.bpmn.model.task.ITaskContext;
import jadex.bpmn.model.task.annotation.Task;
import jadex.bpmn.model.task.annotation.TaskProperty;
import jadex.bpmn.model.task.annotation.TaskPropertyGui;
import jadex.bpmn.runtime.impl.ProcessThread;
import jadex.collection.IndexMap;
import jadex.common.SReflect;
import jadex.core.IComponent;
import jadex.execution.StepAborted;
import jadex.future.DelegationResultListener;
import jadex.future.ExceptionDelegationResultListener;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.IIntermediateFuture;
import jadex.future.IntermediateDefaultResultListener;
import jadex.model.IModelFeature;
import jadex.model.modelinfo.IModelInfo;
import jadex.requiredservice.IRequiredServiceFeature;
import jadex.requiredservice.impl.ServiceCallTask.ServiceCallTaskGui;

/**
 *  Call a service.
 *  Service and method name may be specified as parameters.
 *  Rebind parameter is also supported.
 *  All other in and inout parameters are interpreted as method arguments.
 *  One out or inout parameter may be specifed to receive the call result.
 *  Service name may alternatively supplied as name of lane and
 *  method name as name of activity. 
 */
@Task(description="The print task can be used for calling a component service.", properties={
	@TaskProperty(name="service", clazz=String.class, description="The required service name."),
	@TaskProperty(name="method", clazz=String.class, description="The required method name."),
	@TaskProperty(name="ranking", clazz=String.class, description="The ranking class.")},
	gui=@TaskPropertyGui(ServiceCallTaskGui.class)
//	@TaskProperty(name="rebind", clazz=boolean.class, description="The rebind flag (forces a frsh search).")
)
public class ServiceCallTask implements ITask
{
	//-------- constants --------
	
	/** Property for service name. */
	public static final String PROPERTY_SERVICE	= "service"; 
	
	/** Property for method name. */
	public static final String PROPERTY_METHOD	= "method"; 
	
	/** Property for ranking class name. */
	public static final String PROPERTY_RANKING	= "ranking"; 
	
	//-------- ITask interface --------
	
	/**
	 *  Execute the task.
	 *  @param context	The accessible values.
	 *  @param process	The process instance executing the task.
	 *  @return	To be notified, when the task has completed.
	 */
	public IFuture<Void> execute(final ITaskContext context, final IComponent process)
	{
		final Future<Void>	ret	= new Future<Void>();
		String	service	= (String)context.getPropertyValue(PROPERTY_SERVICE);
		String	method	= (String)context.getPropertyValue(PROPERTY_METHOD);
		String	rank	= (String)context.getPropertyValue(PROPERTY_RANKING);
		String	resultparam	= null;
		
		String[] imports = process.getFeature(IModelFeature.class).getModel().getAllImports();
		ClassLoader cl = process.getClass().getClassLoader();
		
		// Collect arguments and settings.
		final List<Object>	args = new ArrayList<Object>();
		final List<Class<?>> argtypes = new ArrayList<Class<?>>();
		IndexMap<String, MParameter>	mparams	= context.getActivity().getParameters();
		if(mparams!=null)
		{
			for(Iterator<MParameter> it=mparams.values().iterator(); it.hasNext(); )
			{
				MParameter	param	= (MParameter)it.next();
				if(PROPERTY_SERVICE.equals(param.getName()))
				{
					service	= (String)context.getParameterValue(param.getName());
				}
				else if(PROPERTY_METHOD.equals(param.getName()))
				{
					method	= (String)context.getParameterValue(param.getName());		
				}
				else if(MParameter.DIRECTION_IN.equals(param.getDirection()))
				{
					args.add(context.getParameterValue(param.getName()));
					argtypes.add(param.getClazz().getType(cl, imports));
				}
				else if(MParameter.DIRECTION_INOUT.equals(param.getDirection()))
				{
					if(resultparam!=null)
						throw new RuntimeException("Only one 'out' parameter allowed for ServiceCallTask: "+context);
					
					resultparam	= param.getName();
					args.add(context.getParameterValue(param.getName()));
					argtypes.add(param.getClazz().getType(cl, imports));
				}
				else if(MParameter.DIRECTION_OUT.equals(param.getDirection()))
				{
					if(resultparam!=null)
						throw new RuntimeException("Only one 'out' parameter allowed for ServiceCallTask: "+context);
					
					resultparam	= param.getName();
				}
			}
		}
		
		// Apply shortcuts, if necessary.
		if(service==null && context.getActivity().getLane()!=null)
		{
			service	= context.getActivity().getLane().getName();
		}
		if(method==null)
		{
			method	= context.getActivity().getName();
		}
		
		if(service==null)
		{
			ret.setException(new RuntimeException("No 'service' specified for ServiceCallTask: "+context));
			return ret;
		}
		if(method==null)
		{
			ret.setException(new RuntimeException("No 'method' specified for ServiceCallTask: "+context));
			return ret;
		}
		
		// Fetch service and call method.
		final String	fservice	= service;
		final String	fmethod	= method;
		final String	fresultparam	= resultparam;
		
		Class<?> servicetype = process.getFeature(IRequiredServiceFeature.class).getServiceInfo(fservice).getType().getType(process.getClass().getClassLoader(), process.getFeature(IModelFeature.class).getModel().getAllImports());
		Method[] methods = servicetype.getMethods();
		Method met = null;
		for(Method meth : methods)
		{
			// for old models keep both checks
			if(meth.toString().equals(fmethod) || SReflect.getMethodSignature(meth).equals(fmethod))
			{
				met = meth;
				break;
			}
		}
		if(met==null)
		{
			ret.setException(new RuntimeException("SCT: "+ String.valueOf(process.getFeature(IModelFeature.class).getModel().getFilename()) + " Method "+fmethod+" not found for service "+fservice+": "+context));
			return ret;
		}
		final Method m = met;
		
		/*if(rank!=null) //|| multiple)
		{
			IServiceEvaluator eval = null;
			try
			{
				Class<?> evacl = SReflect.findClass(rank, imports, cl);
				try
				{
					Constructor con = evacl.getConstructor(new Class[]{IExternalAccess.class, MethodInfo.class});
					eval = (IServiceEvaluator)con.newInstance(new Object[]{process.getExternalAccess(), new MethodInfo(m)});
				}
				catch(Exception e)
				{
					try
					{
						Constructor con = evacl.getConstructor(new Class[]{IExternalAccess.class});
						eval = (IServiceEvaluator)con.newInstance(new Object[]{process.getExternalAccess()});
					}
					catch(Exception ex)
					{
						ret.setException(ex);
						return ret;
					}
				}
				
				ComposedEvaluator<Object> ranker = new ComposedEvaluator<Object>();
				ranker.addEvaluator(eval);
				
				process.getFeature(IRequiredServicesFeature.class).getServices(service)
					.addResultListener(new ServiceRankingResultListener<Object>(new ExceptionDelegationResultListener<Collection<Tuple2<Object, Double>>, Void>(ret)
				{
					public void customResultAvailable(Collection<Tuple2<Object, Double>> results)
					{
//						System.out.println("services: "+results);
						if(results.isEmpty())
						{
							ret.setException(new ServiceNotFoundException(fservice));
						}
						else
						{
							invokeService(process, fmethod, fservice, fresultparam, args, context, results.iterator().next().getFirstEntity(), m)
								.addResultListener(new DelegationResultListener<Void>(ret));
						}
					}
				}, ranker, null));
					
			}
			catch(Exception e)
			{
				ret.setException(e);
			}
		}
		else*/
		{
			process.getFeature(IRequiredServiceFeature.class).getService(service)
				.addResultListener(new ExceptionDelegationResultListener<Object, Void>(ret)
			{
				public void customResultAvailable(Object result)
				{
					invokeService(process, fmethod, fservice, fresultparam, args, context, result, m).addResultListener(new DelegationResultListener<Void>(ret));
				}
			});
		}
		
		return ret;
	}
	
	/**
	 *  Invoke the service.
	 */
	protected IFuture<Void> invokeService(final IComponent process, String fmethod, String fservice, final String fresultparam, 
		List<Object> args, final ITaskContext context, Object service, Method m)
	{
		final Future<Void> ret = new Future<Void>();
		
		try
		{
			Object	val	= m.invoke(service, args.toArray());
			if(val instanceof IIntermediateFuture)
			{
				MActivity mact = context.getActivity();
				List<MActivity> handlers = mact.getEventHandlers();
				MActivity handler = null;
				if(handlers!=null)
				{
					for(MActivity h: handlers)
					{
						if(h.isMessageEvent())
						{
							handler = h;
							break;
						}
					}
				}
				
				if(handler!=null)
				{
					final boolean isseq = handler.hasProperty(MActivity.ISSEQUENTIAL);
					final List<ProcessThread> queue = isseq? new ArrayList<ProcessThread>(): null;
					final MActivity fhandler = handler;
					((IIntermediateFuture<Object>)val).addResultListener(new IntermediateDefaultResultListener<Object>()
					{
						protected List<Object> results;
						boolean finished = false;
						int opencalls = 0; 
						
						public void intermediateResultAvailable(Object result)
						{
							opencalls++;
							
							if(fhandler!=null)
							{
								// Hack! Need to start threads from 'user' task. Should be done in a handler.
//								BpmnInterpreter ip = (BpmnInterpreter)process;
//								IInternalBpmnComponentFeature bf = (IInternalBpmnComponentFeature)process.getComponentFeature(IBpmnComponentFeature.class);
								ProcessThread th = (ProcessThread)context;
								ProcessThread pat = th.getParent();
								ProcessThread thread = new ProcessThread(fhandler, pat, process)
								{
									public void notifyFinished() 
									{
										opencalls--;
										
										if(isseq)
										{
											queue.remove(this);
											ProcessThread next = queue.size()>0? queue.get(0): null;
											if(next!=null)
											{
												next.setWaiting(false);
											}
											else if(opencalls==0 && finished)
											{
												if(fresultparam!=null)
													context.setParameterValue(fresultparam, results);
												ret.setResult(null);
											}
										}
									}
								};
								
								thread.setParameterValue(MActivity.RETURNPARAM, result);
								pat.addThread(thread);
								
//								System.out.println("queue: "+queue);
								if(isseq)
								{
									// Set waiting if not first thread
									if(queue.size()>0)
									{	
										thread.setWaiting(true);
									}
									queue.add(thread);
								}
							}
							else
							{
								// no handler added, ie. collect values and wait for finished
								if(results==null)
									results = new ArrayList<Object>();
								results.add(result);
							}
						}
						
						public void finished()
						{
							finished = true;
							if(opencalls==0)
							{
								if(fresultparam!=null)
									context.setParameterValue(fresultparam, results);
								ret.setResult(null);
							}
						}
						
						public void exceptionOccurred(Exception exception)
						{
							ret.setException(exception);
						}
					});
				}
			}
			else if(val instanceof IFuture)
			{
				((IFuture<Object>)val).addResultListener(new ExceptionDelegationResultListener<Object, Void>(ret)
				{
					public void customResultAvailable(Object result)
					{
//						System.out.println("result is: "+result);
						if(fresultparam!=null)
							context.setParameterValue(fresultparam, result);
						ret.setResult(null);
					}
				});
			}
			else
			{
				if(fresultparam!=null)
					context.setParameterValue(fresultparam, val);
				ret.setResult(null);
			}
		}
		catch(InvocationTargetException ite)
		{
			// Re-throw exception when current step is aborted 
			if(ite.getTargetException() instanceof StepAborted)
			{
				throw (Error)ite.getTargetException();
			}
			else
			{
				Exception re = ite.getTargetException() instanceof Exception
					? (Exception)ite.getTargetException()
					: new RuntimeException(ite.getTargetException());
				ret.setException(re);
			}
		}
		catch(IllegalArgumentException e)
		{
			Class<?>[] types = new Class<?>[args.size()];
			for (int i = 0; i < args.size(); ++i)
			{
				types[i] = args.get(i) != null? args.get(i).getClass() : null;
			}
			System.err.println("Argument mismatch: " + fmethod + "\n input=" + Arrays.toString(types));
			ret.setException(e);
		}
		catch(Exception e)
		{
			ret.setException(e);					
		}
		
		return ret;
	}
	
//	/**
//	 *  Execute the task.
//	 *  @param context	The accessible values.
//	 *  @param process	The process instance executing the task.
//	 *  @return	To be notified, when the task has completed.
//	 */
//	public IFuture execute(final ITaskContext context, final IInternalAccess process)
//	{
//		final Future	ret	= new Future();
//		String	service	= null;
//		String	method	= null;
//		String	resultparam	= null;
//		boolean	rebind	= false;
//		
//		// Collect arguments and settings.
//		final List	args	= new ArrayList();
//		final List	argtypes	= new ArrayList();
//		IndexMap<String, MParameter>	mparams	= context.getActivity().getParameters();
//		for(Iterator it=mparams.values().iterator(); it.hasNext(); )
//		{
//			MParameter	param	= (MParameter)it.next();
//			if(PARAMETER_SERVICE.equals(param.getName()))
//			{
//				service	= (String)context.getParameterValue(param.getName());
//			}
//			else if(PARAMETER_METHOD.equals(param.getName()))
//			{
//				method	= (String)context.getParameterValue(param.getName());		
//			}
//			else if(PARAMETER_REBIND.equals(param.getName()))
//			{
//				Object	val	= context.getParameterValue(param.getName());
//				rebind	= val!=null ? ((Boolean)val).booleanValue() : false;
//			}
//			else if(MParameter.DIRECTION_IN.equals(param.getDirection()))
//			{
//				args.add(context.getParameterValue(param.getName()));
//				argtypes.add(param.getClazz().getType(process.getClassLoader(), process.getModel().getAllImports()));
//			}
//			else if(MParameter.DIRECTION_INOUT.equals(param.getDirection()))
//			{
//				if(resultparam!=null)
//					throw new RuntimeException("Only one 'out' parameter allowed for ServiceCallTask: "+context);
//				
//				resultparam	= param.getName();
//				args.add(context.getParameterValue(param.getName()));
//				argtypes.add(param.getClazz().getType(process.getClassLoader(), process.getModel().getAllImports()));
//			}
//			else if(MParameter.DIRECTION_OUT.equals(param.getDirection()))
//			{
//				if(resultparam!=null)
//					throw new RuntimeException("Only one 'out' parameter allowed for ServiceCallTask: "+context);
//				
//				resultparam	= param.getName();
//			}
//		}
//		
//		// Apply shortcuts, if necessary.
//		if(service==null && context.getActivity().getLane()!=null)
//		{
//			service	= context.getActivity().getLane().getName();
//		}
//		if(method==null)
//		{
//			method	= context.getActivity().getName();
//		}
//		
//		if(service==null)
//		{
//			throw new RuntimeException("No 'service' specified for ServiceCallTask: "+context);
//		}
//		if(method==null)
//		{
//			throw new RuntimeException("No 'method' specified for ServiceCallTask: "+context);
//		}
//		
//		// Fetch service and call method.
//		final String	fservice	= service;
//		final String	fmethod	= method;
//		final String	fresultparam	= resultparam;
//		process.getServiceContainer().getService(service, rebind)
//			.addResultListener(new DelegationResultListener(ret)
//		{
//			public void customResultAvailable(Object result)
//			{
//				Method	m	= SReflect.getMethod(result.getClass(), fmethod, (Class[])argtypes.toArray(new Class[argtypes.size()]));
//				if(m==null)
//				{
//					throw new RuntimeException("Method "+fmethod+argtypes+" not found for service "+fservice+": "+context);
//				}
//				try
//				{
//					Object	val	= m.invoke(result, args.toArray());
//					if(val instanceof IFuture)
//					{
//						((IFuture)val).addResultListener(new DelegationResultListener(ret)
//						{
//							public void customResultAvailable(Object result)
//							{
//								if(fresultparam!=null)
//									context.setParameterValue(fresultparam, result);
//								ret.setResult(null);
//							}
//						});
//					}
//					else
//					{
//						if(fresultparam!=null)
//							context.setParameterValue(fresultparam, val);
//						ret.setResult(null);
//					}
//				}
//				catch(InvocationTargetException ite)
//				{
//					ret.setException((Exception)ite.getTargetException());
//				}
//				catch(Exception e)
//				{
//					ret.setException(e);					
//				}
//			}
//		});
//		
//		return ret;
//	}
	
	/**
	 *  Cancel the task.
	 *  @return	To be notified, when the compensation has completed.
	 */
	public IFuture<Void> cancel(IComponent instance)
	{
		// Todo: how to interrupt service call!?
		// should call terminate if service call is terminable
		return IFuture.DONE;
	}
	
	/**
	 *  Get the extra parameters that depend on the property settings of the task.
	 * /
	public static List<ParameterMetaInfo> getExtraParameters(Map<String, MProperty> params, IModelContainer modelcontainer, ClassLoader cl)
	{
		List<ParameterMetaInfo> ret = new ArrayList<ParameterMetaInfo>();
		
		IModelInfo mi = modelcontainer.getBpmnModel().getModelInfo();
		try
		{
			MProperty msparam = params.get(PROPERTY_SERVICE);
			MProperty mmparam = params.get(PROPERTY_METHOD);
			
			if(msparam!=null && mmparam!=null)
			{
				String reqname = (String)SJavaParser.evaluateExpression(msparam.getInitialValue().getValue(), mi.getAllImports(), null, cl);
				String methodname = (String)SJavaParser.evaluateExpression(mmparam.getInitialValue().getValue(), mi.getAllImports(), null, cl);
				
				if(reqname!=null && methodname!=null)
				{
					RequiredServiceInfo reqser = mi.getService(reqname);
					if(reqser!=null)
					{
						Class<?> type = reqser.getType().getType(cl==null? ServiceCallTask.class.getClassLoader(): cl, mi.getAllImports());
						
						if(type!=null)
						{
							Method[] ms = type.getMethods();
							// todo check parameter types?
							for(Method m: ms)
							{
								if(SReflect.getMethodSignature(m).equals(methodname) || m.toString().equals(methodname))
								{
									List<String> names = modelcontainer.getParameterNames(m);
									String retname = modelcontainer.getReturnValueName(m);
									Type[] ptypes = m.getGenericParameterTypes();
									Type pret = m.getGenericReturnType();
									
									// Unwrap generic type if is future call
									if(SReflect.isSupertype(IFuture.class, m.getReturnType()))
									{
										if(pret instanceof ParameterizedType)
										{
											ParameterizedType pt = (ParameterizedType)pret;
											Type[] pts = pt.getActualTypeArguments();
											pret = pts[0]; // Hack, but works for IFuture etc.
										}
									}
									
									for(int j=0; j<ptypes.length; j++)
									{
										ret.add(new ParameterMetaInfo(ParameterMetaInfo.DIRECTION_IN, new ClassInfo(ptypes[j]), names!=null && names.size()>0? names.get(j): "param"+j, null, null));
									}
									if(!pret.equals(Void.class) && !pret.equals(void.class))
									{
										if(SReflect.isSupertype(IIntermediateFuture.class, m.getReturnType()))
										{
											ret.add(new ParameterMetaInfo(ParameterMetaInfo.DIRECTION_OUT, new ClassInfo("java.util.Collection<" + SReflect.getClass(pret).getName() + ">"), retname==null? "return": retname, null, null));
										}
										else
										{
											ret.add(new ParameterMetaInfo(ParameterMetaInfo.DIRECTION_OUT, new ClassInfo(pret), retname==null? "return": retname, null, null));
										}
									}
								}
							}
						}
					}
				}
			}	
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	
		return ret;
	}*/
	
	/**
	 * 
	 */
	public static class ServiceCallTaskGui //implements ITaskPropertyGui
	{
		/** The panel. */
		protected JPanel panel;
		
		/** The model. */
		protected IModelInfo model;
		
		/** The task. */
		protected MActivity task;
		
		/** The classloader. */
		protected ClassLoader cl;
		
		/** The combo box for the service name. */
		protected JComboBox cbsername;
		
		/** The combo box for the method name. */
		protected JComboBox cbmethodname;
		
		/** The combo box for the ranking. */
		//protected AutoCompleteCombo cbranking;
		
		/** The container. */
		protected IModelContainer container;
		
		/**
		 *  Once called to init the component.
		 * /
		public void init(final IModelContainer container, final MActivity task, final ClassLoader cl)
		{
			this.container = container;
			this.model = container.getBpmnModel().getModelInfo();
			this.task = task;
			this.cl = cl;
			PropertiesPanel pp = new PropertiesPanel();
			
			cbsername = pp.createComboBox("Required service name:", null);
			cbmethodname = pp.createComboBox("Method name", null);
			cbmethodname.setRenderer(new BasicComboBoxRenderer()
			{
				public Component getListCellRendererComponent(JList list, Object value,
					int index, boolean isSelected, boolean cellHasFocus)
				{
					Method method = (Method)value;
					String txt = null;
					if(method!=null)
						txt = SReflect.getMethodSignature(method);
					return super.getListCellRendererComponent(list, txt, index, isSelected, cellHasFocus);
				}
			});
			
			cbranking = new AutoCompleteCombo(null, cl);
			final FixedClassInfoComboModel mo = new FixedClassInfoComboModel(cbranking, -1, new ArrayList<ClassInfo>(container.getAllClasses()));
			cbranking.setModel(mo);
			pp.addComponent("Ranking", cbranking);
			
			cbsername.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					String reqname = (String)cbsername.getSelectedItem();
					
					task.setProperty(PROPERTY_SERVICE, reqname, true);
					
					if(reqname!=null && model.getService(reqname)!=null)
					{
						RequiredServiceInfo reqser = model.getService(reqname);
						Class<?> type = reqser.getType().getType(cl==null? ServiceCallTask.class.getClassLoader(): cl, model.getAllImports());
						
						if(type!=null)
						{
							ActionListener[] als = cbmethodname.getActionListeners();
							for(ActionListener al: als)
								cbmethodname.removeActionListener(al);
							
							DefaultComboBoxModel mo = ((DefaultComboBoxModel)cbmethodname.getModel());
							mo.removeAllElements();
							Method[] ms = type.getMethods();
							mo.addElement(null);
							for(Method m: ms)
							{
								mo.addElement(m);
							}
							
							for(ActionListener al: als)
								cbmethodname.addActionListener(al);
						}
					}
				}
			});
			
			cbmethodname.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					Method method = (Method)cbmethodname.getSelectedItem();
					task.setProperty(PROPERTY_METHOD, method==null? null: SReflect.getMethodSignature(method), true);
				}
			});
			
			cbranking.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					ClassInfo ci = (ClassInfo)cbranking.getSelectedItem();
					task.setProperty(PROPERTY_RANKING, ci==null || ci.toString().length()==0? null: ci.toString(), true);
				}
			});
			
			refresh();
			
			panel = pp;
		}*/
		
		/**
		 * 
		 * /
		protected void refresh()
		{
			DefaultComboBoxModel mo = ((DefaultComboBoxModel)cbsername.getModel());
			mo.removeAllElements();
			
			RequiredServiceInfo[] reqs = model.getServices();
			
			ActionListener[] als = cbsername.getActionListeners();
			for(ActionListener al: als)
				cbsername.removeActionListener(al);
			if(reqs!=null)
			{
				mo.addElement(null);
				for(int i=0; i<reqs.length; i++)
				{
					mo.addElement(reqs[i].getName());
				}
			}
			for(ActionListener al: als)
				cbsername.addActionListener(al);
			
			if(task.getProperties()!=null)
			{
				MProperty mprop = task.getProperties().get(PROPERTY_SERVICE);
				if(mprop!=null && mprop.getInitialValue()!=null)
				{
					String sername = (String)SJavaParser.parseExpression(mprop.getInitialValue(), model.getAllImports(), cl).getValue(null);
					cbsername.setSelectedItem(sername);
//					System.out.println("sel item: "+sername);
				
					mprop = task.getProperties().get(PROPERTY_METHOD);
					if(mprop!=null && mprop.getInitialValue()!=null)
					{
						String methodname = (String)SJavaParser.parseExpression(mprop.getInitialValue(), model.getAllImports(), cl).getValue(null);
//						System.out.println(task.getName()+" "+mprop.getInitialValueString());
						
						RequiredServiceInfo reqser = model.getService(sername);
						if(reqser!=null)
						{
							Class<?> type = reqser.getType().getType(cl==null? ServiceCallTask.class.getClassLoader(): cl, model.getAllImports());
							
							if(type!=null)
							{
								Method[] ms = type.getMethods();
								for(Method m: ms)
								{
									if(SReflect.getMethodSignature(m).equals(methodname))
									{
										cbmethodname.setSelectedItem(m);
		//								System.out.println("sel item2: "+methodname);
									}
								}
							}
						}
					}
				}
				
				mprop = task.getProperties().get(PROPERTY_RANKING);
				if(mprop!=null && mprop.getInitialValue()!=null)
				{
					String rankclname = (String)SJavaParser.parseExpression(mprop.getInitialValue(), model.getAllImports(), cl).getValue(null);
					if(rankclname!=null)
					{ 
						ClassInfo ci = new ClassInfo(rankclname);
						cbranking.setSelectedItem(ci);
					}
				}
			}
		}*/
		
		/**
		 *  Informs the panel that it should stop all its computation.
		 */
		public void shutdown()
		{
		}
		
		/**
		 *  The component to be shown in the gui.
		 *  @return	The component to be displayed.
		 */
		public JComponent getComponent()
		{
			return panel;
		}
	}
}
