package jadex.bdiv3x.runtime;

import jadex.bdiv3.actions.FindApplicableCandidatesAction;
import jadex.bdiv3.model.MInternalEvent;
import jadex.bdiv3.runtime.impl.RElement;
import jadex.bdiv3.runtime.impl.RProcessableElement;
import jadex.mj.feature.execution.IMjExecutionFeature;

/**
 *  The event base runtime element.
 */
public class REventbase extends RElement implements IEventbase
{
	//-------- attributes --------
	
	/** The scope (for local views). */
	protected String	scope;
	
	//-------- constructors --------
	
	/**
	 *  Create a new goalbase.
	 */
	public REventbase(String scope)
	{
		super(null);
		this.scope	= scope;
	}

	//-------- IEventbase interface --------
	
//	/**
//	 *  Send a message after some delay.
//	 *  @param me	The message event.
//	 *  @return The filter to wait for an answer.
//	 */
//	public IFuture<Void> sendMessage(IMessageEvent<?> me)
//	{
//		return getAgent().getFeature(IMessageFeature.class).sendMessage(me.getMessage());
//	}

	/**
	 *  Dispatch an event.
	 *  @param event The event.
	 */
	public void dispatchInternalEvent(IInternalEvent event)
	{
		FindApplicableCandidatesAction fac = new FindApplicableCandidatesAction((RProcessableElement)event);
		IMjExecutionFeature.get().scheduleStep(fac);
	}

//	/**
//	 *  Create a new message event.
//	 *  @return The new message event.
//	 */
//	public IMessageEvent createMessageEvent(String type)
//	{
//		MMessageEvent mevent = getCapability().getMCapability().getResolvedMessageEvent(scope, type);
//		return new RMessageEvent(mevent, getAgent(), (MConfigParameterElement)null);
//	}

//	/**
//	 *  Create a reply to a message event.
//	 *  @param event	The received message event.
//	 *  @param type	The reply message event type.
//	 *  @return The reply event.
//	 */
//	public <T> IMessageEvent<T>	createReply(IMessageEvent<T> event, String type)
//	{
//		if(event==null)
//			throw new IllegalArgumentException("Event must not null");
//		
//		if(event.getMessage() instanceof FipaMessage)
//		{
//			FipaMessage	reply	= ((FipaMessage)event.getMessage()).createReply();
//			MMessageEvent mevent = getCapability().getMCapability().getResolvedMessageEvent(scope, type);
//			// TODO: set parameter values from model???
//			@SuppressWarnings("unchecked")
//			RMessageEvent<T>	ret	= new RMessageEvent<T>(mevent, (T)reply, getAgent(), (RMessageEvent<T>)event);
//			return ret;
//		}
//		else
//		{
//			throw new UnsupportedOperationException("Currently only FipaMessage supported: "+event.getMessage());
//		}
//	}
	
	/**
	 *  Create a new intenal event.
	 *  @return The new intenal event.
	 */
	public IInternalEvent createInternalEvent(String type)
	{
		MInternalEvent mevent = getCapability().getMCapability().getResolvedInternalEvent(scope, type);
		return new RInternalEvent(mevent, null);
	}

//	/**
//	 *  Register a conversation or reply_with to be able
//	 *  to send back answers to the source capability.
//	 *  @param msgevent The message event.
//	 *  todo: indexing for msgevents for speed.
//	 */
//	public void registerMessageEvent(IMessageEvent mevent);
//	
//	/**
//	 *  Remove a registered message event.
//	 *  @param msgevent The message event.
//	 */
//	public void deregisterMessageEvent(IMessageEvent mevent);
}
