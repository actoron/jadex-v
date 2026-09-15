package apmn;

import jadex.bdi.annotation.*;
import jadex.common.SReflect;
import jadex.core.IComponent;
import jadex.injection.Val;
import jadex.injection.annotation.OnStart;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.*;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.io.*;
import java.lang.reflect.Type;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

public class AgentBuilder
{
    private Class<?> agentClass;

    public static void main(String[] args)
    {
        AgentBuilder agentBuilder = new AgentBuilder();
        try
        {
            agentBuilder.createAgent();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public void createAgent() throws Exception
    {
        ByteBuddy byteBuddy = new ByteBuddy();

        //Annotations
        AnnotationDescription bdiAnn = AnnotationDescription.Builder.ofType(BDIAgent.class).build();
        AnnotationDescription beliefAnn = AnnotationDescription.Builder.ofType(Belief.class).build();
        AnnotationDescription goalAnn = AnnotationDescription.Builder.ofType(Goal.class).build();
        AnnotationDescription goalparamAnn = AnnotationDescription.Builder.ofType(GoalParameter.class).build();
        AnnotationDescription goalcreatAnn = AnnotationDescription.Builder.ofType(GoalCreationCondition.class)
                .defineArray("factchanged", new String[]{"startProcess"}).build();
        AnnotationDescription goaltargAnn = AnnotationDescription.Builder.ofType(GoalTargetCondition.class).build();
        AnnotationDescription onstartAnn = AnnotationDescription.Builder.ofType(OnStart.class).build();


        //ParameterizedType
        Type fieldType = new SReflect.ParameterizedTypeImpl(Val.class, String.class);

        //MethodCall
        MethodCall getClassName = MethodCall.invoke(Class.class.getMethod("getName"))
                .onMethodCall(MethodCall.invoke(Object.class.getMethod("getClass")));

        //Classes
        DynamicType.Builder<?> agentActorBuilder = byteBuddy
                .subclass(Object.class)
                .name("Example");

        TypeDescription agentActor = agentActorBuilder.toTypeDescription();

        //Inner Class
        DynamicType.Builder<?> missionGoalBuilder = byteBuddy
                .subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .name("Example$MissionGoal")
                .annotateType(goalAnn)
                //Getter
                .defineMethod("getText", String.class, Visibility.PUBLIC)
                .intercept(MethodCall.invoke(Val.class.getMethod("get")).onField("text")
                        .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
                //Setter
                .defineMethod("setText", void.class, Visibility.PUBLIC)
                .withParameter(String.class, "val")
                .intercept(
                        MethodCall.invoke(Val.class.getMethod("set", Object.class)).onField("text")
                                .withArgument(0)
                                .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
                //Field
                .defineField("text", fieldType, Visibility.PROTECTED)
                .annotateField(goalparamAnn)
//
//                .defineField("tmp", Object.class, Visibility.PROTECTED)
//
                //Constructor
                .defineConstructor(Visibility.PUBLIC)
                .withParameter(String.class, "text", 0)
                .intercept(
                        MethodCall.invoke(Object.class.getDeclaredConstructor())
                                .andThen(
                                        MethodCall.construct(Val.class.getDeclaredConstructor(Object.class))
                                                .withArgument(0)
                                                .setsField(named("text")
                                                )
                                )).annotateMethod(goalcreatAnn)
                //Method checkTarget
//                .defineMethod("checkTarget", boolean.class, Visibility.PUBLIC)
//                .intercept(
//                        MethodCall.invoke(named("get"))
//                                .onField("text")
//                                .setsField(named("tmp"))
//                                .andThen(
//                                        MethodCall.invoke(Object.class.getMethod("equals", Object.class))
//                                                .onField("tmp")
//                                                .with("finished")
                //Method checkTarget
                .defineMethod("checkTarget", boolean.class, Visibility.PUBLIC)
                .intercept(
                        MethodCall.invoke(String.class.getMethod("equals", Object.class))
                                .on("finished")
                                .withMethodCall(MethodCall.invoke(named("getText")))
                )

//                ))
                .annotateMethod(goaltargAnn)
                .innerTypeOf(agentActor)
                .asMemberType();

        TypeDescription missionGoal = missionGoalBuilder.toTypeDescription();

        MethodDescription setMethod = missionGoal.getDeclaredMethods()
                .filter(named("setText").and(takesArguments(String.class))).getOnly();

        //Annotations
        //innerAnn
        AnnotationDescription trigAnn = AnnotationDescription.Builder.ofType(Trigger.class)
                .defineTypeArray("goals", missionGoal)
                .build();
        //OuterAnn
        AnnotationDescription planAnn = AnnotationDescription.Builder.ofType(Plan.class).define("trigger", trigAnn).build();

        //innerAnn
        AnnotationDescription trigAnn1 = AnnotationDescription.Builder.ofType(Trigger.class)
                .defineTypeArray("goalfinisheds", missionGoal)
                .build();
        //OuterAnn
        AnnotationDescription planAnn1 = AnnotationDescription.Builder.ofType(Plan.class).define("trigger", trigAnn1).build();

        //OuterClass
        agentActorBuilder = agentActorBuilder
                .annotateType(bdiAnn)
                .defineField("startProcess", fieldType, Visibility.PRIVATE)
                .annotateField(beliefAnn)
                .defineMethod("body", void.class, Visibility.PUBLIC)
                .intercept(
                        MethodCall.invoke(Val.class.getMethod("set", Object.class))
                                .onField("startProcess")
                                .with("Hello AgentStarter")
                                .andThen(
                                        MethodCall.invoke(PrintStream.class.getMethod("println", String.class))
                                                .onField(System.class.getField("out"))
                                                .withMethodCall(getClassName)
                                )
                )
                .annotateMethod(onstartAnn)
                .defineMethod("printHello", void.class, Visibility.PROTECTED)
                .withParameter(missionGoal, "goal", 0)
                .intercept(
                        MethodCall.invoke(setMethod)
                                .onArgument(0)
                                .with("finis")
                )
                .annotateMethod(planAnn)
                .defineMethod("finished", void.class, Visibility.PROTECTED)
                .withParameter(missionGoal, "goal")
                .withParameter(IComponent.class, "comp")
                .intercept(
                        MethodCall.invoke(IComponent.class.getMethod("terminate"))
                                .onArgument(1)
                )
                .annotateMethod(planAnn1)
                .declaredTypes(missionGoal);

        //Build together ClassComponents
        DynamicType.Unloaded<?> mG = missionGoalBuilder.make();
        DynamicType.Unloaded<?> aA = agentActorBuilder.make();

        DynamicType.Loaded<?> loaded = aA.include(mG).load(Val.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION);

        this.agentClass = loaded.getLoaded();

        loaded.saveIn(new File("application/bdi-llm/build/classes/java/main"));
    }

    public Class<?> getAgentClass()
    {
        return agentClass;
    }
}
