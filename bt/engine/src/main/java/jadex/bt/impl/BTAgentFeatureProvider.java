package jadex.bt.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Method;

import jadex.bt.IBTAgentFeature;
import jadex.bt.IBTProvider;
import jadex.bt.decorators.ConditionalDecorator;
import jadex.bt.nodes.Node;
import jadex.common.ITriFunction;
import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.core.Application;
import jadex.core.ChangeEvent.Type;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponentHandle;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.future.IFuture;
import jadex.injection.impl.InjectionModel;
import jadex.rules.eca.ChangeInfo;
import jadex.rules.eca.Event;
import jadex.rules.eca.EventType;
import jadex.rules.eca.RuleSystem;

public class BTAgentFeatureProvider extends ComponentFeatureProvider<IBTAgentFeature> implements IComponentLifecycleManager
{
	@Override
	public Class< ? extends Component> getRequiredComponentType()
	{
		return BTAgent.class;
	}
	
	@Override
	public Class<IBTAgentFeature> getFeatureType()
	{
		return IBTAgentFeature.class;
	}

	@Override
	public IBTAgentFeature createFeatureInstance(Component self)
	{
		return new BTAgentFeature((BTAgent)self);
	}
	
	
	@Override
	public int	isCreator(Class<?> pojoclazz)
	{
		boolean ret = SReflect.isSupertype(IBTProvider.class, pojoclazz);
		// TODO: generic @Component annotation?
//		if(!ret)
//		{
//			Agent val = MicroAgentFeatureProvider.findAnnotation(pojoclazz, Agent.class, getClass().getClassLoader());
//			if(val!=null)
//				ret = "bt".equals(val.type());
//		}
		return ret ? 1 : -1;
	}
	
	@Override
	public IFuture<IComponentHandle> create(Object pojo, ComponentIdentifier cid, Application app)
	{
		return BTAgent.create(pojo, cid, app);
	}
//	/**
//	 *  Get the predecessors, i.e. features that should be inited first.
//	 *  @return The predecessors.
//	 */
//	public Set<Class<?>> getPredecessors(Set<Class<?>> all)
//	{
//		all.remove(getFeatureType());
//		return all;
//	}
	
	@Override
	public void init()
	{
//		// Todo: make this configurable?
//		installLambdaMapper();
		
		InjectionModel.addExtraCode(model ->
		{
			if(isCreator(model.getPojoClazz())>0)
			{
				scanClazz(model.getPojoClazz(), model.getPojoClazz());
				
				// Consider all fields as potentially dynamic values
				model.addDynamicValues(null, false);
				model.addChangeHandler(null, (comp, event, annos) ->
				{
					String	typename	=
						event.type()==Type.ADDED ? 		BTAgentFeature.VALUEADDED :
						event.type()==Type.REMOVED ?	BTAgentFeature.VALUEREMOVED :
					/*	event.type()==Type.CHANGED ?*/	BTAgentFeature.PROPERTYCHANGED ;
						
					EventType	type	= new EventType(typename, event.name());
					Event	ev	= new Event(type, new ChangeInfo<Object>(event.value(), event.oldvalue(), event.info()));
					RuleSystem	rs	= ((BTAgentFeature) comp.getFeature(IBTAgentFeature.class)).getRuleSystem();
					rs.addEvent(ev);
				});
				
				model.addPostInject((self, pojos, context, oldval) ->
				{
					((BTAgentFeature)self.getFeature(IBTAgentFeature.class)).executeBehaviorTree(null, null);
					return null;
				});
			}
		});		
	}
	
	//-------- manual lambda mapping --------
	
	/** pojoclazz -> (node_name -> list of asm method desc (or null if no condition for a decorator)). */
	public static final Map<Class<?>, Map<String, List<String>>>	DECORATOR_CONDITIONS	= new LinkedHashMap<>();
	
	/** Classes already scanned for baseclazz. */
	Map<Class<?>, Set<Class<?>>>	SCANNED_CLASSES	= new LinkedHashMap<>();
	
	/**
	 *  Scan the scanclazz for actions and their decorators.
	 *  Add found descriptions to baseclazz.
	 */
	protected void	scanClazz(Class<?> baseclazz, Class<?> scanclazz)
	{
		synchronized(SCANNED_CLASSES)
		{
			Set<Class<?>>	scanned	= SCANNED_CLASSES.get(baseclazz);
			if(scanned==null)
			{
				scanned	= new LinkedHashSet<>();
				SCANNED_CLASSES.put(baseclazz, scanned);
			}
			else if(scanned.contains(scanclazz))
			{
				return;
			}
			scanned.add(scanclazz);
			
			// Skip java.lang for e.g. inner enums
			if(scanclazz.getSuperclass()!=null && !"java.lang".equals(scanclazz.getSuperclass().getPackageName()))
			{
				// Scan superclass first.
				scanClazz(baseclazz, scanclazz.getSuperclass());
			}
			
			Map<String, List<String>>	decorator_conditions;
			synchronized(DECORATOR_CONDITIONS)
			{
				decorator_conditions	= DECORATOR_CONDITIONS.get(baseclazz);
				if(decorator_conditions==null)
				{
					decorator_conditions	= new LinkedHashMap<>();
					DECORATOR_CONDITIONS.put(baseclazz, decorator_conditions);
				}
			}
			Map<String, List<String>>	fdecorator_conditions	= decorator_conditions;
			
			try
			{
				ClassReader	cr	= new ClassReader(scanclazz.getName());
				cr.accept(new ClassVisitor(Opcodes.ASM9)
				{
					/** Inside action node creation? -> remember next string as name. */
					boolean	node_creation	= false;
					
					/** Latest node name -> assume all decorators belong to this. */
					String	node_name;
					
					/** Latest lambda or ITriFunction (asm desc) -> might be for condition or function or else. */
					String	lambda;
					
					/** Latest ITriFunction (asm desc), which was (probably) used in setCondition(). */
					String	condition;
					
					@Override
					public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions)
					{
						return new MethodVisitor(Opcodes.ASM9)
						{
							// Check for node creation
							@Override
							public void visitTypeInsn(int opcode, String type)
							{
								try
								{
//									System.out.println("Type: "+type);
									type	= type.replace('/', '.');
									Class<?> ctype	= Class.forName(type);
									if(SReflect.isSupertype(Node.class, ctype))
									{
//										System.out.println("Node: "+ctype);
										node_creation	= true;
									}
									else
									{
										node_creation	= false;
									}
									
									super.visitTypeInsn(opcode, type);
								}
								catch(Exception e)
								{
									throw SUtil.throwUnchecked(e);
								}
							}
							
							// Check for name of node
							@Override
							public void visitLdcInsn(Object value)
							{
								if(node_creation && value instanceof String)
								{
//									System.out.println("Node name: "+value);
									node_name	= (String) value;
								}
								super.visitLdcInsn(value);
							}
							
							// Check for lambda definition
							@Override
							public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments)
							{
								if(bootstrapMethodArguments.length>=2 && (bootstrapMethodArguments[1] instanceof Handle))
								{
									Handle handle	= (Handle)bootstrapMethodArguments[1];
									String	callee	= handle.getOwner()+"."+handle.getName()+handle.getDesc();
//									System.out.println("Visiting lambda call: "+callee);
									lambda	= callee;
								}
								// else Do we need to handle other cases?
							}
							
							@Override
							public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface)
							{
								try
								{
//									String	callee	= owner+"."+name+descriptor;
									Class<?> calleeclazz = Class.forName(owner.replace('/', '.'));
	
									// Remember apply() from ITriFunction object for next setCondition().
									if("<init>".equals(name) && SReflect.isSupertype(ITriFunction.class, calleeclazz))
									{
//										System.out.println("ITriFunction constructor: "+calleeclazz);
										// TODO: parameter types correct!?
										lambda	= InjectionModel.executableToAsmDesc(calleeclazz.getMethod("apply", Object.class, Object.class, Object.class));
									}
									
									// Remember last lambda as condition
									else if(("setCondition".equals(name) || "setAsyncCondition".equals(name)) && SReflect.isSupertype(ConditionalDecorator.class, calleeclazz))
									{
//										System.out.println("setCondition(): "+calleeclazz+", "+lambda);
										condition	= lambda;
									}
									
									// Finally, when added as decorator -> insert into list
									else if("addDecorator".equals(name) && SReflect.isSupertype(Node.class, calleeclazz))
									{
//										System.out.println("addDecorator(): "+node_name+", "+condition);
										synchronized(DECORATOR_CONDITIONS)
										{
											List<String>	decorators	= fdecorator_conditions.get(node_name);
											if(decorators==null)
											{
												decorators	= new ArrayList<>();
												fdecorator_conditions.put(node_name, decorators);
											}
											decorators.add(condition);
											lambda	= null;
											condition	= null;
										}
									}
									
									// When calling external method to create nodes -> scan also class that contains method.
									else
									{
										Method	m	= new Method(name, descriptor);
										if(m.getReturnType().getSort()==org.objectweb.asm.Type.OBJECT)
										{
											Class<?>	returntype	= Class.forName(m.getReturnType().getClassName());
											if(SReflect.isSupertype(Node.class, returntype))
											{
//												System.out.println("method: "+owner+"."+m);
												scanClazz(baseclazz, calleeclazz);
											}
										}
									}
									
									super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
								}
								catch(Exception e)
								{
									SUtil.throwUnchecked(e);
								}							
							}
						};
					}
				}, 0);
			}
			catch(Exception e)
			{
				SUtil.throwUnchecked(e);
			}
		}
	}
	
	//-------- instrumentation-based lambda mapping (non-working due to ByteBuggy ;-P) --------
	// cf. https://github.com/raphw/byte-buddy/issues/1869

//	/** Mapping from runtime (i.e. proxy class) name to bytecode (i.e. static method) name. */
//	public static Map<String, String>	NAMES	= Collections.synchronizedMap(new LinkedHashMap<>());
//	
//	/** Get the ASM descpriptor of the code of a lambda object. */
//	public static String	getASMMethodDescFromLambda(Object lambda)
//	{
//		String	clazzname	= lambda.getClass().getName();
//		// Strip ID extension
//		clazzname	= clazzname.substring(0, clazzname.indexOf('/'));
//		return NAMES.get(clazzname);
//	}
//
//	/**
//	 *  Install a class loader interceptor
//	 *  to generate a mapping from runtime (i.e. proxy class) name
//	 *  to bytecode (i.e. static method) name.
//	 *  The static method name is required to find dependencies (dynamic values),
//	 *  but at runtime only the proxy class name is available.
//	 */
//	public static void installLambdaMapper()
//	{
//		new AgentBuilder.Default()
//			.with(LambdaInstrumentationStrategy.ENABLED)
//			.type(ElementMatchers.nameContains("$$Lambda$"))
//			.and(ElementMatchers.not(ElementMatchers.nameContains("org.lwjgl.")))
//			.transform((builder, typeDescription, classLoader, module, protectionDomain) ->
//			{
////				System.out.println("Transform: "+typeDescription.getName());
//				return builder;
//			})
//			.with(new AgentBuilder.Listener.Adapter()
//			{
//				@Override
//				public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module,
//						boolean loaded, DynamicType dynamicType)
//				{
////					System.out.println("======== "+typeDescription);
//					ClassReader	cr	= new ClassReader(dynamicType.getBytes());
//					cr.accept(new ClassVisitor(Opcodes.ASM9)
//					{
//						@Override
//						public MethodVisitor visitMethod(int access, String methodname, String desc, String signature, String[] exceptions)
//						{
////							System.out.println("=== "+methodname);
//						    return new MethodVisitor(Opcodes.ASM9)
//						    {
//								public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface)
//								{
//									// Skip all method calls that are not the lambda target
//									if(!methodname.equals("get$Lambda") && !methodname.startsWith("<"))
//									{
////										System.out.println("runtime: "+typeDescription.getName());
////										System.out.println("name: "+owner+"."+name+descriptor);
//										String runtime_name	= typeDescription.getName();
//										owner	= owner.replace('/', '.');
//										// Might not be same owner, e.g. when using my_obj::myMethod notation
////										if(owner.equals(runtime_name.substring(0, runtime_name.indexOf("$$Lambda$"))))
//										{
//								        	NAMES.put(runtime_name, owner+"."+name+descriptor);
//										}
//									}
//								}
//						    };
//						}
//					}, 0);
//				}
//			})
//			.installOn(ByteBuddyAgent.install());
//	}
}
