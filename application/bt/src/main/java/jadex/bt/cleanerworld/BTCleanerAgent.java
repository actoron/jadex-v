package jadex.bt.cleanerworld;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.SwingUtilities;

import jadex.bt.IBTProvider;
import jadex.bt.Val;
import jadex.bt.actions.TerminableUserAction;
import jadex.bt.cleanerworld.environment.Chargingstation;
import jadex.bt.cleanerworld.environment.Cleaner;
import jadex.bt.cleanerworld.environment.CleanerworldEnvironment;
import jadex.bt.cleanerworld.environment.Waste;
import jadex.bt.cleanerworld.environment.Wastebin;
import jadex.bt.cleanerworld.gui.libgdx.EnvGui;
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
import jadex.bt.state.ExecutionContext;
import jadex.bt.tool.BTViewer;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.environment.Environment;
import jadex.environment.EnvironmentEvent;
import jadex.environment.PerceptionProcessor;
import jadex.environment.SpaceObject;
import jadex.execution.ComponentMethod;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.ITerminableFuture;
import jadex.future.TerminableFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnEnd;
import jadex.injection.annotation.OnStart;
import jadex.math.IVector2;
import jadex.math.Vector2Double;
import jadex.rules.eca.EventType;

public class BTCleanerAgent implements IBTProvider
{
	/** The environment. */
	protected CleanerworldEnvironment env;
	private ISubscriptionIntermediateFuture<? extends EnvironmentEvent> envfut;
	
	/** The bdi agent. */
	@Inject
	protected IComponent agent;
	
	/** Set of the known wastes. Managed by SensorActuator object. */
	protected Set<Waste> wastes = new LinkedHashSet<>();
	
	/** Set of the known waste bins. Managed by SensorActuator object. */
	protected Set<Wastebin> wastebins = new LinkedHashSet<>();
	
	/** Set of the known charging stations. Managed by SensorActuator object. */
	protected Set<Chargingstation> stations = new LinkedHashSet<>();
	
	/** Set of the known other cleaners. Managed by SensorActuator object. */
	protected Set<Cleaner> others = new LinkedHashSet<>();
	
	/** Knowledge about myself. */
	protected Val<Cleaner> self = null;
		
	/** Day or night?. Use updaterate to re-check every second. */
	protected Val<Boolean> daytime = new Val<Boolean>(() -> getEnvironment().isDaytime().get(), 1000);
	
	//protected Val<Double> chargestate = new Val<Double>(() -> ((Cleaner)getEnvironment().getSpaceObject(getSelf().getId()).get()).getChargestate(), 1000);
	
	/** The patrol points. */
	protected List<IVector2> patrolpoints = new ArrayList<IVector2>();
	
	protected boolean ui;
	
	public BTCleanerAgent()
	{
	}
	
	public BTCleanerAgent(String envid)
	{
		this(envid, true);
	}
	
	public BTCleanerAgent(String envid, boolean ui)
	{
		this.env = (CleanerworldEnvironment)Environment.get(envid);
		this.ui = ui;
	}
	
	private CleanerworldEnvironment getEnvironment()
	{
		return env;
	}
	
	private IComponent getAgent()
	{
		return agent;
	}
	
	public Cleaner getSelf()
	{
		//return self;
		return self.get();
	}
	
	public Node<IComponent> createBehaviorTree() 	
    { 	
		return buildBehaviorTree();
       	//return BTCache.createOrGet(HelloSharedBTAgent.class, BTCleanerAgent::buildBehaviorTree);
    }
	
	protected static BTCleanerAgent getPojo(ExecutionContext<IComponent> context)
	{
		return (BTCleanerAgent)context.getUserContext().getPojo();
	}
	
	protected static BTCleanerAgent getPojo(IComponent agent)
	{
		return (BTCleanerAgent)agent.getPojo();
	}
	    
    public static Node<IComponent> buildBehaviorTree()
    {
		// find a charging station
		// succeeded when charging station is found
		ActionNode<IComponent> findstation = new ActionNode<>("findstation");
		findstation.setAction(new TerminableUserAction<IComponent>((e, agent) ->
		{
			//System.out.println("Find charging station");
			System.getLogger(BTCleanerAgent.class.getName()).log(Level.INFO, "Find charging station");
			
			TerminableFuture<NodeState> ret = new TerminableFuture<>();

			Chargingstation station = (Chargingstation)findClosestElement(getPojo(agent).internalGetStations(), getPojo(agent).getSelf().getLocation());
			if(station==null)
			{
				//ITerminableFuture<Void> fut = actsense.moveTo(Math.random(), Math.random());
				ITerminableFuture<Void> fut = getPojo(agent).getEnvironment().move((Cleaner)getPojo(agent).getSelf(), new Vector2Double(Math.random(), Math.random()));
				ret.setTerminationCommand(ex -> {System.getLogger(BTCleanerAgent.class.getName()).log(Level.INFO, "terminate on moveTo"); fut.terminate();});
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
			//System.out.println("find station deco: "+stations.size());
			NodeState ret = getPojo(context).internalGetStations().size()>0? NodeState.SUCCEEDED: state;
			return ret;
		}).setDetails("stations.size()>0"));
		//findstation.setSuccessCondition((node, execontext) -> stations.size()>0, 
		//	new EventType[]{new EventType(BTAgentFeature.VALUEADDED, "stations")});
		findstation.addDecorator(new SuccessDecorator<IComponent>().setCondition((node, state, context) -> getPojo(context).internalGetStations().size()>0)
			.observeCondition(new EventType[]{new EventType(BTAgentFeature.VALUEADDED, "stations")}).setDetails("stations.size()>0"));
		findstation.addDecorator(new RetryDecorator<IComponent>());
		
		// go to a charging station
		ActionNode<IComponent> gotostation = new ActionNode<>("movetostation");
		gotostation.setAction(new TerminableUserAction<IComponent>((e, agent) -> 
		{
			Chargingstation station = (Chargingstation)findClosestElement(getPojo(agent).internalGetStations(), getPojo(agent).getSelf().getLocation());
			//System.out.println("Go to station: "+agent.getId()+" "+station+" "+getPojo(agent).internalGetStations().size());
			System.getLogger(BTCleanerAgent.class.getName()).log(Level.INFO, "Go to station: "+agent.getId()+" "+station+" "+getPojo(agent).internalGetStations().size());
			TerminableFuture<NodeState> ret = new TerminableFuture<>();
			//ITerminableFuture<Void> fut = actsense.moveTo(station.getLocation());
			ITerminableFuture<Void> fut = getPojo(agent).getEnvironment().move((Cleaner)getPojo(agent).getSelf(), station.getLocation());
			ret.setTerminationCommand(ex -> {System.getLogger(BTCleanerAgent.class.getName()).log(Level.INFO, "terminate moveTo"); fut.terminate();});
			fut.then(Void -> ret.setResultIfUndone(NodeState.SUCCEEDED)).catchEx(ex -> ret.setResultIfUndone(NodeState.FAILED));
			return ret;
		}));
		
		// load at station
		ActionNode<IComponent> loadatstation = new ActionNode<>("loadatstation");
		loadatstation.setAction(new TerminableUserAction<IComponent>((e, agent) -> 
		{
			TerminableFuture<NodeState> ret = new TerminableFuture<>();
			Chargingstation station = (Chargingstation)findClosestElement(getPojo(agent).internalGetStations(), getPojo(agent).getSelf().getLocation());
			//System.out.println("Load at station: "+agent.getId()+" "+getPojo(agent).getSelf().getCarriedWaste()+" "+station);
			System.getLogger(BTCleanerAgent.class.getName()).log(Level.INFO, "Load at station: "+agent.getId()+" "+getPojo(agent).getSelf().getCarriedWaste()+" "+station);
			ITerminableFuture<Void> fut = getPojo(agent).getEnvironment().loadBattery((Cleaner)getPojo(agent).getSelf(), station);
			ret.setTerminationCommand(ex -> {System.getLogger(BTCleanerAgent.class.getName()).log(Level.INFO, "terminate loadBattery"); fut.terminate();});
			fut.then(Void -> {System.getLogger(BTCleanerAgent.class.getName()).log(Level.INFO, "loaded battery"); ret.setResultIfUndone(NodeState.SUCCEEDED);}).catchEx(ex -> ret.setResultIfUndone(NodeState.FAILED));
			return ret;
		}));
		
		// go to waste
		ActionNode<IComponent> gotowaste = new ActionNode<>("gotowaste");
		gotowaste.setAction(new TerminableUserAction<IComponent>((e, agent) -> 
		{
			Waste waste = (Waste)findClosestElement(getPojo(agent).internalGetWastes(), getPojo(agent).getSelf().getLocation());
			//System.out.println("Go to waste: "+agent.getId()+" "+waste+" "+getPojo(agent).getSelf().getCarriedWaste()+" "+getPojo(agent).internalGetWastes().size());
			System.getLogger(BTCleanerAgent.class.getName()).log(Level.INFO, "Go to waste: "+agent.getId()+" "+waste+" "+getPojo(agent).getSelf().getCarriedWaste()+" "+getPojo(agent).internalGetWastes().size());

			TerminableFuture<NodeState> ret = new TerminableFuture<>();
			//ITerminableFuture<Void> fut = actsense.moveTo(waste.getLocation());
			ITerminableFuture<Void> fut = getPojo(agent).getEnvironment().move((Cleaner)getPojo(agent).getSelf(), waste.getLocation());
			ret.setTerminationCommand(ex -> {System.getLogger(BTCleanerAgent.class.getName()).log(Level.INFO, "terminate moveTo"); fut.terminate();});
			fut.then(Void -> {System.getLogger(BTCleanerAgent.class.getName()).log(Level.INFO, "reached waste"); ret.setResultIfUndone(NodeState.SUCCEEDED);}).catchEx(ex -> ret.setResultIfUndone(NodeState.FAILED));
			return ret;
		}));
		gotowaste.addDecorator(new ConditionalDecorator<IComponent>().setFunction((node, state, context) -> getPojo(context).getSelf().getCarriedWaste()!=null? NodeState.SUCCEEDED: state)
			.setDetails("getSelf().getCarriedWaste()!=null"));
		
		// pickupwaste immediate success if already carries waste
		ActionNode<IComponent> pickupwaste = new ActionNode<>("pickupwaste");
		pickupwaste.setAction(new TerminableUserAction<IComponent>((e, agent) -> 
		{
			TerminableFuture<NodeState> ret = new TerminableFuture<>();
			Waste waste = (Waste)findClosestElement(getPojo(agent).internalGetWastes(), getPojo(agent).getSelf().getLocation());
			//System.out.println("Pickup waste: "+agent.getId()+" "+waste);
			System.getLogger(BTCleanerAgent.class.getName()).log(Level.INFO, "Pickup waste: "+agent.getId()+" "+waste);
			
			//actsense.pickUpWaste(waste);
			ITerminableFuture<Void> fut = getPojo(agent).getEnvironment().pickupWaste((Cleaner)getPojo(agent).getSelf(), waste);
			ret.setTerminationCommand(ex -> {System.getLogger(BTCleanerAgent.class.getName()).log(Level.INFO, "terminate on pickup"); fut.terminate();});
			fut.then(Void -> {System.getLogger(BTCleanerAgent.class.getName()).log(Level.INFO, "picked up waste"); ret.setResultIfUndone(NodeState.SUCCEEDED);}).catchEx(ex -> ret.setResultIfUndone(NodeState.FAILED));
			//fut.get();
			//System.out.println("picked up waste");
			//ret.setResultIfUndone(NodeState.SUCCEEDED);
			return ret;
		}));
		pickupwaste.addDecorator(new ConditionalDecorator<IComponent>().setFunction((node, state, context) -> getPojo(context).getSelf().getCarriedWaste()!=null? NodeState.SUCCEEDED: state)
			.setDetails("getSelf().getCarriedWaste()!=null"));
		
		// find wastebin
		// succeeded when wastbin is found
		ActionNode<IComponent> findwastebin = new ActionNode<>("findwastebin");
		findwastebin.setAction(new TerminableUserAction<IComponent>((e, agent) ->
		{
			//System.out.println("Find wastebin");
			System.getLogger(BTCleanerAgent.class.getName()).log(Level.INFO, "Find wastebin: "+agent.getId());
			
			TerminableFuture<NodeState> ret = new TerminableFuture<>();

			Wastebin wastebin = (Wastebin)findClosestElement(getPojo(agent).internalGetWastebins(), getPojo(agent).getSelf().getLocation());
			if(wastebin==null)
			{
				//ITerminableFuture<Void> fut = actsense.moveTo(Math.random(), Math.random());
				ITerminableFuture<Void> fut = getPojo(agent).getEnvironment().move((Cleaner)getPojo(agent).getSelf(), new Vector2Double(Math.random(), Math.random()));
				ret.setTerminationCommand(ex -> {System.getLogger(BTCleanerAgent.class.getName()).log(Level.INFO, "terminate moveTo"); fut.terminate();});
				fut.then(Void -> ret.setResultIfUndone(NodeState.FAILED)).catchEx(ex -> ret.setResultIfUndone(NodeState.FAILED));
			}
			else
			{
				ret.setResultIfUndone(NodeState.SUCCEEDED);
			}
			
			return ret;
		}));
		
		findwastebin.addDecorator(new SuccessDecorator<IComponent>().setCondition((node, state, context) -> getPojo(context).internalGetWastebins().size()>0)
			.observeCondition(new EventType[]{new EventType(BTAgentFeature.VALUEADDED, "wastebins")}).setDetails("wastebins.size()>0"));
		//findwastebin.setSuccessCondition((node, execontext) -> wastebins.size()>0, 
		//	new EventType[]{new EventType(BTAgentFeature.VALUEADDED, "wastebins")});
		findwastebin.addDecorator(new RetryDecorator<IComponent>());
		
		// move to wastebin
		ActionNode<IComponent> movetowastebin = new ActionNode<>("movetowastebin");
		movetowastebin.setAction(new TerminableUserAction<IComponent>((e, agent) -> 
		{
			Wastebin wastebin = (Wastebin)findClosestElement(getPojo(agent).internalGetWastebins(), getPojo(agent).getSelf().getLocation());
			//System.out.println("Go to wastebin: "+agent.getId()+" "+wastebin);
			System.getLogger(BTCleanerAgent.class.getName()).log(Level.INFO, "Goto wastebin: "+agent.getId()+" "+wastebin);
			TerminableFuture<NodeState> ret = new TerminableFuture<>();
			//ITerminableFuture<Void> fut = actsense.moveTo(wastebin.getLocation());
			ITerminableFuture<Void> fut = getPojo(agent).getEnvironment().move((Cleaner)getPojo(agent).getSelf(),wastebin.getLocation());
			ret.setTerminationCommand(ex -> {System.getLogger(BTCleanerAgent.class.getName()).log(Level.INFO, "terminate on actsense moveTo"); fut.terminate();});
			fut.then(Void -> ret.setResultIfUndone(NodeState.SUCCEEDED)).catchEx(ex -> ret.setResultIfUndone(NodeState.FAILED));
			return ret;
		}));
	
		// drop waste into wastebin
		ActionNode<IComponent> dropwaste = new ActionNode<>("dropwaste");
		dropwaste.setAction(new TerminableUserAction<IComponent>((e, agent) -> 
		{
			TerminableFuture<NodeState> ret = new TerminableFuture<>();
			Wastebin wastebin = (Wastebin)findClosestElement(getPojo(agent).internalGetWastebins(), getPojo(agent).getSelf().getLocation());
			//System.out.println("Drop waste: "+agent.getId()+" "+getPojo(agent).getSelf().getCarriedWaste()+" "+wastebin);
			System.getLogger(BTCleanerAgent.class.getName()).log(Level.INFO, "Goto wastebin: "+"Drop waste: "+agent.getId()+" "+getPojo(agent).getSelf().getCarriedWaste()+" "+wastebin);
			ITerminableFuture<Void> fut = getPojo(agent).getEnvironment().dropWasteInWastebin((Cleaner)getPojo(agent).getSelf(), getPojo(agent).getSelf().getCarriedWaste(), wastebin);
			ret.setTerminationCommand(ex -> {System.getLogger(BTCleanerAgent.class.getName()).log(Level.INFO, "terminate on drop"); fut.terminate();});
			fut.then(Void -> ret.setResultIfUndone(NodeState.SUCCEEDED)).catchEx(ex -> ret.setResultIfUndone(NodeState.FAILED));
			return ret;
		}));
		
		// randomwalk only if no waste known
		ActionNode<IComponent> randomwalk = new ActionNode<>("randomwalk");
		randomwalk.setAction(new TerminableUserAction<IComponent>((e, agent) -> 
		{ 
			//System.out.println("Random walk: "+agent.getId());
			System.getLogger(BTCleanerAgent.class.getName()).log(Level.INFO, "Random walk: "+agent.getId());
			//ITerminableFuture<Void> fut = actsense.moveTo(Math.random(), Math.random());
			ITerminableFuture<Void> fut = getPojo(agent).getEnvironment().move((Cleaner)getPojo(agent).getSelf(), new Vector2Double(Math.random(), Math.random()));
			TerminableFuture<NodeState> ret = new TerminableFuture<>();
			ret.setTerminationCommand(ex -> {System.getLogger(BTCleanerAgent.class.getName()).log(Level.INFO, "terminate on actsense moveTo"); fut.terminate();});
			fut.then(Void -> ret.setResultIfUndone(NodeState.SUCCEEDED)).catchEx(ex -> ret.setResultIfUndone(NodeState.FAILED));
			return ret;
		}));
		randomwalk.addDecorator(new ConditionalDecorator<IComponent>().setFunction((node, state, context) -> getPojo(context).internalGetWastes().size()==0? state: NodeState.FAILED).setDetails("wastes.size()==0"));
		randomwalk.addDecorator(new ConditionalDecorator<IComponent>().setFunction((node, state, context) -> getPojo(context).internalIsDaytime()? state: NodeState.FAILED).setDetails("daytime.get()"));
		randomwalk.addDecorator(new RepeatDecorator<IComponent>());
		
		// patrol walk
		Iterator<IVector2>[] locs = new Iterator[1];
		ActionNode<IComponent> patrolwalk = new ActionNode<>("patrolwalk");
		patrolwalk.setAction(new TerminableUserAction<IComponent>((e, agent) -> 
		{ 
			if(locs[0]==null || !locs[0].hasNext())
				locs[0] = getPojo(agent).getPatrolpoints().iterator();
			IVector2 loc = locs[0].next();
			//System.out.println("Patrol walk: "+agent.getId()+" "+loc);
			System.getLogger(BTCleanerAgent.class.getName()).log(Level.INFO, "Patrol walk: "+agent.getId()+" "+loc);
			//ITerminableFuture<Void> fut = actsense.moveTo(loc);
			ITerminableFuture<Void> fut = getPojo(agent).getEnvironment().move((Cleaner)getPojo(agent).getSelf(), loc);
			TerminableFuture<NodeState> ret = new TerminableFuture<>();
			ret.setTerminationCommand(ex -> {System.getLogger(BTCleanerAgent.class.getName()).log(Level.INFO, "terminate on actsense moveTo"); fut.terminate();});
			fut.then(Void -> ret.setResultIfUndone(NodeState.SUCCEEDED)).catchEx(ex -> ret.setResultIfUndone(NodeState.FAILED));
			return ret;
		}));
		//patrolwalk.addDecorator(new ConditionalDecorator<IComponent>().setFunction((node, state, context) -> !daytime.get()? NodeState.RUNNING: NodeState.FAILED));
		//patrolwalk.setTriggerCondition((node, execontext) -> !daytime.get(), new EventType[]{
		//	new EventType(BTAgentFeature.PROPERTYCHANGED, "daytime")});
		patrolwalk.addDecorator(new TriggerDecorator<IComponent>().setCondition((node, state, context) -> !getPojo(context).internalIsDaytime())
			.observeCondition(new EventType[]{new EventType(BTAgentFeature.VALUECHANGED, "daytime")}).setDetails("!daytime.get()"));
		patrolwalk.addDecorator(new FailureDecorator<IComponent>().setCondition((node, state, context) -> getPojo(context).internalIsDaytime())
			.observeCondition(new EventType[]{new EventType(BTAgentFeature.VALUECHANGED, "daytime")}).setDetails("daytime.get()"));
		patrolwalk.addDecorator(new RepeatDecorator<IComponent>());
					
		// maintain battery loaded
		SequenceNode<IComponent> loadbattery = new SequenceNode<>("loadbattery");
		loadbattery.addChild(findstation);
		loadbattery.addChild(gotostation);
		loadbattery.addChild(loadatstation);
		//loadbattery.setTriggerCondition((node, execontext) -> getSelf().getChargestate()<0.7, new EventType[]{	
		//	new EventType(BTAgentFeature.PROPERTYCHANGED, "chargestate")});
		loadbattery.addDecorator(new FailureDecorator<IComponent>().setCondition((node, state, context) -> getPojo(context).getChargestate()>0.8).setDetails("getChargestate()>0.8"));
		loadbattery.addDecorator(new TriggerDecorator<IComponent>().setCondition((node, state, context) -> getPojo(context).getChargestate()<0.8)
			//.observeCondition(new EventType[]{new EventType(BTAgentFeature.VALUECHANGED, "chargestate")}));
			.observeCondition(new EventType[]{new EventType(BTAgentFeature.VALUECHANGED, "self")}).setDetails("getChargestate()<0.8"));
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
		collectwaste.addDecorator(new ConditionalDecorator<IComponent>().setFunction((node, state, context) -> 
			(getPojo(context).internalGetWastes().size()>0 || getPojo(context).getSelf().getCarriedWaste()!=null) && getPojo(context).internalIsDaytime()? state: NodeState.FAILED)
			.setDetails("(wastes.size()>0 || getSelf().getCarriedWaste()!=null) && daytime.get()"));
		//collectwaste.setTriggerCondition((node, execontext) -> wastes.size()>0 && (getSelf().getCarriedWaste()==null || daytime.get()), new EventType[]{
		//	new EventType(BTAgentFeature.VALUEADDED, "wastes"), new EventType(BTAgentFeature.PROPERTYCHANGED, "daytime")});
		collectwaste.addDecorator(new TriggerDecorator<IComponent>().setCondition((node, state, context) -> 
			(getPojo(context).internalGetWastes().size()>0 || getPojo(context).getSelf().getCarriedWaste()!=null) && getPojo(context).internalIsDaytime())
			.observeCondition(new EventType[]{new EventType(BTAgentFeature.VALUECHANGED, "daytime"), new EventType(BTAgentFeature.VALUEADDED, "wastes")})
			.setDetails("(wastes.size()>0 || getSelf().getCarriedWaste()!=null) && daytime.get()"));
		collectwaste.addDecorator(new RetryDecorator<IComponent>(0));
		
		// main control
		SelectorNode<IComponent> sn = new SelectorNode<IComponent>("main");
		sn.addChild(loadbattery); // always
		sn.addChild(collectwaste); // at daytime, when waste
		sn.addChild(randomwalk); // at daytime, when no waste
		sn.addChild(patrolwalk); // at night
		sn.addDecorator(new RepeatDecorator<IComponent>(0, 1000));
		
		return sn;
	}
	
	public double getChargestate()
	{
		return getSelf().getChargestate();
		//return chargestate.get();
	}
	
	public static <T extends SpaceObject> T findClosestElement(Set<T> elements, IVector2 loc) 
	{
		return elements.stream()
			.filter(p -> p.getPosition() != null)
			.min(Comparator.comparingDouble(p -> distance(p.getPosition(), loc)))
			.orElse(null);
	}
	
	public static double distance(IVector2 p1, IVector2 p2) 
	{
		double dx = p1.getX().getAsDouble()-p2.getX().getAsDouble();
		double dy = p1.getY().getAsDouble()-p2.getY().getAsDouble();
        //return Math.sqrt(dx*dx + dy*dy);
		return dx*dx+dy*dy; // speed optimized
	}

	@OnStart
	public void start()
	{
		Cleaner s = new Cleaner(new Vector2Double(Math.random()*0.4+0.3, Math.random()*0.4+0.3), getAgent().getId().getLocalName(), 0.1, 0.1, 0.8);  
		s = (Cleaner)getEnvironment().addSpaceObject((SpaceObject)s).get();
		self = new Val<>(s);
		//System.out.println("started: self set"+agent.getId());
		
		PerceptionProcessor pp = new PerceptionProcessor();
		
		/*pp.manage(Waste.class, wastes, 
    		obj -> {
    			if(obj.getPosition()==null)
    			{
    				System.out.println("waste with pos null: "+obj);
    				return;
    			}
    			pp.findAndUpdateOrAdd(obj, wastes);
    		},
    		obj -> {wastes.remove(obj); System.out.println("removing: "+obj+" from "+wastes);},
    		obj -> {if(obj.getPosition()==null) 
    		{
    			System.out.println("removing: "+obj+" from "+wastes);
    			wastes.remove(obj); 
    		}}
    	);*/
		
		pp.manage(Waste.class, wastes);
		pp.manage(Wastebin.class, wastebins);
		pp.manage(Chargingstation.class, stations);
		pp.manage(Cleaner.class, others, obj -> 
		{
			if(obj.equals(getSelf()))
				getSelf().updateFrom(obj);
			else
				PerceptionProcessor.findAndUpdateOrAdd(obj, others);
		}, obj -> others.remove(obj), obj -> {if(obj.getPosition()==null) others.remove(obj);});
		
		envfut = getEnvironment().observeObject((Cleaner)getSelf());
		envfut.next(e ->
		{
			agent.getFeature(IExecutionFeature.class).scheduleStep(() ->
			{
				pp.handleEvent(e);
			});
		});
		
		patrolpoints.add(new Vector2Double(0.1, 0.1));
		patrolpoints.add(new Vector2Double(0.1, 0.9));
		patrolpoints.add(new Vector2Double(0.3, 0.9));
		patrolpoints.add(new Vector2Double(0.3, 0.1));
		patrolpoints.add(new Vector2Double(0.5, 0.1));
		patrolpoints.add(new Vector2Double(0.5, 0.9));
		patrolpoints.add(new Vector2Double(0.7, 0.9));
		patrolpoints.add(new Vector2Double(0.7, 0.1));
		patrolpoints.add(new Vector2Double(0.9, 0.1));
		patrolpoints.add(new Vector2Double(0.9, 0.9));
		
		// Open a window showing the agent's perceptions
		if(ui)
		{
			new SensorGui(agent.getComponentHandle()).setVisible(true);
			SwingUtilities.invokeLater(() -> new BTViewer(agent.getComponentHandle()).setVisible(true));
		}
	}
	
	@OnEnd
	public void onEnd()
	{
		envfut.terminate();
		env.removeSpaceObject(getSelf());
	}
	
	public List<IVector2> getPatrolpoints()
	{
		return patrolpoints;
	}
	
	public Set<Chargingstation> internalGetStations() 
	{
		return stations;
	}
	
	public Set<Waste> internalGetWastes() 
	{
		return wastes;
	}
	
	public Set<Wastebin> internalGetWastebins() 
	{
		return wastebins;
	}
	
	public Boolean internalIsDaytime() 
	{
		return daytime.get();
	}
	
	@ComponentMethod
	public IFuture<Set<Waste>> getWastes() 
	{
		return new Future<>(wastes);
	}

	@ComponentMethod
	public IFuture<Set<Wastebin>> getWastebins() 
	{
		return new Future<>(wastebins);
	}

	@ComponentMethod
	public IFuture<Set<Chargingstation>> getStations() 
	{
		return new Future<>(stations);
	}

	@ComponentMethod
	public IFuture<Set<Cleaner>> getCleaners() 
	{
		return new Future<>(others);
	}
	
	@ComponentMethod
	public IFuture<Cleaner> getCleaner() 
	{	
		return new Future<>(getSelf());
	}

	@ComponentMethod
	public IFuture<Boolean> isDaytime() 
	{
		return new Future<>(daytime.get());
	}	
	
	@ComponentMethod
	public IFuture<IVector2> getTarget() 
	{
		return getEnvironment().getMoveTarget(getSelf());
	}
	
	public static void main(String[] args)
	{
		//IComponentManager.get().getFeature(ILoggingFeature.class).setDefaultSystemLoggingLevel(Level.INFO);
		
		int fps = 5; // steps / frames per second
		CleanerworldEnvironment env = IComponentManager.get().create(new CleanerworldEnvironment(fps)).get().getPojoHandle(CleanerworldEnvironment.class);
		env.createWorld().get();
		String envid = Environment.add(env);
		
		IComponentManager.get().create(new BTCleanerAgent(envid));
		
		//EnvironmentGui.create(envid); // old Swing ui
		EnvGui.create(envid, env.getStepsPerSecond().get()); // new libgdx ui
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}