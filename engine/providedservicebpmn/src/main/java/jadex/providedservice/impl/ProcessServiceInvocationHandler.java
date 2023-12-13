package jadex.providedservice.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import jadex.bpmn.model.MActivity;
import jadex.bpmn.model.MBpmnModel;
import jadex.bpmn.model.MSubProcess;
import jadex.bpmn.runtime.IBpmnComponentFeature;
import jadex.bpmn.runtime.impl.IInternalBpmnComponentFeature;
import jadex.bpmn.runtime.impl.ProcessThread;
import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.common.UnparsedExpression;
import jadex.core.IComponent;
import jadex.execution.future.FutureFunctionality;
import jadex.future.Future;
import jadex.javaparser.SJavaParser;
import jadex.model.IModelFeature;
import jadex.providedservice.annotation.Service;

// todo

/**
 *  Invocation handler for mapping service requests to
 *  start events of BPMN processes.
 */
@Service	// Hack!!! Let BasicServiceInvocationHandler know that this is a service implementation.
// Used to avoid: Pojo service should declare @Service annotation 
public class ProcessServiceInvocationHandler implements InvocationHandler
{
	//-------- constants --------
	
	// todo: unify with constant in ProcessThread 
	/** The future result parameter name. */
	public static final String	THREAD_PARAMETER_SERVICE_RESULT	= "$$service_result";
	
	/** The user result parameter name. */
	// Todo: remove. use explicit model.
	//public static final String	EVENT_PARAMETER_SERVICE_RESULT	= "service_result";
	
	//-------- attributes --------
	
	/** The process instance. */
	protected IComponent instance;
	
	/** The method / event mapping. */
	protected Map<String, MActivity> events;
	
	//-------- constructors --------
	
	/**
	 *  Create a new process service invocation handler.
	 */
	public ProcessServiceInvocationHandler(IComponent instance, Map<String, MActivity> events)
	{
		this.instance	= instance;
		this.events	= events;
	}
	
	/**
	 *  Create a new process service invocation handler.
	 */
	public ProcessServiceInvocationHandler(IComponent instance, String actid)
	{
		this.instance	= instance;
		
		//MBpmnModel model = (MBpmnModel)instance.getModel().getRawModel();
		MBpmnModel model = (MBpmnModel)instance.getFeature(IModelFeature.class).getModel().getRawModel();
		
		MSubProcess proc = (MSubProcess)model.getActivityById(actid);
		final Map<MSubProcess, List<MActivity>> evtsubstarts = model.getEventSubProcessStartEventMapping();
		
		List<MActivity> macts = evtsubstarts.get(proc);
		
		events = new HashMap<String, MActivity>();
				
		Class<?> iface = null;
		for(MActivity mact: macts)
		{
			if(MBpmnModel.EVENT_START_MESSAGE.equals(mact.getActivityType()))
			{
				if(mact.hasPropertyValue(MActivity.IFACE))
				{
					if(iface==null)
					{
						UnparsedExpression uexp = mact.getPropertyValue(MActivity.IFACE);
						iface = (Class<?>)SJavaParser.parseExpression(uexp, model.getModelInfo().getAllImports(), instance.getClass().getClassLoader()).getValue(null);
					}
					
					UnparsedExpression uexp = mact.getPropertyValue(MActivity.METHOD);
					String method = (String)SJavaParser.parseExpression(uexp, model.getModelInfo().getAllImports(), instance.getClass().getClassLoader()).getValue(null);
//					String method = mact.getPropertyValue("method");
					events.put(method, mact);
				}
			}
		}
	}
	
	//-------- InvocationHandler interface --------
	
	/**
	 *  Called when a method is invoked on a proxy.
	 */
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
	{
		// Drop goal when future is terminated from service caller
		final Future<Object> ret = (Future<Object>)FutureFunctionality.getDelegationFuture(method.getReturnType(), new FutureFunctionality((Logger)null));
//		{
//			public void terminate(Exception reason, IResultListener<Void> terminate)
//			{
//				System.out.println("terminated call: "+fgoal);
//				ip.dropGoal(fgoal);
//				super.terminate(reason, terminate);
//			}
//		});
		
//		Future<Void> ret = new Future<Void>();
		
		MActivity act = events.get(SReflect.getMethodSignature(method));
		if(act==null)
			act = events.get(method.toString());
		ProcessThread	thread	= new ProcessThread(act, ((IInternalBpmnComponentFeature)instance.getFeature(IBpmnComponentFeature.class)).getTopLevelThread(), instance);
		((IInternalBpmnComponentFeature)instance.getFeature(IBpmnComponentFeature.class)).getTopLevelThread().addThread(thread);

//		List<MParameter> params	= act.getParameters(new String[]{MParameter.DIRECTION_IN, MParameter.DIRECTION_INOUT});
//		String[] params	= act.getPropertyNames();
//		if(params!=null && args!=null)
//		{
//			for(int i=0; i<params.size() && i<args.length; i++)
//			{
//				thread.setOrCreateParameterValue(params.get(i).getName(), args[i]);
//			}
//		}
		
		if(args!=null)
		{
			for(int i=0; i<args.length; i++)
			{
	//			MParameter mparam = act.getParameter("param"+i);
				thread.setOrCreateParameterValue("param"+i, args[i]);
			}
		}
		
		thread.setOrCreateParameterValue("$callargs", args);
		thread.setOrCreateParameterValue(THREAD_PARAMETER_SERVICE_RESULT, ret);
		
		String[] pnames = act.getPropertyNames();
		if(pnames!=null)
		{
			boolean hasval = false;
			for(int i=0; i<pnames.length && i<pnames.length && !hasval; i++)
			{
				hasval |= act.hasInitialPropertyValue(pnames[i]);
			}

			// If no initial values have been defined try to copy 1:1
			if(!hasval)
			{
				if(args.length==pnames.length)
				{
					for(int i=0; i<pnames.length && i<pnames.length; i++)
					{
						thread.setOrCreateParameterValue(pnames[i], args[i]);
					}
				}
				else
				{
					throw new RuntimeException("Parameter mapping problem in service call: "+SUtil.arrayToString(args));
				}
			}
			else
			{
				for(int i=0; i<pnames.length && i<pnames.length; i++)
				{
					Object val = thread.getPropertyValue(pnames[i]);
					thread.setOrCreateParameterValue(pnames[i], val);
				}
			}
		}
		
		// Hack for old style service impls as signal events
		// The signal events otherwise waits on the signal
		if(act.isSignalEvent())
		{
			((IInternalBpmnComponentFeature)instance.getFeature(IBpmnComponentFeature.class)).step(act, instance, thread, null);
		}
		
		return ret;
	}
}
