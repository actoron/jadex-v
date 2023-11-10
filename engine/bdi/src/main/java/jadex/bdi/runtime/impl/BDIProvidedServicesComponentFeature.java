package jadex.bdi.runtime.impl;

/**
 *  Overriden to allow for service implementations to be directly mapped to plans.
 */
public class BDIProvidedServicesComponentFeature //extends ProvidedServicesComponentFeature
{
//	//-------- constructors --------
//	
//	/**
//	 *  Factory method constructor for instance level.
//	 */
//	public BDIProvidedServicesComponentFeature(IInternalAccess component, ComponentCreationInfo cinfo)
//	{
//		super(component, cinfo);
//	}
//	
//	/**
//	 *  Init a service.
//	 *  Overriden to allow for service implementations as BPMN processes using signal events.
//	 */
//	public IFuture<Object> createServiceImplementation(ProvidedServiceInfo info, IValueFetcher fetcher)
//	{
//		// todo: cleanup this HACK!!!
//		if(getComponent().getFeature0(IPojoComponentFeature.class)!=null)
//		{
//			int i = info.getName()!=null ? info.getName().indexOf(MElement.CAPABILITY_SEPARATOR) : -1;
//			Object	ocapa	= getComponent().getFeature(IPojoComponentFeature.class).getPojoAgent();
//			String	capa	= null;
//			if(i!=-1)
//			{
//				capa	= info.getName().substring(0, i); 
//				SimpleValueFetcher fet = new SimpleValueFetcher(fetcher);
//				ocapa = ((BDIAgentFeature)getComponent().getFeature(IBDIAgentFeature.class)).getCapabilityObject(capa);
//				fet.setValue("$pojocapa", ocapa);
//				fetcher = fet;
//				
//				Set<Object> vals = new HashSet<Object>();
//				vals.add(ocapa);
//				vals.add(new CapabilityPojoWrapper(getInternalAccess(), ocapa, capa));
//				hackguesser = new SimpleParameterGuesser(super.getParameterGuesser(), vals);
//			}
//			else
//			{
//				hackguesser = null;
//			}
//		}
//		
//		// Support special case that BDI should implement provided service with plans.
//		Future<Object> ret = new Future<>();
//		ProvidedServiceImplementation impl = info.getImplementation();
//		if(impl!=null && impl.getClazz()!=null && impl.getClazz().getType(getComponent().getClassLoader()).equals(IBDIAgent.class))
//		{
//			Class<?> iface = info.getType().getType(getComponent().getClassLoader());
//			Object res = ProxyFactory.newProxyInstance(getComponent().getClassLoader(), new Class[]{iface}, 
//				new BDIServiceInvocationHandler(getInternalAccess(), iface));
//			ret.setResult(res);
//		}
//		else
//		{
//			super.createServiceImplementation(info, fetcher).delegateTo(ret);
//		}
//		
////		hackguesser = null;
//		
//		return ret;
//	}
//	
////	public IValueFetcher getValueFetcher()
////	{
////		return super.getValueFetcher();
////	}
//	
//	protected IParameterGuesser hackguesser;
//	public IParameterGuesser getParameterGuesser()
//	{
//		return hackguesser!=null? hackguesser: super.getParameterGuesser();
//	}
}