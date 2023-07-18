package jadex.enginecore.component.impl.remotecommands;

import java.lang.reflect.Method;
import java.util.Map;

import jadex.common.MethodInfo;
import jadex.common.SUtil;
import jadex.enginecore.IComponentIdentifier;
import jadex.enginecore.IInternalAccess;
import jadex.enginecore.component.IRemoteCommand;
import jadex.enginecore.service.BasicService;
import jadex.enginecore.service.IServiceIdentifier;
import jadex.enginecore.service.annotation.Security;
import jadex.enginecore.service.component.IProvidedServicesFeature;
import jadex.enginecore.service.search.ServiceNotFoundException;
import jadex.enginecore.service.search.ServiceQuery;
import jadex.enginecore.service.types.registry.IRemoteRegistryService;
import jadex.enginecore.service.types.security.ISecurityInfo;
import jadex.future.Future;
import jadex.future.IFuture;

/**
 *  Invoke a remote method.
 */
public class RemoteMethodInvocationCommand<T>	extends AbstractInternalRemoteCommand	implements IRemoteCommand<T>, ISecuredRemoteCommand
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
	public RemoteMethodInvocationCommand(Object target, Method method, Object[] args, Map<String, Object> nonfunc)
	{
		super(nonfunc);
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
	public Object	getTargetId()
	{
		return target;
	}
	
	/**
	 *  Set the target id.
	 */
	public void	setTargetId(Object target)
	{
		this.target	= target;
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
	 */
	public void	setMethod(MethodInfo method)
	{
		this.method	= method;
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
	 */
	public void	setArguments(Object[] args)
	{
		this.args	= args;
	}

	/**
	 *  Execute the method.
	 */
	@Override
	public IFuture<T> execute(IInternalAccess access, ISecurityInfo secinf)
	{
//		if(method.toString().toLowerCase().indexOf("getdesc")!=-1)
//			System.out.println("Executing requested remote method invocation: "+access.getId()+", "+method);
		
		Object ret	= null;
		if(target instanceof IServiceIdentifier)
		{
			IServiceIdentifier	sid	= (IServiceIdentifier)target;
			if(sid.getProviderId().equals(access.getId()))
			{
				try
				{
					Method m = method.getMethod(access.getClassLoader());
					//if(m.getName().startsWith("method"))
					//	System.out.println("here");
					Object service = access.getFeature(IProvidedServicesFeature.class).getProvidedService(sid);
					if(service==null)
					{
						ret = new Future<Object>(new ServiceNotFoundException(sid.getServiceType()+" on component: "+access));
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
				ret	= new Future<Object>(new IllegalArgumentException("Can not invoke service of other component: "+access.getId()+", "+sid));
			}
		}
		else if(target instanceof IComponentIdentifier)
		{
			IComponentIdentifier	cid	= (IComponentIdentifier)target;
			if(cid.equals(access.getId()))
			{
				try
				{
					Method	m	= method.getMethod(access.getClassLoader());
					ret	= m.invoke(access.getExternalAccess(), args);
					
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
				ret	= new Future<Object>(new IllegalArgumentException("Can not access other component: "+access.getId()+", "+cid));
			}			
		}
		
		@SuppressWarnings("unchecked")
		IFuture<T>	fret	= ret instanceof IFuture<?> ? (IFuture<T>)ret : new Future<T>((T)ret);
		return fret;
	}
	
	/**
	 *  Checks if the remote command is internally valid.
	 * 
	 *  @param access The component access.
	 *  @return Exception describing the error if invalid.
	 */
	public Exception isValid(IInternalAccess access)
	{
		try
		{
			method.getMethod(access.getClassLoader());
		}
		catch (Exception e)
		{
			return e;
		}
		return null;
	}
	
	protected static final Method	SEARCHMETHOD;
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
	}
	
	/**
	 *  Method to provide the required security level.
	 *  Overridden by subclasses.
	 */
	@Override
	public Security	getSecurityLevel(IInternalAccess access)
	{
		return BasicService.getSecurityLevel(access, null, null, null, method.getMethod(access.getClassLoader()), target instanceof IServiceIdentifier? (IServiceIdentifier)target: null);
	}
	
	/**
	 *  Get a string representation.
	 */
	public String	toString()
	{
		return "RemoteMethodInvocationCommand("+method.getClassName()+"."+method.getName()+(args!=null?SUtil.arrayToString(args):"[]")+")";
	}
}
