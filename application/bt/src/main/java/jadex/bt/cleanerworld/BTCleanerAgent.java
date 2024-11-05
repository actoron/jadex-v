package jadex.bt.cleanerworld;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jadex.bt.IBTProvider;
import jadex.bt.Val;
import jadex.bt.actions.TerminableUserAction;
import jadex.bt.actions.UserAction;
import jadex.bt.cleanerworld.environment.IChargingstation;
import jadex.bt.cleanerworld.environment.ICleaner;
import jadex.bt.cleanerworld.environment.ILocation;
import jadex.bt.cleanerworld.environment.ILocationObject;
import jadex.bt.cleanerworld.environment.IWaste;
import jadex.bt.cleanerworld.environment.IWastebin;
import jadex.bt.cleanerworld.environment.SensorActuator;
import jadex.bt.cleanerworld.environment.impl.Location;
import jadex.bt.cleanerworld.gui.libgdx.EnvGui;
import jadex.bt.cleanerworld.gui.swing.EnvironmentGui;
import jadex.bt.cleanerworld.gui.swing.SensorGui;
import jadex.bt.decorators.ConditionalDecorator;
import jadex.bt.decorators.FailureDecorator;
import jadex.bt.decorators.RepeatDecorator;
import jadex.bt.decorators.RetryDecorator;
import jadex.bt.decorators.SuccessDecorator;
import jadex.bt.decorators.TriggerDecorator;
import jadex.bt.impl.BTAgentFeature;
import jadex.bt.nodes.ActionNode;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.nodes.SelectorNode;
import jadex.bt.nodes.SequenceNode;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.future.Future;
import jadex.future.ITerminableFuture;
import jadex.future.TerminableFuture;
import jadex.logger.ILoggingFeature;
import jadex.logger.JadexLoggerFinder;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;
import jadex.rules.eca.EventType;

@Agent(type="bt")
public class BTCleanerAgent implements IBTProvider
{
	/** The bdi agent. */
	@Agent
	protected IComponent agent;
	
	/** The connector for the environment. */
	protected SensorActuator actsense = new SensorActuator();
	
	/** Set of the known wastes. Managed by SensorActuator object. */
	protected Set<IWaste> wastes = new LinkedHashSet<>();
	
	/** Set of the known waste bins. Managed by SensorActuator object. */
	protected Set<IWastebin> wastebins = new LinkedHashSet<>();
	
	/** Set of the known charging stations. Managed by SensorActuator object. */
	protected Set<IChargingstation> stations = new LinkedHashSet<>();
	
	/** Set of the known other cleaners. Managed by SensorActuator object. */
	protected Set<ICleaner> others = new LinkedHashSet<>();
	
	/** Knowledge about myself. Managed by SensorActuator object. */
	protected Val<ICleaner> self = new Val<>(actsense.getSelf());
	//protected ICleaner self = actsense.getSelf();
		
	/** Day or night?. Use updaterate to re-check every second. */
	protected Val<Boolean> daytime = new Val<Boolean>(() -> actsense.isDaytime(), 1000);
	
	protected Val<Double> chargestate = new Val<Double>(() -> actsense.getSelf().getChargestate(), 1000);
	
	/** The patrol points. */
	protected List<ILocation> patrolpoints = new ArrayList<ILocation>();
	
	public BTCleanerAgent()
	{
	}
	
	public Node<IComponent> createBehaviorTree()
	{
		// find a charging station
		// succeeded when charging station is found
		ActionNode<IComponent> findstation = new ActionNode<>("findstation");
		findstation.setAction(new TerminableUserAction<IComponent>((e, agent) ->
		{
			System.out.println("Find charging station");
			
			TerminableFuture<NodeState> ret = new TerminableFuture<>();

			IChargingstation station = (IChargingstation)findClosestElement(stations, getSelf().getLocation());
			if(station==null)
			{
				ITerminableFuture<Void> fut = actsense.moveTo(Math.random(), Math.random());
				ret.setTerminationCommand(ex -> {System.out.println("terminate on actsense moveTo"); fut.terminate();});
				fut.then(Void -> ret.setResultIfUndone(NodeState.FAILED)).catchEx(ex -> ret.setResultIfUndone(NodeState.FAILED));
			}
			else
			{
				ret.setResultIfUndone(NodeState.SUCCEEDED);
			}
			
			return ret;
		}));
		findstation.addDecorator(new ConditionalDecorator<IComponent>().setFunction((node, state, context) -> 
		{
			System.out.println("find station deco: "+stations.size());
			return stations.size()>0? NodeState.SUCCEEDED: NodeState.RUNNING;
		}));
		//findstation.setSuccessCondition((node, execontext) -> stations.size()>0, 
		//	new EventType[]{new EventType(BTAgentFeature.VALUEADDED, "stations")});
		findstation.addDecorator(new SuccessDecorator<IComponent>().setCondition((node, state, context) -> stations.size()>0)
			.observeCondition(new EventType[]{new EventType(BTAgentFeature.VALUEADDED, "stations")}));
		findstation.addDecorator(new RetryDecorator<IComponent>());
		
		// go to a charging station
		ActionNode<IComponent> gotostation = new ActionNode<>("movetostation");
		gotostation.setAction(new TerminableUserAction<IComponent>((e, agent) -> 
		{
			IChargingstation station = (IChargingstation)findClosestElement(stations, getSelf().getLocation());
			System.out.println("Go to station: "+agent.getId()+" "+station+" "+stations.size());
			TerminableFuture<NodeState> ret = new TerminableFuture<>();
			ITerminableFuture<Void> fut = actsense.moveTo(station.getLocation());
			ret.setTerminationCommand(ex -> {System.out.println("terminate on actsense moveTo"); fut.terminate();});
			fut.then(Void -> ret.setResultIfUndone(NodeState.SUCCEEDED)).catchEx(ex -> ret.setResultIfUndone(NodeState.FAILED));
			return ret;
		}));
		
		// load at station
		ActionNode<IComponent> loadatstation = new ActionNode<>("loatatstation");
		loadatstation.setAction(new UserAction<IComponent>((e, agent) -> 
		{
			Future<NodeState> ret = new Future<>();
			IChargingstation station = (IChargingstation)findClosestElement(stations, getSelf().getLocation());
			System.out.println("Load at station: "+agent.getId()+" "+actsense.getSelf().getCarriedWaste()+" "+station);
			try
			{
				// todo: make abortable?!
				actsense.recharge(station, 0.99).then(Void -> ret.setResultIfUndone(NodeState.SUCCEEDED))
					.catchEx(ex -> ret.setResult(NodeState.FAILED));
			}
			catch(Exception ex)
			{
				ret.setResultIfUndone(NodeState.FAILED);
			}
			return ret;
		}));
		
		// go to waste
		ActionNode<IComponent> gotowaste = new ActionNode<>("gotowaste");
		gotowaste.setAction(new TerminableUserAction<IComponent>((e, agent) -> 
		{
			IWaste waste = (IWaste)findClosestElement(wastes, getSelf().getLocation());
			System.out.println("Go to waste: "+agent.getId()+" "+waste);
			TerminableFuture<NodeState> ret = new TerminableFuture<>();
			ITerminableFuture<Void> fut = actsense.moveTo(waste.getLocation());
			ret.setTerminationCommand(ex -> {System.out.println("terminate on actsense moveTo"); fut.terminate();});
			fut.then(Void -> {System.out.println("reached waste"); ret.setResultIfUndone(NodeState.SUCCEEDED);}).catchEx(ex -> ret.setResultIfUndone(NodeState.FAILED));
			return ret;
		}));
		gotowaste.addDecorator(new ConditionalDecorator<IComponent>().setFunction((node, state, context) -> getSelf().getCarriedWaste()!=null? NodeState.SUCCEEDED: NodeState.RUNNING));
		
		// pickupwaste immediate success if already carries waste
		ActionNode<IComponent> pickupwaste = new ActionNode<>("pickupwaste");
		pickupwaste.setAction(new UserAction<IComponent>((e, agent) -> 
		{
			Future<NodeState> ret = new Future<>();
			IWaste waste = (IWaste)findClosestElement(wastes, getSelf().getLocation());
			
			System.out.println("Pickup waste: "+agent.getId()+" "+waste);
			
			try
			{
				// todo: make async
				actsense.pickUpWaste(waste);
				System.out.println("picked up waste");
				ret.setResultIfUndone(NodeState.SUCCEEDED);
			}
			catch(Exception ex)
			{
				ret.setResultIfUndone(NodeState.FAILED);
			}
			return ret;
		}));
		pickupwaste.addDecorator(new ConditionalDecorator<IComponent>().setFunction((node, state, context) -> getSelf().getCarriedWaste()!=null? NodeState.SUCCEEDED: NodeState.RUNNING));
		
		// find wastebin
		// succeeded when wastbin is found
		ActionNode<IComponent> findwastebin = new ActionNode<>("findwastebin");
		findwastebin.setAction(new TerminableUserAction<IComponent>((e, agent) ->
		{
			System.out.println("Find wastebin");
			
			TerminableFuture<NodeState> ret = new TerminableFuture<>();

			IWastebin wastebin = (IWastebin)findClosestElement(wastebins, getSelf().getLocation());
			if(wastebin==null)
			{
				ITerminableFuture<Void> fut = actsense.moveTo(Math.random(), Math.random());
				ret.setTerminationCommand(ex -> {System.out.println("terminate on actsense moveTo"); fut.terminate();});
				fut.then(Void -> ret.setResultIfUndone(NodeState.FAILED)).catchEx(ex -> ret.setResultIfUndone(NodeState.FAILED));
			}
			else
			{
				ret.setResultIfUndone(NodeState.SUCCEEDED);
			}
			
			return ret;
		}));
		
		findwastebin.addDecorator(new SuccessDecorator<IComponent>().setCondition((node, state, context) -> wastebins.size()>0)
			.observeCondition(new EventType[]{new EventType(BTAgentFeature.VALUEADDED, "wastebins")}));
		//findwastebin.setSuccessCondition((node, execontext) -> wastebins.size()>0, 
		//	new EventType[]{new EventType(BTAgentFeature.VALUEADDED, "wastebins")});
		findwastebin.addDecorator(new RetryDecorator<IComponent>());
		
		// move to wastebin
		ActionNode<IComponent> movetowastebin = new ActionNode<>("movetowastebin");
		movetowastebin.setAction(new TerminableUserAction<IComponent>((e, agent) -> 
		{
			IWastebin wastebin = (IWastebin)findClosestElement(wastebins, getSelf().getLocation());
			System.out.println("Go to wastebin: "+agent.getId()+" "+wastebin);
			TerminableFuture<NodeState> ret = new TerminableFuture<>();
			ITerminableFuture<Void> fut = actsense.moveTo(wastebin.getLocation());
			ret.setTerminationCommand(ex -> {System.out.println("terminate on actsense moveTo"); fut.terminate();});
			fut.then(Void -> ret.setResultIfUndone(NodeState.SUCCEEDED)).catchEx(ex -> ret.setResultIfUndone(NodeState.FAILED));
			return ret;
		}));
	
		// drop waste into wastebin
		ActionNode<IComponent> dropwaste = new ActionNode<>("dropwaste");
		dropwaste.setAction(new UserAction<IComponent>((e, agent) -> 
		{
			Future<NodeState> ret = new Future<>();
			IWastebin wastebin = (IWastebin)findClosestElement(wastebins, getSelf().getLocation());
			System.out.println("Drop waste: "+agent.getId()+" "+actsense.getSelf().getCarriedWaste()+" "+wastebin);
			try
			{
				// todo: make async
				actsense.dropWasteInWastebin(actsense.getSelf().getCarriedWaste(), wastebin);
				ret.setResultIfUndone(NodeState.SUCCEEDED);
			}
			catch(Exception ex)
			{
				ret.setResultIfUndone(NodeState.FAILED);
			}
			return ret;
		}));
		
		// randomwalk only if no waste known
		ActionNode<IComponent> randomwalk = new ActionNode<>("randomwalk");
		randomwalk.setAction(new TerminableUserAction<IComponent>((e, agent) -> 
		{ 
			System.out.println("Random walk: "+agent.getId());
			ITerminableFuture<Void> fut = actsense.moveTo(Math.random(), Math.random());
			TerminableFuture<NodeState> ret = new TerminableFuture<>();
			ret.setTerminationCommand(ex -> {System.out.println("terminate on actsense moveTo"); fut.terminate();});
			fut.then(Void -> ret.setResultIfUndone(NodeState.SUCCEEDED)).catchEx(ex -> ret.setResultIfUndone(NodeState.FAILED));
			return ret;
		}));
		randomwalk.addDecorator(new ConditionalDecorator<IComponent>().setFunction((node, state, context) -> wastes.size()==0? NodeState.RUNNING: NodeState.FAILED));
		randomwalk.addDecorator(new ConditionalDecorator<IComponent>().setFunction((node, state, context) -> daytime.get()? NodeState.RUNNING: NodeState.FAILED));
		randomwalk.addDecorator(new RepeatDecorator<IComponent>());
		
		// patrol walk
		Iterator<ILocation>[] locs = new Iterator[1];
		ActionNode<IComponent> patrolwalk = new ActionNode<>("patrolwalk");
		patrolwalk.setAction(new TerminableUserAction<IComponent>((e, agent) -> 
		{ 
			if(locs[0]==null || !locs[0].hasNext())
				locs[0] = patrolpoints.iterator();
			ILocation loc = locs[0].next();
			System.out.println("Patrol walk: "+agent.getId()+" "+loc);
			ITerminableFuture<Void> fut = actsense.moveTo(loc);
			TerminableFuture<NodeState> ret = new TerminableFuture<>();
			ret.setTerminationCommand(ex -> {System.out.println("terminate on actsense moveTo"); fut.terminate();});
			fut.then(Void -> ret.setResultIfUndone(NodeState.SUCCEEDED)).catchEx(ex -> ret.setResultIfUndone(NodeState.FAILED));
			return ret;
		}));
		//patrolwalk.addDecorator(new ConditionalDecorator<IComponent>().setFunction((node, state, context) -> !daytime.get()? NodeState.RUNNING: NodeState.FAILED));
		//patrolwalk.setTriggerCondition((node, execontext) -> !daytime.get(), new EventType[]{
		//	new EventType(BTAgentFeature.PROPERTYCHANGED, "daytime")});
		patrolwalk.addDecorator(new TriggerDecorator<IComponent>().setCondition((node, state, context) -> !daytime.get())
			.observeCondition(new EventType[]{new EventType(BTAgentFeature.VALUECHANGED, "daytime")}));
		patrolwalk.addDecorator(new RepeatDecorator<IComponent>());
					
		// maintain battery loaded
		SequenceNode<IComponent> loadbattery = new SequenceNode<>("loadbattery");
		loadbattery.addChild(findstation);
		loadbattery.addChild(gotostation);
		loadbattery.addChild(loadatstation);
		//loadbattery.setTriggerCondition((node, execontext) -> getSelf().getChargestate()<0.7, new EventType[]{	
		//	new EventType(BTAgentFeature.PROPERTYCHANGED, "chargestate")});
		loadbattery.addDecorator(new FailureDecorator<IComponent>().setCondition((node, state, context) -> getChargestate()>0.7));
		loadbattery.addDecorator(new TriggerDecorator<IComponent>().setCondition((node, state, context) -> getChargestate()<0.7)
			.observeCondition(new EventType[]{new EventType(BTAgentFeature.VALUECHANGED, "chargestate")}));
			//.observeCondition(new EventType[]{new EventType(BTAgentFeature.PROPERTYCHANGED, "self", "chargestate")}));
		
		// || (nearStation() && chargestate<0.99)
		//loadbattery.addAfterDecorator(new RetryDecorator<IComponent>(0));
		
		// collect waste should be activated when
		SequenceNode<IComponent> collectwaste = new SequenceNode<>("collectwaste");
		collectwaste.addChild(gotowaste);
		collectwaste.addChild(pickupwaste);
		collectwaste.addChild(findwastebin);
		collectwaste.addChild(movetowastebin);
		collectwaste.addChild(dropwaste);
		collectwaste.addDecorator(new ConditionalDecorator<IComponent>().setFunction((node, state, context) -> daytime.get()? NodeState.RUNNING: NodeState.FAILED));
		//collectwaste.setTriggerCondition((node, execontext) -> wastes.size()>0 && (getSelf().getCarriedWaste()==null || daytime.get()), new EventType[]{
		//	new EventType(BTAgentFeature.VALUEADDED, "wastes"), new EventType(BTAgentFeature.PROPERTYCHANGED, "daytime")});
		collectwaste.addDecorator(new TriggerDecorator<IComponent>().setCondition((node, state, context) -> wastes.size()>0 && (getSelf().getCarriedWaste()==null || daytime.get()))
			.observeCondition(new EventType[]{new EventType(BTAgentFeature.VALUECHANGED, "daytime")}));
		collectwaste.addDecorator(new RetryDecorator<IComponent>(0));
		
		// main control
		SelectorNode<IComponent> sn = new SelectorNode<IComponent>("main");
		sn.addChild(loadbattery); // always
		sn.addChild(randomwalk); // at daytime, when no waste
		sn.addChild(collectwaste); // at daytime, when waste
		sn.addChild(patrolwalk); // at night
		sn.addDecorator(new RepeatDecorator<IComponent>(0, 1000));
		
		return sn;
	}
	
	public ICleaner getSelf()
	{
		//return self;
		return self.get();
	}
	
	public double getChargestate()
	{
		//return getSelf().getChargestate();
		return chargestate.get();
	}
	
	public static ILocationObject findClosestElement(Set<? extends ILocationObject> elements, ILocation loc) 
	{
		return elements.stream().min(Comparator.comparingDouble(p -> distance(p.getLocation(), loc))).orElse(null);
	}
	
	public static double distance(ILocation p1, ILocation p2) 
	{
		double dx = p1.getX()-p2.getX();
		double dy = p1.getY()-p2.getY();
        //return Math.sqrt(dx*dx + dy*dy);
		return dx*dx+dy*dy; // speed optimized
	}

	@OnStart
	private void start()
	{
		actsense.manageWastesIn(wastes);
		actsense.manageWastebinsIn(wastebins);
		actsense.manageChargingstationsIn(stations);
		actsense.manageCleanersIn(others);
		
		patrolpoints.add(new Location(0.1, 0.1));
		patrolpoints.add(new Location(0.1, 0.9));
		patrolpoints.add(new Location(0.3, 0.9));
		patrolpoints.add(new Location(0.3, 0.1));
		patrolpoints.add(new Location(0.5, 0.1));
		patrolpoints.add(new Location(0.5, 0.9));
		patrolpoints.add(new Location(0.7, 0.9));
		patrolpoints.add(new Location(0.7, 0.1));
		patrolpoints.add(new Location(0.9, 0.1));
		patrolpoints.add(new Location(0.9, 0.9));
		
		// Open a window showing the agent's perceptions
		new SensorGui(actsense).setVisible(true);
	}
	
	public static void main(String[] args)
	{
		IComponentManager.get().getFeature(ILoggingFeature.class).setDefaultSystemLoggingLevel(Level.INFO);
		
		IComponentManager.get().create(new BTCleanerAgent());
		//EnvironmentGui.create();
		EnvGui.createEnv();
		IComponentManager.get().waitForLastComponentTerminated();
	}
}