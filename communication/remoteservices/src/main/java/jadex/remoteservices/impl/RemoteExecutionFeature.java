package jadex.remoteservices.impl;

import jadex.common.SUtil;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponentFeature;
import jadex.core.impl.Component;
import jadex.execution.future.FutureFunctionality;
import jadex.execution.impl.ILifecycle;
import jadex.future.*;
import jadex.messaging.IMessageFeature;
import jadex.messaging.IMessageHandler;
import jadex.messaging.ISecurityInfo;
import jadex.providedservice.annotation.Security;
import jadex.providedservice.impl.service.CallAccess;
import jadex.providedservice.impl.service.ServiceCall;
import jadex.providedservice.impl.service.ServiceIdentifier;
import jadex.remoteservices.IRemoteCommand;
import jadex.remoteservices.IRemoteConversationCommand;
import jadex.remoteservices.IRemoteExecutionFeature;
import jadex.remoteservices.IRemoteOrderedConversationCommand;
import jadex.remoteservices.impl.remotecommands.*;

import java.lang.reflect.Method;
import java.util.*;


/**
 *  Feature for securely sending and handling remote execution commands.
 */
public class RemoteExecutionFeature implements ILifecycle, IRemoteExecutionFeature, IInternalRemoteExecutionFeature
{
	//-------- constants ---------
	
	/** Put string representation of command in message header. */
	public static final boolean	DEBUG	= false;

	/** The factory. */
	//public static final IComponentFeatureFactory FACTORY = new ComponentFeatureFactory(IRemoteExecutionFeature.class, RemoteExecutionComponentFeature.class);
	
	/** Debug info of the remote execution command. */
	public static final String RX_DEBUG = "__rx_debug__";
	
	/** Commands safe to use with untrusted clients. */
	@SuppressWarnings("serial")
	protected static final Set<Class<?>> SAFE_COMMANDS	= Collections.unmodifiableSet(new HashSet<Class<?>>()
	{{
		// Unconditional commands
		add(RemoteFinishedCommand.class);
		add(RemoteForwardCmdCommand.class);
		add(RemoteIntermediateResultCommand.class);
		add(RemotePullCommand.class);
		add(RemoteBackwardCommand.class);
		add(RemoteResultCommand.class);
		add(RemoteTerminationCommand.class);

		// Conditional commands (throwing security exception in execute when not allowed).
//		add(RemoteSearchCommand.class);
		add(RemoteMethodInvocationCommand.class);
	}});

	/** The component. */
	protected Component component;

	/** Commands that have been sent to a remote component.
	 *  Stored to set return value etc. */
	protected Map<String, OutCommand> outcommands;
	
	/** Commands that have been received to be executed locally.
	 *  Stored to allow termination etc.*/
	protected Map<String, IFuture<?>> incommands;

	/** Default timeout, to be removed later. TODO: Remove */
	protected long timeout = 30000;
	
	/**
	 *  Create the feature.
	 */
	public RemoteExecutionFeature(Component component)
	{
		this.component = component;
	}
	
	/**
	 *  Initialize the feature.
	 */
	@Override
	public void	onStart()
	{
		component.getFeature(IMessageFeature.class).addMessageHandler(new RxHandler());
	}

	/**
	 *  Shutdown the feature.
	 */
	@Override
	public void onEnd()
	{
	}

	/**
	 *  Execute a command on a remote agent.
	 *  @param target	The component to send the command to.
	 *  @param command	The command to be executed.
	 *  @param clazz	The return type.
	 *  @param timeout	Custom timeout or null for default.
	 *  @return	The result(s) of the command, if any.
	 */
	public <T> IFuture<T> execute(ComponentIdentifier target, IRemoteCommand<T> command, Class<? extends IFuture<T>> clazz, Long timeout)
	{
		final String rxid = command.getId();
//		System.out.println(getComponent().getComponentIdentifier() + " sending remote command: "+command+", rxid="+rxid);

		// TODO: Merge with DecouplingInterceptor code.
		@SuppressWarnings("unchecked")
		final Future<T> ret	= (Future<T>) FutureFunctionality.getDelegationFuture(clazz, new FutureFunctionality()
		{
			@Override
			public boolean isUndone(boolean undone)
			{
				// Always undone when (potentially) timeout exception.
				return undone || timeout>=0;
			}
			
			@Override
			public void handleTerminated(Exception reason)
			{
				sendRxMessage(target, new RemoteTerminationCommand<T>(reason));
			}
			
			@Override
			public void handlePull()
			{
				sendRxMessage(target, new RemotePullCommand<T>());
			}
			
			@Override
			public void handleBackwardCommand(Object info)
			{
				sendRxMessage(target, new RemoteBackwardCommand<T>(info));
//				// ignore backward failures and wait for forward failure (i.e. timeout)  
//					.addResultListener(new IResultListener<Void>()
//				{
//					@Override
//					public void resultAvailable(Void result)
//					{
//						System.out.println(getComponent()+" sent successful backward command: "+info);
//					}
//					
//					@Override
//					public void exceptionOccurred(Exception exception)
//					{
//						System.out.println(getComponent()+" sending backward command failed: "+info+", "+exception);
//					}
//				});
			}
			
			// cleanup on finished:
			
			@Override
			public void handleFinished(Collection<Object> results) throws Exception
			{
//				System.out.println("Remove due to finished: "+target+", "+command);
				outcommands.remove(rxid);
			}
			
			@Override
			public Object handleResult(Object result) throws Exception
			{
//				System.out.println("Remove due to result: "+target+", "+command);
				outcommands.remove(rxid);
				return result;
			}
			
			@Override
			public void handleException(Exception exception)
			{
//				System.out.println("Remove due to exception: "+target+", "+command+", "+exception);
				outcommands.remove(rxid);
			}
		});

		// TODO: Implement timeout alternative
		/*if(ftimeout>=0)
		{
			IResultListener<T> trl	= new TimeoutIntermediateResultListener(ftimeout, getComponent().getExternalAccess(), Starter.isRealtimeTimeout(getComponent().getId(), true), command, null)
			{
				@Override
				public void timeoutOccurred(TimeoutException te)
				{
//					System.out.println(getComponent()+" remote timeout triggered: "+ftimeout+", "+command);
					ret.setExceptionIfUndone(te);
				}
				
//				@Override
//				protected synchronized void initTimer()
//				{
//					System.out.println(getComponent()+" (re)scheduling remote timeout: "+ftimeout+", "+command);
//					super.initTimer();
//				}
//				
//				@Override
//				public synchronized void cancel()
//				{
//					System.out.println(getComponent()+" cancelling remote timeout: "+ftimeout+", "+command);
//					super.cancel();
//				}
			};
			ret.addResultListener(trl);
		}
		*/
		
		if(outcommands==null)
		{
			outcommands	= new HashMap<String, OutCommand>();
		}
		OutCommand outcmd = new OutCommand(ret);
		outcommands.put(rxid, outcmd);
		
		sendRxMessage(target, command).addResultListener(new IResultListener<Void>()
		{
			public void exceptionOccurred(Exception exception)
			{
//				System.out.println("Remove due to exception2: "+target+", "+command+", "+exception);
				OutCommand outcmd = outcommands.remove(rxid);
				if (outcmd != null)
				{
					@SuppressWarnings("unchecked")
					Future<T> ret = (Future<T>) outcmd.getFuture();
					if (ret != null)
						ret.setExceptionIfUndone(exception);
				}
			}
			
			public void resultAvailable(Void result)
			{
			}
		});
		
		return ret;
	}
	
	/**
	 *  Invoke a method on a remote object.
	 *  @param ref	The target reference.
	 *  @param method	The method to be executed.
	 *  @param args	The arguments.
	 *  @return	The result(s) of the method invocation, if any. Connects any futures involved.
	 */
	public <T> IFuture<T> executeRemoteMethod(RemoteReference ref, Method method, Object[] args)
	{		ServiceCall invoc = ServiceCall.getNextInvocation();
		// TODO: Implement timeout alternative
		Map<String, Object>	nonfunc	= invoc!=null ? invoc.getProperties() : null;
		CallAccess.resetNextInvocation();
		
		@SuppressWarnings("unchecked")
		Class<IFuture<T>> clazz = (Class<IFuture<T>>)(IFuture.class.isAssignableFrom(method.getReturnType())
			? (Class<IFuture<T>>)method.getReturnType()
			: IFuture.class);
		
//		if(method.toString().toLowerCase().indexOf("getdesc")!=-1)
//			System.out.println("Executing requested remote method invocation: "+method);

		final String rxid = SUtil.createUniqueId();
		return execute(ref.getRemoteComponent(), new RemoteMethodInvocationCommand<T>(rxid, component.getId(), ref.getTargetIdentifier(), method, args, nonfunc), clazz, timeout);
	}

	/**
	 *  Sends RX message.
	 *  
	 *  @param receiver The receiver.
	 *  @param msg The message.
	 *  
	 *  @return Null, when sent.
	 */
	protected IFuture<Void> sendRxMessage(ComponentIdentifier receiver, final Object msg)
	{
		/*if(DEBUG)
			header.put(RX_DEBUG, msg!=null ? msg.toString() : null);*/
		
		IFuture<Void> ret = component.getFeature(IMessageFeature.class).sendMessage(msg, receiver);
//		ret.addResultListener(new IResultListener<Void>()
//		{
//			public void exceptionOccurred(Exception exception)
//			{
//				System.out.println("not sent: "+exception+" "+msg);
//			}
//			public void resultAvailable(Void result)
//			{
//				System.out.println("sent: "+msg);
//			}
//		});
		return ret;
	}
	
	/**
	 *  Handle RX Messages.
	 *  Also handles untrusted messages and does its own security checks.
	 *
	 */
	protected class RxHandler implements IMessageHandler
	{
		/**
		 *  Test if handler should handle a message.
		 *  @return True if it should handle the message. 
		 */
		public boolean isHandling(ISecurityInfo secinfos, Object msg)
		{
			return msg instanceof IIdSenderCommand;
		}
		
		/**
		 *  Test if handler should be removed.
		 *  @return True if it should be removed. 
		 */
		public boolean isRemove()
		{
			return false;
		}
		
		/**
		 *  Handle the message.
		 *  @param secinfos The security information.
		 *  @param msg The message.
		 */
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public void handleMessage(ISecurityInfo secinfos, Object msg)
		{
			final String rxid = ((IIdSenderCommand) msg).getId();
//			System.out.println(getComponent().getId() + " received remote command: "+msg+", rxid="+rxid);
			
			if(msg instanceof IRemoteCommand)
			{
				IRemoteCommand<?> cmd = (IRemoteCommand<?>)msg;
				final ComponentIdentifier remote = cmd.getSender();
				Exception validityex = ((IRemoteCommand) msg).isValid(component);
				if (validityex == null)
				{
					if(checkSecurity(secinfos, cmd))
					{
						ServiceCall	sc	= null;
						if(cmd instanceof AbstractInternalRemoteCommand)
						{
							// Creates a new ServiceCall for the current call and copies the values
							
							// Create new hashmap to prevent remote manipulation of the map object
							Map<String, Object>	nonfunc	= new HashMap<>(SUtil.notNull(((AbstractInternalRemoteCommand)cmd).getProperties()));
//							if(nonfunc==null)
//								nonfunc = new HashMap<String, Object>();
							nonfunc.put(ServiceCall.SECURITY_INFOS, secinfos);
							//TODO: REQUIRED IComponentIdentifier.LOCAL??
							//IComponentIdentifier.LOCAL.set((IComponentIdentifier)header.getProperty(IMsgHeader.SENDER));

							// Local is used to set the caller in the new service call context
							sc = ServiceCall.getOrCreateNextInvocation(nonfunc);
							// After call creation it can be reset
							//TODO: REQUIRED IComponentIdentifier.LOCAL??
							//IComponentIdentifier.LOCAL.set(getComponent().getId());
						}
						final ServiceCall fsc = sc;
						
						final IFuture<?> retfut = cmd.execute(component, secinfos);
						CallAccess.resetNextInvocation();
						if(incommands == null)
							incommands = new HashMap<String, IFuture<?>>();
						IFuture<?> prev	= incommands.put(rxid, retfut);
						assert prev==null;
						
						final IResultListener<Void>	term;
						if(retfut instanceof ITerminableFuture)
						{
							term = new IResultListener<Void>()
							{
								public void exceptionOccurred(Exception exception)
								{
									((ITerminableFuture)retfut).terminate(exception);
									incommands.remove(rxid);
								}
								
								public void resultAvailable(Void result)
								{
								}
							};
						}
						else
						{
							term	= null;
						}
						
						retfut.addResultListener(new IIntermediateFutureCommandResultListener()
						{
							/** Result counter. */
							int counter = Integer.MIN_VALUE;
							
							public void intermediateResultAvailable(Object result)
							{
								RemoteIntermediateResultCommand<?> rc = new RemoteIntermediateResultCommand(rxid, component.getId(), result, fsc!=null ? fsc.getProperties() : null);
								rc.setResultCount(counter++);
//								System.out.println("send RemoteIntermediateResultCommand to: "+remote);
								IFuture<Void> fut = sendRxMessage(remote, rc);
								if(term!=null)
								{
									fut.addResultListener(term);
								}
							}
							
							public void finished()
							{
								incommands.remove(rxid);
								RemoteFinishedCommand<?> rc = new RemoteFinishedCommand(rxid, component.getId(), fsc!=null ? fsc.getProperties() : null);
								rc.setResultCount(counter++);
								sendRxMessage(remote, rc);
							}
							
							public void resultAvailable(final Object result)
							{
//								getComponent().getLogger().severe("sending result: "+rxid+", "+result);
								incommands.remove(rxid);
								RemoteResultCommand<?> rc = new RemoteResultCommand(rxid, component.getId(), result, fsc!=null ? fsc.getProperties() : null);
								final int msgcounter = counter++;
								rc.setResultCount(msgcounter);
								sendRxMessage(remote, rc).addResultListener(new IResultListener<Void>()
								{
									@Override
									public void exceptionOccurred(Exception exception)
									{
										component.getLogger().log(System.Logger.Level.ERROR,"sending result failed: "+rxid+", "+result+", "+exception);
										// Serialization of result failed -> send back exception.
										RemoteResultCommand<?> rc = new RemoteResultCommand(rxid, component.getId(), exception, fsc!=null ? fsc.getProperties() : null);
										rc.setResultCount(msgcounter);
										sendRxMessage(remote, rc);
									}
									
									@Override
									public void resultAvailable(Void v)
									{
//										getComponent().getLogger().severe("sending result succeeded: "+rxid+", "+result);
										// OK -> ignore
									}
								});
							}
							
							public void exceptionOccurred(Exception exception)
							{
								incommands.remove(rxid);
								RemoteResultCommand<?> rc = new RemoteResultCommand(rxid, component.getId(), exception, fsc!=null ? fsc.getProperties() : null);
								rc.setResultCount(counter++);
								sendRxMessage(remote, rc);
							}
							
							public void commandAvailable(Object command)
							{
								RemoteForwardCmdCommand fc = new RemoteForwardCmdCommand(rxid, component.getId(), command);
								fc.setResultCount(counter++);
//								System.out.println(getComponent()+" sending forward command: "+remote+", "+msg+", "+command);
								IFuture<Void>	fut	= sendRxMessage(remote, fc);
//								fut.addResultListener(new IResultListener<Void>()
//								{
//									@Override
//									public void resultAvailable(Void result)
//									{
//										System.out.println(getComponent()+" successfully sent forward command: "+remote+", "+msg+", "+command);
//									}
//									
//									@Override
//									public void exceptionOccurred(Exception exception)
//									{
//										System.out.println(getComponent()+" sending forward command failed: "+remote+", "+msg+", "+command+", "+exception);
//									}
//								});

								if(term!=null)
								{
									fut.addResultListener(term);
								}
							}
							
							public void maxResultCountAvailable(int max) 
							{
							}
						});
					}
					else
					{
						// Not allowed -> send back exception.
						String errormsg = "Command "+msg.getClass()+" not allowed.";
						if (msg instanceof RemoteMethodInvocationCommand)
						{
							RemoteMethodInvocationCommand rmic = ((RemoteMethodInvocationCommand) msg);
							errormsg = "Method invocation command "+rmic.getMethod()+" not allowed.";
							
						}
						RemoteResultCommand<?> rc = new RemoteResultCommand(rxid, component.getId(), new SecurityException(errormsg), null);
						sendRxMessage(remote, rc);
					}
				}
				else
				{
					// Not allowed -> send back exception.
					RemoteResultCommand<?> rc = new RemoteResultCommand(rxid, component.getId(), validityex, null);
					sendRxMessage(remote, rc);
				}
			}
			else if(msg instanceof IRemoteConversationCommand || msg instanceof IRemoteOrderedConversationCommand)
			{
				if(checkSecurity(secinfos, (IIdSenderCommand) msg))
				{
					// Can be result/exception -> for outcommands
					// or termination -> for incommands.
					OutCommand outcmd = outcommands!=null ? outcommands.get(rxid) : null;
					IFuture<?> fut = outcmd != null ? outcmd.getFuture() : null;
					fut	= fut!=null ? fut : incommands!=null ? incommands.get(rxid) : null;
					
					if(fut!=null)
					{
						if(msg instanceof AbstractInternalRemoteCommand)
						{
							// Create new hashmap to prevent remote manipulation of the map object
							Map<String, Object>	nonfunc	= new HashMap(SUtil.notNull(((AbstractInternalRemoteCommand)msg).getProperties()));
							nonfunc.put(ServiceCall.SECURITY_INFOS, secinfos);
							ServiceCall sc = ServiceCall.getLastInvocation();
							if(sc==null)
							{
								ComponentIdentifier sender = ((IIdSenderCommand) msg).getSender();
								// TODO: why null?
								sc	= CallAccess.createServiceCall(sender, nonfunc);
								CallAccess.setLastInvocation(sc);
							}
							else
							{
								for(String name: nonfunc.keySet())
								{
									sc.setProperty(name, nonfunc.get(name));
								}
							}
						}
						
						if(msg instanceof IRemoteConversationCommand)
						{
							IRemoteConversationCommand<?> cmd = (IRemoteConversationCommand<?>)msg;
							cmd.execute(component, (IFuture)fut, secinfos);
						}
						else
						{
							IRemoteOrderedConversationCommand cmd = (IRemoteOrderedConversationCommand)msg;
							cmd.execute(component, outcmd, secinfos);
						}
					}
					else
					{
						component.getLogger().log(System.Logger.Level.WARNING,"Outdated remote command: "+msg);
					}
				}
				else
				{

					component.getLogger().log(System.Logger.Level.WARNING, "Command from "+((IIdSenderCommand) msg).getSender()+" not allowed: "+msg.getClass());
				}
			}
			/*else if(header.getProperty(MessageFeature.EXCEPTION)!=null) //TODO:
			{
				// Message could not be delivered -> remove the future (and abort, if possible)
				
				// locally executing command -> terminate, if terminable (i.e. abort to callee)
				if(incommands!=null && incommands.get(rxid) instanceof ITerminableFuture)
				{
					((ITerminableFuture)incommands.remove(rxid))
						.terminate((Exception)header.getProperty(MessageComponentFeature.EXCEPTION));
				}
				
				// Remotely executing command -> set local future to failed (i.e. abort to caller)
				else if(outcommands!=null && outcommands.get(rxid) instanceof OutCommand)
				{
//					System.out.println("Remove due to exception3: "+header.getProperty(MessageComponentFeature.EXCEPTION));
					((Future) ((OutCommand)outcommands.remove(rxid)).getFuture())
						.setException((Exception)header.getProperty(MessageComponentFeature.EXCEPTION));
				}
			}*/
			else
			{
				component.getLogger().log(System.Logger.Level.WARNING, "Invalid remote execution message: "+msg);
			}
		}

		/**
		 *  Check if it is ok to execute a command.
		 */
		protected boolean checkSecurity(ISecurityInfo secinfos, IIdSenderCommand msg)
		{
			boolean	trusted	= false;
			
			if (secinfos == null)
			{
				System.err.println("Remote execution command received without security infos (misrouted local message?): From " + msg.getSender() + " To: unknown");
				return false;
			}

			// Verify sender.
			if (!secinfos.getSender().equals(msg.getSender().getGlobalProcessIdentifier()))
				return false;
			
			// Admin platforms (i.e. in possession  of our platform key) can do anything.
			if(secinfos.getRoles().contains(Security.ADMIN))
			{
				trusted	= true;
			}
			
			// Internal command -> safe to check as stated by command.
			else if(SAFE_COMMANDS.contains(msg.getClass()))
			{
				if(msg instanceof ISecuredRemoteCommand)
				{
					Set<String>	secroles = ServiceIdentifier.getRoles(((ISecuredRemoteCommand)msg).getSecurityLevel(component), component);
					//System.out.println("secroles " + (secroles != null ? Arrays.toString(secroles.toArray()) : "null") + " " + secinfos);
					// No service roles and trusted role is ok.
					if (secroles == null && secinfos.getRoles().contains(Security.TRUSTED))
					{
						trusted = true;
					}
					
					// Custom role match is ok
					else if(!Collections.disjoint(secroles == null ? Collections.emptySet() : secroles, secinfos.getRoles()))
						trusted	= true;
					
					// Always allow 'unrestricted' access
					else if(secroles != null && secroles.contains(Security.UNRESTRICTED))
					{
						trusted	= true;
					}
				}
				else
				{
					// safe command without special security, e.g. intermediate result
					trusted	= true;
				}
			}
			
			if(!trusted)
			{
				component.getLogger().log(System.Logger.Level.INFO,"Untrusted command not executed: "+msg);
//				System.out.println("Untrusted command not executed: "+msg);
			}
//			else
//			{
//				System.out.println("Trusted command allowed: "+msg);
//			}
			
			return trusted;
		}
	}
	
	/** Command that has been sent to a remote component.
	 *  Stored to set return value etc. */
	protected static class OutCommand implements IOrderedConversation
	{
		/** Commands that have been deferred until a prior command arrives. */
		protected PriorityQueue<AbstractResultCommand> deferredcommands;
		
		protected int resultcount = Integer.MIN_VALUE;
		
		/** Future for results. */
		protected IFuture<?> future;
		
		/**
		 *  Creates the command.
		 */
		public OutCommand(IFuture<?> future)
		{
			this.future = future;
		}
		
		/** Gets the future. */
		public IFuture<?> getFuture()
		{
			return future;
		}
		
		/**
		 *  Gets the count of the next result.
		 *  
		 *  @return The count of the next result.
		 */
		public int getNextResultCount()
		{
			return resultcount;
		}
		
		/**
		 *  Increases the next result count. 
		 */
		public void incNextResultCount()
		{
			++resultcount;
		}
		
		/**
		 *  Returns queue of commands that have been deferred due to
		 *  out-of-order arrival.
		 *  
		 *  @return Queue of commands, may be null.
		 */
		public PriorityQueue<AbstractResultCommand> getDeferredCommands()
		{
			if (deferredcommands == null)
			{
				deferredcommands = new PriorityQueue<AbstractResultCommand>(11, new Comparator<AbstractResultCommand>()
				{
					public int compare(AbstractResultCommand o1, AbstractResultCommand o2)
					{
						if (o1.getResultCount() == null)
							return 1;
						if (o2.getResultCount() == null)
							return -1;
						return o1.getResultCount() - o2.getResultCount();
					}
				});
			}
				
			
			return deferredcommands;
		}
	}
}
