package jadex.bt.impl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import jadex.bt.IBTAgentFeature;
import jadex.bt.IBTProvider;
import jadex.common.SReflect;
import jadex.core.Application;
import jadex.core.ChangeEvent;
import jadex.core.ChangeEvent.Type;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.injection.impl.InjectionFeatureProvider;
import jadex.injection.impl.InjectionModel;
import jadex.rules.eca.ChangeInfo;
import jadex.rules.eca.Event;
import jadex.rules.eca.EventType;
import jadex.rules.eca.RuleSystem;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.LambdaInstrumentationStrategy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

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
	public Map<String, Object> getResults(IComponent comp)
	{
		// Hack!? delegate result handling to injection feature.
		return new InjectionFeatureProvider().getResults(comp);
	}
	
	@Override
	public ISubscriptionIntermediateFuture<ChangeEvent> subscribeToResults(IComponent comp)
	{
		// Hack!? delegate result handling to injection feature.
		return new InjectionFeatureProvider().subscribeToResults(comp);
	}
	
	@Override
	public void init()
	{
//		// Todo: make this configurable?
//		installLambdaMapper();
		
		InjectionModel.addExtraCode(model ->
		{
			if(isCreator(model.getPojoClazz())>0)
			{
				// Consider all fields as potentially dynamic values
				model.addDynamicValues(null, false);
				model.addChangeHandler(null, (comp, event) ->
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

	/** Mapping from runtime (i.e. proxy class) name to bytecode (i.e. static method) name. */
	public static Map<String, String>	NAMES	= Collections.synchronizedMap(new LinkedHashMap<>());
	
	/** Get the ASM descpriptor of the code of a lambda object. */
	public static String	getASMMethodDescFromLambda(Object lambda)
	{
		String	clazzname	= lambda.getClass().getName();
		// Strip ID extension
		clazzname	= clazzname.substring(0, clazzname.indexOf('/'));
		return NAMES.get(clazzname);
	}

	/**
	 *  Install a class loader interceptor
	 *  to generate a mapping from runtime (i.e. proxy class) name
	 *  to bytecode (i.e. static method) name.
	 *  The static method name is required to find dependencies (dynamic values),
	 *  but at runtime only the proxy class name is available.
	 */
	public static void installLambdaMapper()
	{
		new AgentBuilder.Default()
			.with(LambdaInstrumentationStrategy.ENABLED)
			.type(ElementMatchers.nameContains("$$Lambda$"))
			.and(ElementMatchers.not(ElementMatchers.nameContains("org.lwjgl.")))
			.transform((builder, typeDescription, classLoader, module, protectionDomain) ->
			{
//				System.out.println("Transform: "+typeDescription.getName());
				return builder;
			})
			.with(new AgentBuilder.Listener.Adapter()
			{
				@Override
				public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module,
						boolean loaded, DynamicType dynamicType)
				{
//					System.out.println("======== "+typeDescription);
					ClassReader	cr	= new ClassReader(dynamicType.getBytes());
					cr.accept(new ClassVisitor(Opcodes.ASM9)
					{
						@Override
						public MethodVisitor visitMethod(int access, String methodname, String desc, String signature, String[] exceptions)
						{
//							System.out.println("=== "+methodname);
						    return new MethodVisitor(Opcodes.ASM9)
						    {
								public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface)
								{
									// Skip all method calls that are not the lambda target
									if(!methodname.equals("get$Lambda") && !methodname.startsWith("<"))
									{
//										System.out.println("runtime: "+typeDescription.getName());
//										System.out.println("name: "+owner+"."+name+descriptor);
										String runtime_name	= typeDescription.getName();
										owner	= owner.replace('/', '.');
										// Might not be same owner, e.g. when using my_obj::myMethod notation
//										if(owner.equals(runtime_name.substring(0, runtime_name.indexOf("$$Lambda$"))))
										{
								        	NAMES.put(runtime_name, owner+"."+name+descriptor);
										}
									}
								}
						    };
						}
					}, 0);
				}
			})
			.installOn(ByteBuddyAgent.install());
	}
}
