import java.util.Arrays;
import java.util.function.Consumer;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.LambdaInstrumentationStrategy;

public class ByteBuddyLambdaTest {	
	public static void main(String[] args) {
		new AgentBuilder.Default()
			.with(LambdaInstrumentationStrategy.ENABLED)
			.installOn(ByteBuddyAgent.install());
		
		interface StringConsumer extends Consumer<String> {}
		StringConsumer	test	= System.out::println;
		test.accept("test");
		
		Class<?> clazz	= test.getClass();
		System.out.println("TypeN: "+clazz.getTypeName());
		System.out.println("Super: "+clazz.getSuperclass());
		System.out.println("Decla: "+clazz.getDeclaringClass());
		
		
		System.out.println("Inter: "+Arrays.toString(clazz.getInterfaces()));
		System.out.println("Inner: "+Arrays.toString(clazz.getDeclaredClasses()));
		System.out.println("Field: "+Arrays.toString(clazz.getDeclaredFields()));
		System.out.println("Methd: "+Arrays.toString(clazz.getDeclaredMethods()));
	}
}
