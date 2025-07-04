package jadex.remoteservice.impl.remotecommands;

import java.lang.reflect.Method;
import java.util.Map;

import javax.management.ServiceNotFoundException;

import jadex.common.MethodInfo;
import jadex.common.SUtil;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.messaging.ISecurityInfo;
import jadex.providedservice.IProvidedServiceFeature;
import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.annotation.Security;
import jadex.providedservice.impl.service.ServiceIdentifier;
import jadex.remoteservice.IRemoteCommand;


/**
 *  Invoke a remote method.
 */
public class RemoteMethodInvocationCommand<T> extends AbstractInternalRemoteCommand implements IRemoteCommand<T>, ISecuredRemoteCommand
{
	//-------- attributes --------

	/** The target id. */
	private Object target;
	
	/** The remote method. */
	private MethodInfo method;
	
	/** The arguments. */
	private Object[] args;

	/**
	 *  Create a remote method invocation command.
	 */
	public RemoteMethodInvocationCommand()
	{
		// Bean constructor.
	}

	/**
	 *  Create a remote method invocation command.
	 */
	public RemoteMethodInvocationCommand(String id, ComponentIdentifier sender, Object target, Method method, Object[] args, Map<String, Object> nonfunc)
	{
		super(id, sender, nonfunc);
		this.target	= target;
		this.method	= new MethodInfo(method);
		this.args	= args;
//		System.out.println("created rmi command: "+target+" "+method.getName());
		
		//if(method.toString().toLowerCase().indexOf("registryv2")!=-1)
		//	System.out.println("Creating command for: "+method);
	}

	/**
	 *  Get the target id.
	 */
	public Object getTargetId()
	{
		return target;
	}
	
	/**
	 *  Set the target id.
	 *  @return The command (builder pattern).
	 */
	public RemoteMethodInvocationCommand<T> setTargetId(Object target)
	{
		this.target	= target;
		return this;
	}

	/**
	 *  Get the method.
	 */
	public MethodInfo	getMethod()
	{
		return method;
	}
	
	/**
	 *  Set the method.
	 *  @return The command (builder pattern).
	 */
	public RemoteMethodInvocationCommand<T>	setMethod(MethodInfo method)
	{
		this.method	= method;
		return this;
	}

	/**
	 *  Get the arguments.
	 */
	public Object[]	getArguments()
	{
		return args;
	}
	
	/**
	 *  Set the arguments.
	 *  @return The command (builder pattern).
	 */
	public RemoteMethodInvocationCommand<T>	setArguments(Object[] args)
	{
		this.args = args;
		return this;
	}

	/**
	 *  Execute the method.
	 */
	public IFuture<T> execute(IComponent component, ISecurityInfo secinf)
	{
//		if(method.toString().toLowerCase().indexOf("getdesc")!=-1)
//			System.out.println("Executing requested remote method invocation: "+access.getId()+", "+method);
		
		Object ret	= null;
		if(target instanceof IServiceIdentifier)
		{
			IServiceIdentifier	sid	= (IServiceIdentifier)target;
			if(sid.getProviderId().equals(component.getId()))
			{
				try
				{
					Method m = method.getMethod(ComponentManager.get().getClassLoader());
					//if(m.getName().startsWith("method"))
					//	System.out.println("here");
					Object service = component.getFeature(IProvidedServiceFeature.class).getProvidedService(sid);
					if(service==null)
					{
						ret = new Future<Object>(new ServiceNotFoundException(sid.getServiceType()+" on component: "+component));
					}
					else
					{
						ret	= m.invoke(service, args);
					}
				}
				catch(NullPointerException nex)
				{
					ret	= new Future<Object>(nex);
				}
				catch(Exception e)
				{
					ret	= new Future<Object>(e);
				}				
			}
			else
			{
				ret	= new Future<Object>(new IllegalArgumentException("Can not invoke service of other component: "+component.getId()+", "+sid));
			}
		}
		else if(target instanceof ComponentIdentifier)
		{
			ComponentIdentifier cid	= (ComponentIdentifier)target;
			if(cid.equals(component.getId()))
			{
				try
				{
					Method	m	= method.getMethod(ComponentManager.get().getClassLoader());
					ret	= m.invoke(component.getComponentHandle(), args);
					
//					System.out.println("adding lis: "+Arrays.toString(args));
//					if(method.toString().toLowerCase().indexOf("getdesc")!=-1)
//					{
//						((IFuture)ret).addResultListener(new IResultListener()
//						{
//							public void exceptionOccurred(Exception exception)
//							{
//								System.out.println("ex: "+exception+" "+Arrays.toString(args));
//							}
//							
//							public void resultAvailable(Object result)
//							{
//								System.out.println("res is: "+result+" "+Arrays.toString(args));
//							}
//						});
//					}
				}
				catch(Exception e)
				{
					ret	= new Future<Object>(e);
				}
			}
			else
			{
				ret	= new Future<Object>(new IllegalArgumentException("Can not access other component: "+component.getId()+", "+cid));
			}			
		}
		
		@SuppressWarnings("unchecked")
		IFuture<T>	fret	= ret instanceof IFuture<?> ? (IFuture<T>)ret : new Future<T>((T)ret);
		return fret;
	}
	
	/**
	 *  Checks if the remote command is internally valid.
	 * 
	 *  @param component The component access.
	 *  @return Exception describing the error if invalid.
	 */
	public Exception isValid(IComponent component)
	{
		try
		{
			method.getMethod(ComponentManager.get().getClassLoader());
		}
		catch (Exception e)
		{
			return e;
		}
		return null;
	}
	
	/*protected static final Method SEARCHMETHOD;
	static
	{
		try
		{
			SEARCHMETHOD	= IRemoteRegistryService.class.getMethod("searchServices", ServiceQuery.class);
		}
		catch(NoSuchMethodException e)
		{
			throw SUtil.throwUnchecked(e);
		}
	}*/
	
	/**
	 *  Method to provide the required security level.
	 *  Overridden by subclasses.
	 */
	public Security getSecurityLevel(Component component)
	{
		return ServiceIdentifier.getSecurityLevel(component, method.getMethod(IComponentManager.get().getClassLoader()), target instanceof IServiceIdentifier? (IServiceIdentifier)target: null);
	}

	/**
	 *  Get a string representation.
	 */
	public String	toString()
	{
		return "RemoteMethodInvocationCommand("+method.getClassName()+"."+method.getName()+(args!=null?SUtil.arrayToString(args):"[]")+")";
	}
}
