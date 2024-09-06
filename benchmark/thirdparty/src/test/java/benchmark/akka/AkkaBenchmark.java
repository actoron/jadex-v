package benchmark.akka;

import org.junit.jupiter.api.Test;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import jadex.benchmark.BenchmarkHelper;
import jadex.future.Future;


public class AkkaBenchmark
{
	//-------- messages --------
	
	public static record CreateActor(Future<ActorRef<DoLifecycle>> create){}
	public static record DoLifecycle(Future<String> ret, boolean stop){}
	
	//-------- benchmark actor --------
	
	public static class BenchmarkActor extends AbstractBehavior<DoLifecycle>
	{
		Future<String>	stop	= new Future<>();
		
		private BenchmarkActor(ActorContext<DoLifecycle> context)
		{
			super(context);
		}

		@Override
		public Receive<DoLifecycle> createReceive()
		{
			return newReceiveBuilder()
					.onMessage(DoLifecycle.class, this::onDoLifecycle)
					.onSignal(PostStop.class, this::onPostStop)
				.build();
		}

		private Behavior<DoLifecycle> onDoLifecycle(DoLifecycle command)
		{
			if(command.stop)
			{
				stop	= command.ret;
				return Behaviors.stopped();
			}
			else
			{
				command.ret.setResult(this.toString()+", "+command.toString());
				return this;
			}
		}
		
		private Behavior<DoLifecycle> onPostStop(PostStop signal)
		{
			stop.setResult(this.toString()+", "+signal.toString());
			return this;
		}
	}
	
	//-------- benchmark system --------
	
	public static class BenchmarkSystem	extends AbstractBehavior<CreateActor>
	{
		private BenchmarkSystem(ActorContext<CreateActor> context)
		{
			super(context);
		}

		@Override
		public Receive<CreateActor> createReceive()
		{
			return newReceiveBuilder()
					.onMessage(CreateActor.class, this::onCreateActor)
					.build();
		}

		private Behavior<CreateActor> onCreateActor(CreateActor command)
		{
			Behavior<DoLifecycle> behavior = Behaviors.setup(context -> new AkkaBenchmark.BenchmarkActor(context));
			ActorRef<DoLifecycle> actor = getContext().spawnAnonymous(behavior);
			
			Future<String> ret	= new Future<>();
			actor.tell(new DoLifecycle(ret, false));
			ret.get();
			command.create.setResult(actor);
			return this;
		}
	}
	
	//-------- methods --------
	
	@Test
	public void benchmarkTime()
	{
		ActorSystem<CreateActor> system = ActorSystem.create(Behaviors.setup(BenchmarkSystem::new), "benchmarkTime");
		
		BenchmarkHelper.benchmarkTime(() -> {
			Future<ActorRef<DoLifecycle>> create = new Future<>();
			system.tell(new CreateActor(create));
			ActorRef<DoLifecycle>	actor	= create.get();
			
			Future<String> stop = new Future<>();
			actor.tell(new DoLifecycle(stop, true));
			stop.get();
		});

		system.terminate();
	}

	@Test
	public void benchmarkMemory()
	{
		ActorSystem<CreateActor> system = ActorSystem.create(Behaviors.setup(BenchmarkSystem::new), "benchmarkMemory");
		
		BenchmarkHelper.benchmarkMemory(() -> {
			Future<ActorRef<DoLifecycle>> create = new Future<>();
			system.tell(new CreateActor(create));
			ActorRef<DoLifecycle>	actor	= create.get();
			
			return () ->
			{
				Future<String> stop = new Future<>();
				actor.tell(new DoLifecycle(stop, true));
				stop.get();
			};
		});

		system.terminate();
	}
}
