package jadex.nfproperty.impl;

import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.future.IFuture;

/**
 *  Injects properties on root component.
 */
public abstract class NFRootProperty<T, U> extends SimpleValueNFProperty<T, U>
{
	/** The root access. */
	protected IComponentHandle root;
	
	/** The flag if the property has been injected to the root component. */
	protected boolean injected;
	
	/**
	 *  Create a new property.
	 */
	public NFRootProperty(final IComponent comp, final NFPropertyMetaInfo mi)
	{
		this(comp, mi, true);
	}
	
	/**
	 *  Create a new property.
	 */
	public NFRootProperty(final IComponent comp, final NFPropertyMetaInfo mi, boolean inject)
	{
		super(comp, mi);
		if(inject)
			injectPropertyToRootComponent();
	}
	
	/**
	 *  Inject the property to the root component.
	 */
	protected IFuture<Void> injectPropertyToRootComponent()
	{
		throw new UnsupportedOperationException();
		
		/*final Future<Void> ret = new Future<Void>();

		if(!injected)
		{
			this.injected = true;
			
			// Add property to root component
//			IComponentManagementService cms = comp.getFeature(IRequiredServicesFeature.class).getLocalService(new ServiceQuery<>(IComponentManagementService.class));
			comp.getExternalAccessAsync(comp.getId().getRoot()).addResultListener(new DefaultResultListener<IExternalAccess>()
			{
				public void resultAvailable(IExternalAccess root)
				{
					NFRootProperty.this.root = root;
					INFPropertyMetaInfo mi = getMetaInfo();
					NFPropertyMetaInfo cmi = new NFPropertyMetaInfo(mi.getName(), mi.getType(), mi.getUnit(), mi.isDynamic(), mi.getUpdateRate(), mi.isRealtime(), Target.Root);
//					((INFPropertyProvider)root.getExternalComponentFeature(INFPropertyComponentFeature.class)).addNFProperty(new NFPropertyRef<T, U>((INFPropertyProvider)comp.getExternalAccess().getExternalComponentFeature(INFPropertyComponentFeature.class), root, cmi)).addResultListener(new DelegationResultListener<Void>(ret));
					
					NFPropertyRef<T, U> pr = new NFPropertyRef<T, U>(comp.getExternalAccess(), cmi, null, null);
					root.addNFProperty(pr).addResultListener(new DelegationResultListener<Void>(ret));
				}
			});
		}
		else
		{
			ret.setResult(null);
		}
		
		return ret;*/
	}
	
	/**
	 *  Property was removed and should be disposed.
	 */
	public IFuture<Void> dispose()
	{
		throw new UnsupportedOperationException();
		
		/*final Future<Void> ret = new Future<Void>();
		
		if(root!=null && injected)
		{
//			((INFPropertyProvider)root.getExternalComponentFeature(INFPropertyComponentFeature.class)).removeNFProperty(getName());//.addResultListener(new DelegationResultListener<Void>(ret));
			root.removeNFProperty(getName()).addResultListener(new IResultListener<Void>()
			{
				public void resultAvailable(Void result)
				{
					NFRootProperty.super.dispose().addResultListener(new DelegationResultListener<Void>(ret));
				}
				
				public void exceptionOccurred(Exception exception)
				{
					NFRootProperty.super.dispose().addResultListener(new DelegationResultListener<Void>(ret));
				}
			});
		}
		else
		{
			return super.dispose();
		}
		
		return ret;*/
	}

	/**
	 *  Get the injected.
	 *  @return The injected.
	 */
	public boolean isInjected()
	{
		return injected;
	}
}

