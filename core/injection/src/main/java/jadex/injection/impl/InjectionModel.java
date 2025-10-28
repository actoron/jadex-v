package jadex.injection.impl;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.common.Tuple2;
import jadex.core.IComponent;
import jadex.execution.impl.ExecutionFeatureProvider;
import jadex.injection.AbstractDynVal.ObservationMode;
import jadex.injection.Dyn;
import jadex.injection.IInjectionFeature;
import jadex.injection.Val;
import jadex.injection.annotation.OnEnd;
import jadex.injection.annotation.OnStart;
import jadex.injection.annotation.ProvideResult;

/**
 *  The injection model caches all injection/invocation-related stuff.
 */
public class InjectionModel
{
	/**
	 *  Meta information for a dynamic value field.
	 *  
	 *  @param field	The field.
	 *  @param name	The fully qualified name.
	 *  @param type	The value type (e.g. inner generic type of List).
	 *  @param kinds	The detected dynamic value annotations for the field.
	 */
	public static record MDynVal(Field field, String name, Class<?> type, Set<Class<? extends Annotation>> kinds) {}
	
	/** The pojo classes as a hierarchy of component pojo plus subobjects, if any.
	 *  The model is for the last pojo in the list. */
	protected final List<Class<?>>	pojoclazzes;
	
	/** Optional path name(s) if this model is a named subobject (e.g. capability). */
	protected final List<String>	path;
	
	/** The context specific value fetchers. */
	protected final Map<Class<? extends Annotation>,List<IValueFetcherCreator>>	contextfetchers;
	
	/** Flag to prevent changing the model after creation. */
	protected boolean	inited	= false;
	
	/** Code to run on component start before field injections. */
	protected final List<IInjectionHandle>	preinject	= new ArrayList<>();
	
	/** Field injections on component start. */
	protected final List<IInjectionHandle>	fields	= new ArrayList<>();
	
	/** Code to run on component start after field injections. */
	protected final List<IInjectionHandle>	postinject	= new ArrayList<>();
	
	/** User code to run on component start. */
	protected List<IInjectionHandle>	onstart;
	
	/** Method injections to be initialized after component start. */
	protected final List<IInjectionHandle>	methods	= new ArrayList<>();
	
	/** User code to run on component end. */
	protected List<IInjectionHandle>	onend;
	
	/** Code to fetch component results. */
	protected IInjectionHandle	results;
	
	/** Dynamic values (field -> dynval info). */
	protected final Map<Field, MDynVal>	dynvals_by_field	= new LinkedHashMap<>();
	
	/** Dynamic values (name -> dynval info). */
	protected final Map<String, MDynVal>	dynvals_by_name	= new LinkedHashMap<>();
	
	/** Generic engine specific handlers for dynamic value kinds (anno or null -> list(handler)). */
	protected Map<Class<? extends Annotation>, List<IChangeHandler>>	dynvalhandlers	= new LinkedHashMap<>();
	

	
	//-------- init --------
	
	/**
	 *  Create injection model for given stack of pojo classes.
	 */
	protected InjectionModel(List<Class<?>> classes, List<String> path, Map<Class<? extends Annotation>,List<IValueFetcherCreator>> contextfetchers)
	{
		this.pojoclazzes	= classes;
		this.path	= path;
		this.contextfetchers	= contextfetchers;
	}
	
	/**
	 *  Init the model after creation.
	 */
	protected void init()
	{
		// Scan class for resolving dynamic value dependencies.
		scanClass(pojoclazzes.get(pojoclazzes.size()-1));
		
		// Initialize fetchers and injections.
		for(IExtraCodeCreator extra: EXTRA_CODE_CREATORS)
		{
			extra.addExtraCode(this);
		}
		initFieldInjections();
		initMethodInjections();
		onstart	= createMethodInvocations(OnStart.class);
		onend	= createMethodInvocations(OnEnd.class);
		results	= createResultsFetcher();
		
		// make model read-only
		inited	= true;
	}
	
	/**
	 *  Init the field injection handles.
	 */
	protected void	initFieldInjections()
	{
		assert !inited;
		
		Map<Field, IInjectionHandle>	allfields	= new LinkedHashMap<>();
		
		// Search in global fetchers.
		synchronized(FETCHERS)
		{
			addValueFetchers(allfields, FETCHERS);
		}
		
		// Search in local fetchers.
		if(contextfetchers!=null)
		{
			addValueFetchers(allfields, contextfetchers);
		}
		
		for(Field f: allfields.keySet())
		{
			if(allfields.get(f)==null)
			{
				throw new UnsupportedOperationException("Cannot inject "+f);
			}
			else
			{
				fields.add(allfields.get(f));
			}
		}
	}
	
	/**
	 *  Add method injections to run after component start.
	 */
	protected void	initMethodInjections()
	{
		assert !inited;
		synchronized(METHOD_INJECTIONS)
		{
			for(Class<? extends Annotation> anno: METHOD_INJECTIONS.keySet())
			{
				for(Method method: InjectionModel.findMethods(pojoclazzes.get(pojoclazzes.size()-1), anno))
				{
					IInjectionHandle injection	= null;
					for(IMethodInjectionCreator check: METHOD_INJECTIONS.get(anno))
					{
						IInjectionHandle	test	= check.getInjectionHandle(this, method, method.getAnnotation(anno));
						if(test!=null)
						{
							if(injection!=null)
							{
								throw new RuntimeException("Conflicting method injections: "+injection+", "+test);
							}
							injection	= test;
						}
					}
					
					if(injection!=null)
					{
						methods.add(injection);	
					}
					else
					{
						throw new UnsupportedOperationException("Cannot inject "+method);
					}
				}
			}
		}
	}
	
	/**
	 *  Get the fetcher to retrieve current component results.
	 */
	protected IInjectionHandle	createResultsFetcher()
	{
		assert !inited;
		IInjectionHandle	results	= null;
		
		List<Getter>	fetchers = getGetters(ProvideResult.class);
		
		if(fetchers!=null)
		{
			// Find names for getters
			Map<String, Tuple2<IInjectionHandle, Annotation[]>>	ffetchers	= new LinkedHashMap<>();
			for(Getter fetcher: fetchers)
			{
				String name	= fetcher.annotation() instanceof ProvideResult && ! "".equals(((ProvideResult)fetcher.annotation()).value())
					? ((ProvideResult)fetcher.annotation()).value()
					: fetcher.member() instanceof Method && fetcher.member().getName().startsWith("get")
						? fetcher.member().getName().substring(3).toLowerCase()
						: fetcher.member().getName();
				Annotation[]	annos	= fetcher.member() instanceof Method
					? (Annotation[])SUtil.joinArrays(((Method)fetcher.member()).getAnnotations(),
						((Method)fetcher.member()).getAnnotatedReturnType().getAnnotations())
					: ((Field)fetcher.member()).getAnnotations();
				ffetchers.put(name, new Tuple2<>(fetcher.fetcher(), annos));
			}
			
			// New handle to apply all getters
			results	= (comp, pojos, context, oldval) ->
			{
				Map<String, Object>	ret	= new LinkedHashMap<>();
				for(String name: ffetchers.keySet())
				{
					ret.put(name, ExecutionFeatureProvider.copyVal(ffetchers.get(name).getFirstEntity().apply(comp, pojos, context, null), ffetchers.get(name).getSecondEntity()));
				}
				return ret;
			};
		}
		return results;
	}
	
	/**
	 *  Add value fetchers for fields.
	 */
	protected void addValueFetchers(Map<Field, IInjectionHandle> allfields,
			Map<Class<? extends Annotation>, List<IValueFetcherCreator>> fetchers)
	{
		for(Class<? extends Annotation> anno: fetchers.keySet())
		{
			List<Field> fields = findFields(anno);
			
			for(Field field: fields)
			{
				// TODO: do we want to check this?
//				if(allfields.containsKey(field))
//				{
//					throw new RuntimeException("Multiple Inject annotations on field: "+field);
//				}
				if(!allfields.containsKey(field))
				{
					allfields.put(field, null);
				}
				
				try
				{
					IInjectionHandle	fetcher	= null;
					for(IValueFetcherCreator check: fetchers.get(anno))
					{
						IInjectionHandle	test	= check.getValueFetcher(pojoclazzes, field.getGenericType(), field.getAnnotation(anno));
						if(test!=null)
						{
							if(fetcher!=null && fetcher!=test)	// TODO: why duplicate of fetcher in bdi marsworld
							{
								throw new RuntimeException("Conflicting field injections: "+field+", "+fetcher.getClass()+", "+test.getClass());
							}
							fetcher	= test;
						}
					}
							
					if(fetcher!=null)
					{
						if(allfields.get(field)!=null)
						{
							throw new UnsupportedOperationException("Multiple value fetchers for field: "+field);
						}
						
						field.setAccessible(true);
						MethodHandle	getter	= MethodHandles.lookup().unreflectGetter(field);
						MethodHandle	setter	= MethodHandles.lookup().unreflectSetter(field);
						final IInjectionHandle	ffetcher	= fetcher;
						allfields.put(field, (self, pojos, context, oldval) -> {
							try
							{
								oldval	= getter.invoke(pojos.get(pojos.size()-1));
								Object[]	args	= new Object[]{pojos.get(pojos.size()-1), ffetcher.apply(self, pojos, context, oldval)};
								return setter.invokeWithArguments(args);
							}
							catch(Throwable e)
							{
								// Rethrow user exception
								throw SUtil.throwUnchecked(e);
							}
						});			
					}
				}
				catch(Exception e)
				{
					SUtil.throwUnchecked(e);
				}
			}
		}
	}
	
	/**
	 *  Get all invocation handles for methods with given annotation.
	 */
	protected List<IInjectionHandle>	createMethodInvocations(Class<? extends Annotation> annotation)
	{
		List<IInjectionHandle>	handles	= new ArrayList<>();
		
		List<Method> methods = findMethods(pojoclazzes.get(pojoclazzes.size()-1), annotation);
		for(Method method: methods)
		{
			handles.add(createMethodInvocation(method, pojoclazzes, contextfetchers, null));
		}
		
		return handles;
	}
	
	//-------- internal getters for injection feature at runtime --------
	
	/**
	 *  Get the field injections to run on component start.
	 */
	protected List<IInjectionHandle> getFieldInjections()
	{
		assert inited;
		return fields;
	}
	
	/**
	 *  Get the method injections to run after component start.
	 */
	protected List<IInjectionHandle> getMethodInjections()
	{
		assert inited;
		return methods;
	}
	
	/**
	 *  Get the code to run on component start.
	 */
	protected List<IInjectionHandle> getOnStart()
	{
		assert inited;
		return onstart;
	}

	/**
	 *  Get external code to run before field injections.
	 */
	protected List<IInjectionHandle> getPreInject()
	{
		assert inited;
		return preinject;
	}

	/**
	 *  Get external code to run after field injections.
	 */
	protected List<IInjectionHandle> getPostInject()
	{
		assert inited;
		return postinject;
	}

	/**
	 *  Get the code to run on component end.
	 */
	protected List<IInjectionHandle> getOnEnd()
	{
		assert inited;
		return onend;
	}
	
	/**
	 *  Get the code to fetch component results.
	 */
	protected IInjectionHandle getResultsFetcher()
	{
		assert inited;
		return results;
	}

	//-------- getters/setters/helpers for use during model init --------
	
	/**
	 *  Get the pojo classes as a hierarchy of component pojo plus subobjects, if any.
	 */
	public List<Class<?>> getPojoClazzes()
	{
		return pojoclazzes;
	}
	
	/**
	 *  Get the innermost pojo class, i.e. the class reresented by this model.
	 */
	public Class<?> getPojoClazz()
	{
		return pojoclazzes.get(pojoclazzes.size()-1);
	}
	
	/**
	 *  Get the path name(s) if this model is a named subobject (e.g. capability).
	 */
	public List<String> getPath()
	{
		return path;
	}
	
	/**
	 *  Get the context specific value fetchers.
	 */
	public Map<Class<? extends Annotation>,List<IValueFetcherCreator>> getContextFetchers()
	{
		return contextfetchers;
	}
	
	/**
	 *  Add pre-field-injection code.
	 */
	public void	addPreInject(IInjectionHandle handle)
	{
		assert !inited;
		preinject.add(handle);
	}

	/**
	 *  Add post-field-injection code.
	 */
	public void	addPostInject(IInjectionHandle handle)
	{
		assert !inited;
		postinject.add(handle);
	}

	/**
	 *  Add init codes for dynamic values, if any.
	 *  @param anno	The annotation to look for on a field or null for checking all fields.
	 *  @param required	If true, all fields with the annotation must be a supported dynamic value type.
	 */
	public void	addDynamicValues(Class<? extends Annotation> anno, boolean required)
	{
		assert !inited;
		String	prefix	= path==null ? "" : path.stream().map(entry -> entry+"." ).reduce("", (a,b) -> a+b);
		
		List<Field>	fields	= anno!=null ? findFields(anno)
			: Arrays.asList(SReflect.getAllFields(getPojoClazz()));
		
		// First add all fields to root model to handle cross-dependencies.
		for(Field f: fields)
		{
			if(isDynamicValue(f.getType()))
			{
				getRootModel().addDynamicField(f, prefix+f.getName(), anno);
			}
			else if(required)
			{
				throw new UnsupportedOperationException("Field is not a supported dynamic value type (Dyn, Val, List, Set, Map, Bean): "+f);
			}
		}
		
		// Now create inits for all fields.
		for(Field f: fields)
		{
			// Only add init code on first call, e.g. when field is marked as belief and result at the same time.
			MDynVal	mdynval	= getRootModel().getDynamicValue(f);
			if(isDynamicValue(f.getType()) && mdynval.kinds().size()==1)
			{
				IInjectionHandle	init	= createDynamicValueInit(mdynval);
				if(init!=null)
				{
					// Add init after field injections as e.g. Dyn value may access injected field.
					postinject.add(init);
				}
			}
		}
	}
	
	
	/**
	 *  Set the handler for a kind of dynamic value (e.g. result or belief).
	 *  @param kind	The annotation (or null for all supported fields) to mark a dynamic value as a specific kind.
	 *  @param handler The change handler.
	 */
	public void	addChangeHandler(Class<? extends Annotation> kind, IChangeHandler handler)
	{
		if(this!=getRootModel())
		{
			throw new UnsupportedOperationException("Change handlers must be added to root model.");
		}
		
		List<IChangeHandler>	handlers	= dynvalhandlers.get(kind);
		if(handlers==null)
		{
			handlers	= new ArrayList<>();
			dynvalhandlers.put(kind, handlers);
		}
		handlers.add(handler);
	}
	
	/**
	 *  Get the handler for a kind of dynamic value.
	 */
	protected List<IChangeHandler>	getChangeHandlers(Class<? extends Annotation> kind)
	{
		return dynvalhandlers.get(kind);
	}
	
	/**
	 *  Get the root model for the component pojo.
	 */
	public InjectionModel	getRootModel()
	{
		if(pojoclazzes.size()>1)
		{
			return getStatic(pojoclazzes.subList(0, 1), null, null);
		}
		else
		{
			return this;
		}
	}
	
	/**
	 *  Add a dynamic value field to the model.
	 */
	protected void	addDynamicField(Field field, String name, Class<? extends Annotation> kind)
	{
		// Called after init, e.g. when provided service impl pojo is added during on start 
//		assert !inited;
		
		MDynVal	mdynval	= dynvals_by_field.get(field);
		if(mdynval==null)
		{
			Class<?>	type	= getValueType(field);
			mdynval	= new MDynVal(field, name, type, new LinkedHashSet<>());
			dynvals_by_field.put(field, mdynval);
			dynvals_by_name.put(name, mdynval);
		}
		
		mdynval.kinds().add(kind);
	}
	
	/**
	 *  Get the dynamic values.
	 */
	public Collection<MDynVal>	getDynamicValues()
	{
		return dynvals_by_name.values();
	}
	
	/**
	 *  Get the meta info of a dynamic value field, if any.
	 */
	public MDynVal	getDynamicValue(Field f)
	{
		return dynvals_by_field.get(f);
	}
	
	/**
	 *  Get the meta info of a dynamic value field, if any.
	 */
	public MDynVal	getDynamicValue(String name)
	{
		return dynvals_by_name.get(name);
	}
	
	/**
	 *  Helper method to find all direct and inherited fields with the given annotation.
	 *  Fields are returned in order: subclass first, then superclass.
	 */
	public List<Field> findFields(Class<? extends Annotation> annotation)
	{
		List<Field>	allfields	= new ArrayList<>();
		Class<?> myclazz	= getPojoClazz();
		while(myclazz!=null)
		{
			for(Field field: myclazz.getDeclaredFields())
			{
				if(field.isAnnotationPresent(annotation))
				{
					if((field.getModifiers() & Modifier.STATIC)!=0)
					{
						throw new RuntimeException("Fields with @"+annotation.getSimpleName()+" must not be static: "+field);
					}
					allfields.add(field);
				}
			}
			myclazz	= myclazz.getSuperclass();
		}
		return allfields;
	}
	
	/**
	 *  Generic object to get a value from a field or method.
	 */
	public record Getter(Member member, Annotation annotation, IInjectionHandle fetcher) {}
	
	/**
	 *  Get value fetchers, that fetch the value of an annotated field.
	 *  The fetchers provide the result as FieldValue record.
	 */
	public List<Getter>	getGetters(Class<? extends Annotation> anno)
	{
		List<Getter>	fetchers	= null;
		for(Field field: findFields(anno))
		{
			if(fetchers==null)
			{
				fetchers	= new ArrayList<>(4);
			}
			try
			{
				field.setAccessible(true);
				MethodHandle	get	= MethodHandles.lookup().unreflectGetter(field);
				fetchers.add(new Getter(field, field.getAnnotation(anno), (comp, pojos, context, oldval) ->
				{
					try
					{
						return get.invoke(pojos.get(pojos.size()-1));
					}
					catch(Throwable t)
					{
						throw SUtil.throwUnchecked(t);
					}
				}));
			}
			catch(Exception e)
			{
				SUtil.throwUnchecked(e);
			}
		}

		for(Method method: findMethods(getPojoClazz(), anno))
		{
			if(fetchers==null)
			{
				fetchers	= new ArrayList<>(4);
			}
			
			fetchers.add(new Getter(method, method.getAnnotation(anno),
				createMethodInvocation(method, getPojoClazzes(), getContextFetchers(), null)));
		}

		return fetchers;
	}
	
	//-------- model cache --------
	
	/** The model cache. */
	protected static Map<List<Class<?>>, InjectionModel>	MODEL_CACHE	= new LinkedHashMap<>();
	
	/**
	 *  Helper to get the model at runtime for a stack of pojo objects.
	 *  Requires that the model exists.
	 */
	public static InjectionModel	get(List<Object> pojos)
	{
		
		List<Class<?>>	key	= new ArrayList<Class<?>>(pojos.size());
		for(Object pojo: pojos)
		{
			key.add(pojo!=null ? pojo.getClass() : Object.class);
		}
		return getStatic(key, null, null);
	}
	
	/**
	 *  Get the model for a stack of pojo classes.
	 */
	public static InjectionModel	getStatic(List<Class<?>> key, List<String> path, Map<Class<? extends Annotation>,List<IValueFetcherCreator>> contextfetchers)
	{
		boolean init	= false;
		InjectionModel	model;
		synchronized(MODEL_CACHE)
		{
			model	= MODEL_CACHE.get(key);
			if(model==null)
			{
				model	= new InjectionModel(key, path, contextfetchers);
				MODEL_CACHE.put(key, model);
				init	= true;
			}
			
			// Init in synchronized to avoid second component accessing model before init is complete
			if(init)
			{
				model.init();
			}
		}
		
		return model;
	}
	
	//-------- dependency scanning --------
	
	/** The field accesses by method. */
	protected static final Map<String, Set<Field>>	ACCESSED_FIELDS	= new LinkedHashMap<>();
	
	/** The code executed for a Dyn value, i.e. the Callable.call() method descriptor. */
	protected static final Map<Field, String>	DYN_METHODS	= new LinkedHashMap<>();
	
	/** The method accesses by method. */
	protected static final Map<String, Set<String>>	ACCESSED_METHODS	= new LinkedHashMap<>();
	
	/**
	 *  Scan a class for method and field accesses.
	 */
	protected static void scanClass(Class<?> pojoclazz)
	{
		// TODO: skip already scanned classes (e.g. due to common superclass)
		
		if(pojoclazz.getName().contains("$$Lambda"))
		{
			return;	// Skip lambda classes
		}
		
		// Skip java.lang for e.g. inner enums
		if(pojoclazz.getSuperclass()!=null && !"java.lang".equals(pojoclazz.getSuperclass().getPackageName()))
		{
			// Scan superclass first.
			scanClass(pojoclazz.getSuperclass());
		}
		
		String	pojoclazzname	= pojoclazz.getName().replace('.', '/');
		try
		{
			ClassReader	cr	= new ClassReader(pojoclazz.getName());
			cr.accept(new ClassVisitor(Opcodes.ASM9)
			{
				String	lastdyn	= null;
				
				@Override
				public void visitInnerClass(String name, String outerName, String innerName, int access)
				{
					// visitInnerClass also called for non-inner classes, wtf?
					if(!name.equals(pojoclazzname) && name.startsWith(pojoclazzname))
					{
//						System.out.println("Visiting inner class: "+name);
						try
						{
							Class<?> innerclazz = Class.forName(name.replace('/', '.'));
							scanClass(innerclazz);
						}
						catch(ClassNotFoundException e)
						{
							SUtil.throwUnchecked(e);
						}
					}

					super.visitInnerClass(name, outerName, innerName, access);
				}
				
				@Override
				public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
				{
					String	method	= pojoclazz.getName().replace('.', '/') +"."+name+desc; 
//					System.out.println("Visiting method: "+method);
					return new MethodVisitor(Opcodes.ASM9)
					{
			            @Override
			            public void visitFieldInsn(int opcode, String owner, String name, String descriptor)
			            {
			                if(opcode==Opcodes.GETFIELD)
			                {
//			                	System.out.println("\tVisiting field access: "+owner+"."+name);
								try
								{
									Class<?> ownerclazz = Class.forName(owner.replace('/', '.'));
									Field f	= SReflect.getField(ownerclazz, name);
									synchronized(ACCESSED_FIELDS)
									{
										Set<Field>	fields	= ACCESSED_FIELDS.get(method);
										if(fields==null)
										{
											fields	= new LinkedHashSet<>();
											ACCESSED_FIELDS.put(method, fields);
										}
										fields.add(f);
									}
								}
								catch(Exception e)
								{
									SUtil.throwUnchecked(e);
								}
			                }
			                
			                else if(opcode==Opcodes.PUTFIELD)
			                {
//			                	System.out.println("\tVisiting field write: "+owner+"."+name+"; "+lastdyn);
								try
								{
									Class<?> ownerclazz = Class.forName(owner.replace('/', '.'));
									Field f	= SReflect.getField(ownerclazz, name);
									if(f.getType().equals(Dyn.class) && lastdyn!=null)
									{
//										System.out.println("\tRemembering lambda for Dyn: "+f+", "+lastdyn);
										synchronized(DYN_METHODS)
										{
											DYN_METHODS.put(f, lastdyn);
										}
									}
								}
								catch(Exception e)
								{
									SUtil.throwUnchecked(e);
								}
			                }
			                
			                super.visitFieldInsn(opcode, owner, name, descriptor);
			            }
						
						public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface)
						{
							String	callee	= owner+"."+name+descriptor;
//							System.out.println("\tVisiting method call: "+callee);
							addMethodAccess(method, callee);
							
							String	dynclazz	= Dyn.class.getName().replace(".", "/");
							String	obsclazz	= ObservationMode.class.getName().replace(".", "/");
							
							// Remember call() from Callable object for next Dyn constructor.
							if("<init>".equals(name))
							{
								try
								{
									Class<?> calleeclazz = Class.forName(owner.replace('/', '.'));
									if(SReflect.isSupertype(Callable.class, calleeclazz))
									{
										lastdyn	= methodToAsmDesc(calleeclazz.getMethod("call"));
//										System.out.println("\tRemembering call(): "+lastdyn);
									}
								}
								catch(Exception e)
								{
									SUtil.throwUnchecked(e);
								}
							}
							
							// Only remember lambda when followed by a Dyn constructor
							// to store dependency on next putfield.
							else if(!callee.equals(dynclazz+".<init>(Ljava/util/concurrent/Callable;)V")
								&&  !callee.equals(dynclazz+".setUpdateRate(J)L"+dynclazz+";")
								&&  !callee.equals(dynclazz+".setObservationMode(L"+obsclazz+";)L"+dynclazz+";"))
							{
//								System.out.println("\tForgetting lambda due to: "+callee);
								lastdyn	= null;
							}
						}
						
						public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments)
						{
							if(bootstrapMethodArguments.length>=2 && (bootstrapMethodArguments[1] instanceof Handle))
							{
								Handle handle	= (Handle)bootstrapMethodArguments[1];
								String	callee	= handle.getOwner()+"."+handle.getName()+handle.getDesc();
//								System.out.println("\tVisiting lambda call: "+callee);
								addMethodAccess(method, callee);
								
								// Remember lambda for next Dyn constructor.
								lastdyn	= callee;
							}
							// else Do we need to handle other cases?
						}
						
						void addMethodAccess(String caller, String callee)
						{
							synchronized(ACCESSED_METHODS)
							{
								Set<String>	methods	= ACCESSED_METHODS.get(caller);
								if(methods==null)
								{
									methods	= new LinkedHashSet<>();
									ACCESSED_METHODS.put(method, methods);
								}
								methods.add(callee);
							}
						}
					};
				}
			}, 0);
		}
		catch(Exception e)
		{
			System.err.println("WARNING: Exception scanning class "+pojoclazzname+": "+e);
//			SUtil.throwUnchecked(e);
		}
	}
	
	/**
	 *  Convert method to ASM descriptor.
	 */
	protected static String methodToAsmDesc(Method method)
	{
		return method.getDeclaringClass().getName().replace('.', '/')
			+ "." + org.objectweb.asm.commons.Method.getMethod(method).toString();
	}
	
	/**
	 *  Scan byte code to find beliefs that are accessed in the method.
	 */
	public Set<String> findDependentFields(Method method)
	{
		return findDependentFields(methodToAsmDesc(method));
	}
	
	public Set<String> findDependentFields(Field f)
	{
		Set<String>	ret;
		String callable = null;
		synchronized(DYN_METHODS)
		{
			if(DYN_METHODS.containsKey(f))
			{
				callable	= DYN_METHODS.get(f);
			}
		}
		
		if(callable!=null)
		{
			ret	= findDependentFields(callable);
		}
		else
		{
			throw new UnsupportedOperationException("Cannot determine Callable.call() implementation for dynamic value: "+f);
		}
		return ret;
	}	

	
	/**
	 *  Scan byte code to find fields that are accessed in the method.
	 */
	public Set<String> findDependentFields(String desc)
	{
//		System.out.println("Finding fields accessed in method: "+desc);
		// Find all method calls
		List<String>	calls	= new ArrayList<>();
		calls.add(desc);
		synchronized(ACCESSED_METHODS)
		{
			for(int i=0; i<calls.size(); i++)
			{
				String call	= calls.get(i);
				if(ACCESSED_METHODS.containsKey(call))
				{
					// Add all sub-methods
					for(String subcall: ACCESSED_METHODS.get(call))
					{
						if(!calls.contains(subcall))
						{
							calls.add(subcall);
						}
					}
				}
			}
		}
		
		// Find all accessed fields by fully qualified name
		Set<String>	deps	= new LinkedHashSet<>();
		synchronized(ACCESSED_FIELDS)
		{
			for(String desc0: calls)
			{
				if(ACCESSED_FIELDS.containsKey(desc0))
				{
					for(Field f: ACCESSED_FIELDS.get(desc0))
					{
//						System.out.println("Found field access in method: "+f+", "+method);
						MDynVal dep	= getRootModel().getDynamicValue(f);
						if(dep!=null)
						{
//							System.out.println("Found dynamic field access in method: "+dep+", "+method);
							deps.add(dep.name());
						}
					}
				}
//				else
//				{
//					System.out.println("No field access found in method: "+desc0);
//				}
			}
		}
		
		return deps;
	}

	/**
	 *  Check if the field contains a dynamic value (Dyn, Val, List, Set, Map, Bean).
	 */
	protected static boolean	isDynamicValue(Class<?> type)
	{
		return Dyn.class.equals(type)
			|| Val.class.equals(type)
			|| List.class.equals(type)
			|| Set.class.equals(type)
			|| Map.class.equals(type)
			|| SPropertyChange.getAdder(type)!=null;
	}
	
	/**
	 *  Get the value type for a dynamic value field
	 */
	protected Class<?>	getValueType(Field f)
	{
		Class<?>	ret	= f.getType();
		java.lang.reflect.Type		gtype	= f.getGenericType();
		
		if(SReflect.isSupertype(Dyn.class, ret)
			||SReflect.isSupertype(Val.class, ret))
		{
			if(gtype instanceof ParameterizedType)
			{
				ParameterizedType	generic	= (ParameterizedType)gtype;
				gtype	= generic.getActualTypeArguments()[0];
				ret	= getRawClass(gtype);
			}
			else
			{
				throw new RuntimeException("Dynamic value field does not define generic value type: "+f);
			}
		}
		
		if(SReflect.isSupertype(Collection.class, ret))
		{
			if(gtype instanceof ParameterizedType)
			{
				ParameterizedType	generic	= (ParameterizedType)gtype;
				ret	= getRawClass(generic.getActualTypeArguments()[0]);
			}
			else
			{
				throw new RuntimeException("Dynamic value field does not define generic value type: "+f);
			}
		}
		else if(SReflect.isSupertype(Map.class, ret))
		{
			if(gtype instanceof ParameterizedType)
			{
				ParameterizedType	generic	= (ParameterizedType)gtype;
				ret	= getRawClass(generic.getActualTypeArguments()[1]);
			}
			else
			{
				throw new RuntimeException("Dynamic value field does not define generic value type: "+f);
			}
		}
		
		return ret;
	}
	
	protected static Class<?> getRawClass(java.lang.reflect.Type type)
	{
		if(type instanceof Class<?>)
		{
			return (Class<?>)type;
		}
		else if(type instanceof ParameterizedType)
		{
			return (Class<?>)((ParameterizedType)type).getRawType();
		}
		else
		{
			throw new RuntimeException("Cannot get raw class of type: "+type);
		}
	}
	
	/**
	 *  Get the init code for a field containing a dynamic value (Dyn, Val, List, Set, Map, Bean).
	 *  @param f	The field.
	 *  @param evpub	The event publisher to use for change events.
	 *  @return The init code or null, if the field type is not supported.
	 */
	protected IInjectionHandle	createDynamicValueInit(MDynVal mdynval)
	{
		IInjectionHandle	ret = null;
		Field	f	= mdynval.field();
		String	name	= mdynval.name();
		
		if(name!=null)
		{
			MethodHandle	getter;
			MethodHandle	setter;
			try
			{
				f.setAccessible(true);
				getter	= MethodHandles.lookup().unreflectGetter(f);
				setter	= MethodHandles.lookup().unreflectSetter(f);
			}
			catch(Exception e)
			{
				throw SUtil.throwUnchecked(e);
			}
			
			// Dynamic belief (Dyn object)
			if(Dyn.class.equals(f.getType()))
			{
				Set<String>	deps	= findDependentFields(f);
				
				// Init Dyn on agent start
				ret	= (comp, pojos, context, dummy) ->
				{
					try
					{
						Dyn<Object>	dyn	= (Dyn<Object>)getter.invoke(pojos.get(pojos.size()-1));
						if(dyn==null)
						{
							throw new RuntimeException("Dynamic value field is null: "+f);
						}
						DynValHelper.init(dyn, comp, name);
						
						if(!deps.isEmpty())
						{
							((InjectionFeature)comp.getFeature(IInjectionFeature.class)).addDependencies(dyn, name, deps);
						}
											
						return null;
					}
					catch(Throwable t)
					{
						throw SUtil.throwUnchecked(t);
					}
				};
			}
			
			// Val belief
			else if(Val.class.equals(f.getType()))
			{				
				// Init Val on agent start
				ret	= (comp, pojos, context, dummy) ->
				{
					try
					{
						Val<Object>	value	= (Val<Object>)getter.invoke(pojos.get(pojos.size()-1));
						if(value==null)
						{
							value	= new Val<Object>((Object)null);
							setter.invoke(pojos.get(pojos.size()-1), value);
						}
						DynValHelper.init(value, comp, name);						
						return null;
					}
					catch(Throwable t)
					{
						throw SUtil.throwUnchecked(t);
					}
				};
			}
			// List belief
			else if(List.class.equals(f.getType()))
			{
				ret	= (comp, pojos, context, oldval) ->
				{
					try
					{
						List<Object>	value	= (List<Object>)getter.invoke(pojos.get(pojos.size()-1));
						if(value==null)
						{
							value	= new ArrayList<>();
						}
						value	= new ListWrapper<>(comp, name , ObservationMode.ON_ALL_CHANGES, value);
						setter.invoke(pojos.get(pojos.size()-1), value);
						return null;
					}
					catch(Throwable t)
					{
						throw SUtil.throwUnchecked(t);
					}
				};
			}
			
			// Set belief
			else if(Set.class.equals(f.getType()))
			{
				ret	= (comp, pojos, context, oldval) ->
				{
					try
					{
						Set<Object>	value	= (Set<Object>)getter.invoke(pojos.get(pojos.size()-1));
						if(value==null)
						{
							value	= new LinkedHashSet<>();
						}
						value	= new SetWrapper<>(comp, name , ObservationMode.ON_ALL_CHANGES, value);
						setter.invoke(pojos.get(pojos.size()-1), value);
						return null;
					}
					catch(Throwable t)
					{
						throw SUtil.throwUnchecked(t);
					}
				};
			}
			
			// Map belief
			else if(Map.class.equals(f.getType()))
			{
				ret	= (comp, pojos, context, oldval) ->
				{
					try
					{
						Map<Object, Object>	value	= (Map<Object, Object>)getter.invoke(pojos.get(pojos.size()-1));
						if(value==null)
						{
							value	= new LinkedHashMap<>();
						}
						value	= new MapWrapper<>(comp, name , ObservationMode.ON_ALL_CHANGES, value);
						setter.invoke(pojos.get(pojos.size()-1), value);
						return null;
					}
					catch(Throwable t)
					{
						throw SUtil.throwUnchecked(t);
					}
				};
			}
			
			// Last resort -> Bean belief
			// Check if addPropertyChangeListener() method exists
			else if(SPropertyChange.getAdder(f.getType())!=null)
			{
				ret	= (comp, pojos, context, oldval) ->
				{
					try
					{
						Object	bean	= getter.invoke(pojos.get(pojos.size()-1));
						if(bean!=null)
						{
							SPropertyChange.updateListener(bean, null, null, comp, name, null);
						}
						else
						{
							System.err.println("Warning: bean is null and will not be observed (use Val<> for delayed setting): "+f);
						}
						return null;
					}
					catch(Throwable t)
					{
						throw SUtil.throwUnchecked(t);
					}
				};
			}
		}
		return ret;
	}

	
	/**
	 *  Helper method to find all constructors with the given annotation.
	 */
	public static List<Constructor<?>> findConstructors(Class<?> clazz, Class<? extends Annotation> annotation)
	{
		List<Constructor<?>>	allcons	= new ArrayList<>();
		for(Constructor<?> con: clazz.getDeclaredConstructors())
		{
			if(con.isAnnotationPresent(annotation))
			{
				allcons.add(con);
			}
		}
		return allcons;
	}
	
	/**
	 *  Helper method to find all direct and inherited methods with the given annotation.
	 *  Methods are returned in reverse order: superclass first, then subclass.
	 */
	public static List<Method> findMethods(Class<?> clazz, Class<? extends Annotation> annotation)
	{
		List<Method>	allmethods	= new ArrayList<>();
		Class<?> myclazz	= clazz;
		while(myclazz!=null)
		{
			// Sort by name as java does not guarantee any order (reversed because it is later re-reversed)
			List<Method>	methods	= Arrays.asList(myclazz.getDeclaredMethods());
			methods.sort((a,b) -> a.getName().compareTo(b.getName()));
			for(Method method: methods.reversed())
			{
				if(method.isAnnotationPresent(annotation))
				{
					allmethods.add(method);
				}
			}
			myclazz	= myclazz.getSuperclass();
		}
		
		// Reverse list to execute superclass methods first
		Set<List<Object>>	method_ids	= new HashSet<>();
		List<Method>	methods	= new ArrayList<>();
		while(!allmethods.isEmpty())
		{
			try
			{
				Method	m	= allmethods.removeLast();

				// Skip if already added (i.e. annotation declared in superclass and subclass)
				List<Object>	method_id	= new ArrayList<>();
				method_id.add(m.getName());
				for(Class<?> param: m.getParameterTypes())
					method_id.add(param);
				if(!method_ids.contains(method_id))
				{
					methods.add(m);
					method_ids.add(method_id);
				}
			}
			catch(Exception e)
			{
				// Should not happen
				SUtil.throwUnchecked(e);
			}
		}
		return methods;
	}
	
	/**
	 *  Helper method to find all direct and inherited inner classes with the given annotation.
	 *  Inner classes are returned in order: subclass first, then superclass.
	 */
	public static List<Class<?>> findInnerClasses(Class<?> clazz, Class<? extends Annotation> annotation)
	{
		List<Class<?>>	allclasses	= new ArrayList<>();
		Class<?> myclazz	= clazz;
		while(myclazz!=null)
		{
			for(Class<?> innerclazz: myclazz.getDeclaredClasses())
			{
				if(innerclazz.isAnnotationPresent(annotation))
				{
					allclasses.add(innerclazz);
				}
			}
			myclazz	= myclazz.getSuperclass();
		}
		return allclasses;
	}
	
	/**
	 *  Check constructors of the given class and find one where all args can be injected.
	 *  @throw {@link UnsupportedOperationException} when no viable constructor is found or multiple constructors are viable.
	 */
	public static IInjectionHandle	findViableConstructor(Class<?> clazz, List<Class<?>> parentclasses,
		Map<Class<? extends Annotation>, List<IValueFetcherCreator>> contextfetchers)
	{
		Constructor<?>	found	= null;
		IInjectionHandle	ret	= null;
		for(Constructor<?> con: clazz.getDeclaredConstructors())
		{
			try
			{
				ret	= createMethodInvocation(con, parentclasses, contextfetchers, null);
				if(found!=null)
				{
					throw new UnsupportedOperationException("Multiple viable constructors: "+con+", "+found);
				}
				found	= con;
			}
			catch(Exception e)
			{
				// ignore constructor.
			}
		}
		
		if(ret==null)
		{
			throw new UnsupportedOperationException("No viable constructors: "+clazz);
		}
		
		return ret;
	}
	
	/**
	 *  Create a handle for a method or constructor invocation.
	 */
	public static IInjectionHandle	createMethodInvocation(Executable method, List<Class<?>> classes,
		Map<Class<? extends Annotation>, List<IValueFetcherCreator>> contextfetchers, List<IInjectionHandle> preparams)
	{
		try
		{
			IInjectionHandle	ret;
			
			method.setAccessible(true);
			boolean	isstatic	= method instanceof Constructor || (method.getModifiers()&Modifier.STATIC)!=0;
			MethodHandle	handle	= method instanceof Method ? MethodHandles.lookup().unreflect((Method)method) : MethodHandles.lookup().unreflectConstructor((Constructor<?>) method);
			
			// Find parameters
			Parameter[]	params	= method.getParameters();
			Type[]	ptypes	= method.getGenericParameterTypes();
			if(ptypes.length!=0)
			{
				List<IInjectionHandle>	param_injections	= new ArrayList<>();
				for(int i=0; i<ptypes.length; i++)
				{
					// Check, if fetcher is provided from outside
					IInjectionHandle	fetcher	= preparams!=null && i<preparams.size()
						? preparams.get(i) : null;
					
					if(fetcher==null)
					{
						Set<IValueFetcherCreator>	tried	= new HashSet<IValueFetcherCreator>();
						synchronized(FETCHERS)
						{
							for(Class<? extends Annotation> anno: FETCHERS.keySet())
							{
								for(IValueFetcherCreator check: FETCHERS.get(anno))
								{
									if(tried.contains(check))
									{
										// Hack!!! Skip duplicate if same creator is added for different annotations.
										continue;
									}
									tried.add(check);
									
									IInjectionHandle	test	= check.getValueFetcher(classes, ptypes[i], params[i].isAnnotationPresent(anno) ? params[i].getAnnotation(anno) : null);
									if(test!=null)
									{
										if(fetcher!=null)
										{
											throw new RuntimeException("Conflicting parameter injections: "+method+", "+fetcher+", "+test);
										}
										fetcher	= test;
									}
								}
							}
						}
						if(contextfetchers!=null)
						{
							for(Class<? extends Annotation> anno: contextfetchers.keySet())
							{
								for(IValueFetcherCreator check: contextfetchers.get(anno))
								{
									if(tried.contains(check))
									{
										// Hack!!! Skip duplicate if same creator is added for different annotations.
										continue;
									}
									tried.add(check);
									
									IInjectionHandle	test	= check.getValueFetcher(classes, ptypes[i], params[i].isAnnotationPresent(anno) ? params[i].getAnnotation(anno) : null);
									if(test!=null)
									{
										if(fetcher!=null)
										{
											throw new RuntimeException("Conflicting parameter injections: "+fetcher+", "+test);
										}
										fetcher	= test;
									}
								}
							}
						}
					}
					
					if(fetcher!=null)
					{
						param_injections.add(fetcher);
					}
					else
					{
						throw new UnsupportedOperationException("Cannot inject "+ptypes[i].getTypeName()+" in "+method);
					}
				}
				
				if(isstatic)
				{
					ret	= new IInjectionHandle()
					{
						@Override
						public Object apply(IComponent self, List<Object> pojos, Object context, Object oldval)
						{
							try
							{
								Object[]	args	= new Object[param_injections.size()];
								for(int j=0; j<args.length; j++)
								{
									args[j]	= param_injections.get(j).apply(self, pojos, context, null);
								}
								
								return handle.invokeWithArguments(args);
							}
							catch(Throwable e)
							{
								// Rethrow user exception
								throw SUtil.throwUnchecked(e);
							}
						}
						
						@Override
						public boolean isStatic()
						{
							return true;
						}
					};
				}
				else
				{
					ret	= (self, pojos, context, oldval) ->
					{
						try
						{
							Object[]	args	= new Object[param_injections.size()+1];
							args[0]	= pojos.get(pojos.size()-1);
							for(int j=1; j<args.length; j++)
							{
								args[j]	= param_injections.get(j-1).apply(self, pojos, context, null);
							}
							
							return handle.invokeWithArguments(args);
						}
						catch(Throwable e)
						{
							// Rethrow user exception
							throw SUtil.throwUnchecked(e);
						}
					};
				}
			}
			else
			{
				if(isstatic)
				{
					ret	= new IInjectionHandle()
					{
						@Override
						public Object apply(IComponent self, List<Object> pojos, Object context, Object oldval)
						{
							try
							{
								return handle.invoke();
							}
							catch(Throwable e)
							{
								// Rethrow user exception
								throw SUtil.throwUnchecked(e);
							}
						}
						
						@Override
						public boolean isStatic()
						{
							return true;
						}
					};
				}
				else
				{
					ret	= (self, pojos, context, oldval) ->
					{
						try
						{
							return handle.invoke(pojos.get(pojos.size()-1));
						}
						catch(Throwable e)
						{
							// Rethrow user exception
							throw SUtil.throwUnchecked(e);
						}
					};					
				}
			}
			
			return ret;
		}
		catch(Exception e)
		{
			throw SUtil.throwUnchecked(e);
		}
	}
	
	//-------- extension points --------
	
	/** The supported parameter/field injections (i.e class -> value). */
	protected static Map<Class<? extends Annotation>,List<IValueFetcherCreator>>	FETCHERS	= new LinkedHashMap<>();
	
	/**
	 *  Add a parameter/field injection (i.e field/parameter type -> value fetcher).
	 *  In case of field injection, the context is the old field value.
	 */
	@SafeVarargs
	public static void	addValueFetcher(IValueFetcherCreator fetcher, Class<? extends Annotation>... annos)
	{
		if(annos.length==0)
		{
			// Catch common programming mistake.
			throw new IllegalArgumentException("Missing annotation type(s).");
		}
		
		synchronized(FETCHERS)
		{
			for(Class<? extends Annotation> anno: annos)
			{
				List<IValueFetcherCreator>	list	= FETCHERS.get(anno);
				if(list==null)
				{
					list	= new ArrayList<>(1);
					FETCHERS.put(anno, list);
				}
				list.add(fetcher);
			}
		}
	}
	
	
	/** Add e.g. pre/post inject code handles to model. */
	protected static List<IExtraCodeCreator>	EXTRA_CODE_CREATORS	= Collections.synchronizedList(new ArrayList<>());
	
	/**
	 *  Add e.g. pre/post inject code handles to model.
	 */
	public static void	addExtraCode(IExtraCodeCreator extra)
	{
		EXTRA_CODE_CREATORS.add(extra);
	}
	
	/** The supported method injections (i.e method -> injection handle). */
	protected static Map<Class<? extends Annotation>, List<IMethodInjectionCreator>>	METHOD_INJECTIONS	= new LinkedHashMap<>();

	/**
	 *  Add a method injections (i.e annotation -> method -> injection handle).
	 */
	@SafeVarargs
	public static void	addMethodInjection(IMethodInjectionCreator minjection, Class<? extends Annotation>... annos)
	{
		if(annos.length==0)
		{
			// Catch common programming mistake.
			throw new IllegalArgumentException("Missing annotation type(s).");
		}
		
		synchronized(METHOD_INJECTIONS)
		{
			for(Class<? extends Annotation> anno: annos)
			{
				List<IMethodInjectionCreator>	list	= METHOD_INJECTIONS.get(anno);
				if(list==null)
				{
					list	= new ArrayList<>(1);
					METHOD_INJECTIONS.put(anno, list);
				}
				list.add(minjection);
			}
		}
	}
}
