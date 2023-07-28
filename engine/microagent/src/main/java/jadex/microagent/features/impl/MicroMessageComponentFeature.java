	package jadex.microagent.features.impl;

import jadex.common.MethodInfo;
import jadex.enginecore.IInternalAccess;
import jadex.enginecore.component.ComponentCreationInfo;
import jadex.enginecore.component.IComponentFeatureFactory;
import jadex.enginecore.component.IMessageFeature;
import jadex.enginecore.component.IMsgHeader;
import jadex.enginecore.component.impl.ComponentFeatureFactory;
import jadex.enginecore.component.impl.MessageComponentFeature;
import jadex.enginecore.component.streams.IConnection;
import jadex.enginecore.service.types.security.ISecurityInfo;
import jadex.future.IResultListener;
import jadex.microagent.MicroModel;
import jadex.microagent.annotation.OnMessage;
import jadex.microagent.annotation.OnStream;

/**
 *  Extension to allow message injection in agent methods.
 */
public class MicroMessageComponentFeature extends MessageComponentFeature
{
	//-------- constants --------
	
	/** The factory. */
	public static final IComponentFeatureFactory FACTORY = new ComponentFeatureFactory(IMessageFeature.class, MicroMessageComponentFeature.class);
	
	//-------- constructors --------
	
	/**
	 *  Create the feature.
	 */
	public MicroMessageComponentFeature(IInternalAccess component, ComponentCreationInfo cinfo)
	{
		super(component, cinfo);
	}
	
	//-------- micro agent specific message handling --------
	
	/**
	 *  Called for all messages without matching message handlers.
	 *  Can be overwritten by specific message feature implementations (e.g. micro or BDI).
	 */
	protected void processUnhandledMessage(final ISecurityInfo secinf, final IMsgHeader header, final Object body)
	{
		///WTF?
//		if(body instanceof StreamPacket)
//		{
			MicroModel model = (MicroModel)component.getModel().getRawModel();
			MethodInfo mi = model.getAgentMethod(OnMessage.class);
		
			if(mi!=null)
			{
				MicroLifecycleComponentFeature.invokeMethod(getInternalAccess(), OnMessage.class, new Object[]{secinf, header, body, body != null ? body.getClass() : null})
					.addResultListener(new IResultListener<Void>()
				{
					@Override
					public void resultAvailable(Void result)
					{
						// OK -> ignore
					}
					
					@Override
					public void exceptionOccurred(Exception exception)
					{
						getComponent().getLogger().warning("Exception during message handling: "+exception);
					}
				});
			}
			else
			{
				MicroLifecycleComponentFeature.invokeMethod(getInternalAccess(), OnMessage.class, new Object[]{secinf, header, body, body != null ? body.getClass() : null})
					.addResultListener(new IResultListener<Void>()
				{
					@Override
					public void resultAvailable(Void result)
					{
						// OK -> ignore
					}
					
					@Override
					public void exceptionOccurred(Exception exception)
					{
						getComponent().getLogger().warning("Exception during message handling: "+exception);
					}
				});
			}
//		}
	}	
	
	/**
	 *  Inform the component that a stream has arrived.
	 *  @param con The stream that arrived.
	 */
	public void streamArrived(IConnection con)
	{
		MicroModel model = (MicroModel)component.getModel().getRawModel();
		MethodInfo mi = model.getAgentMethod(OnStream.class);
	
		if(mi!=null)
		{
			MicroLifecycleComponentFeature.invokeMethod(getInternalAccess(), OnStream.class, new Object[]{con})
				.addResultListener(new IResultListener<Void>()
			{
				@Override
				public void resultAvailable(Void result)
				{
					// OK -> ignore
				}
				
				@Override
				public void exceptionOccurred(Exception exception)
				{
					getComponent().getLogger().warning("Exception during message handling: "+exception);
				}
			});
		}
		else
		{
			MicroLifecycleComponentFeature.invokeMethod(getInternalAccess(), OnStream.class, new Object[]{con})
				.addResultListener(new IResultListener<Void>()
			{
				@Override
				public void resultAvailable(Void result)
				{
					// OK -> ignore
				}
				
				@Override
				public void exceptionOccurred(Exception exception)
				{
					getComponent().getLogger().warning("Exception during message handling: "+exception);
				}
			});
		}
	}
}