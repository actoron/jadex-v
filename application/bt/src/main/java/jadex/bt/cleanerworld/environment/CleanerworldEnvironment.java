package jadex.bt.cleanerworld.environment;

import java.util.HashSet;
import java.util.Set;

import jadex.environment.Environment;
import jadex.environment.EnvironmentTask;
import jadex.environment.EnvironmentTask.TaskData;
import jadex.environment.SpaceObject;
import jadex.execution.ComponentMethod;
import jadex.execution.NoCopy;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ITerminableFuture;
import jadex.future.TerminableFuture;
import jadex.math.IVector2;
import jadex.math.Vector2Double;

public class CleanerworldEnvironment extends Environment 
{
	private boolean daytime = true;
	
	private double defdistance = 0.05;
	
	public CleanerworldEnvironment() 
	{
		this(null, 10);
	}
	
	public CleanerworldEnvironment(int sps) 
	{
		this(null, sps);
	}
	
	public CleanerworldEnvironment(String id, int sps) 
	{
		super(id, sps);
	}
	
	public <T> T getData(String name, TaskData data, Class<T> type)
	{
		return data==null || data.data()==null? null: (T)data.data().get(name);
	}
	
	@ComponentMethod
	public IFuture<Void> createWorld()
	{
		addSpaceObject(new Waste(new Vector2Double(0.1, 0.5))).get();
		addSpaceObject(new Waste(new Vector2Double(0.2, 0.5))).get();
		addSpaceObject(new Waste(new Vector2Double(0.3, 0.5))).get();
		addSpaceObject(new Waste(new Vector2Double(0.9, 0.9))).get();
		addSpaceObject(new Wastebin(new Vector2Double(0.2, 0.2), 20)).get();
		addSpaceObject(new Wastebin(new Vector2Double(0.8, 0.1), 20)).get();
		addSpaceObject(new Chargingstation(new Vector2Double(0.775, 0.775))).get();
		addSpaceObject(new Chargingstation(new Vector2Double(0.15, 0.4))).get();
		return IFuture.DONE;
	}
	
	@ComponentMethod
	public IFuture<Boolean> isDaytime() 
	{
		return new Future<>(daytime);
	}

	@ComponentMethod
	public void setDaytime(boolean daytime) 
	{
		this.daytime = daytime;
	}
	
	@ComponentMethod
	public void toggleDaytime() 
	{
		this.daytime = !daytime;
	}
	
	@ComponentMethod
	public IFuture<Set<Wastebin>> getWastebins()
	{
		//System.out.println(getSpaceObjectsByType(Wastebin.class).get());
		return getSpaceObjectsByType(Wastebin.class);
	}
	
	@ComponentMethod
	public IFuture<Set<Cleaner>> getCleaners()
	{
		return getSpaceObjectsByType(Cleaner.class);
	}
	
	@ComponentMethod
	public IFuture<Set<Chargingstation>> getChargingStations()
	{
		return getSpaceObjectsByType(Chargingstation.class);
	}
	
	@ComponentMethod
	public IFuture<Set<Waste>> getWastes()
	{
		return getSpaceObjectsByType(Waste.class);
	}
	
	@ComponentMethod
	public IFuture<Wastebin> getWastebin(String id)
	{
		return new Future<>((Wastebin)getSpaceObject(id));
	}
	
	@ComponentMethod
	public void addWaste(Waste waste)
	{
		addSpaceObject(waste);
	}
	
	@ComponentMethod
	public void removeWaste(@NoCopy Waste waste)
	{
		removeSpaceObject(waste);
	}
	
	@ComponentMethod
	public ITerminableFuture<Void> move(@NoCopy Cleaner obj, IVector2 destination)
	{
		return move(obj, destination, obj.getSpeed());
	}
	
	@ComponentMethod
	public ITerminableFuture<Void> pickupWaste(@NoCopy Cleaner cl, @NoCopy Waste waste)
	{
		TerminableFuture<Void> ret = new TerminableFuture<Void>();
		
		Cleaner cleaner = (Cleaner)getSpaceObject(cl);
		
		addTask(new EnvironmentTask(cleaner, "pickupWaste", this, ret, data ->
		{
			return performPickupWaste(cleaner, getSpaceObject(waste), data.delta());
		}));
		
		return ret;
	}
	
	@ComponentMethod
	public ITerminableFuture<Void> dropWasteInWastebin(@NoCopy Cleaner cl, @NoCopy Waste waste, @NoCopy Wastebin wastebin)
	{
		TerminableFuture<Void> ret = new TerminableFuture<Void>();
		
		Cleaner cleaner = (Cleaner)getSpaceObject(cl);
		
		addTask(new EnvironmentTask(cleaner, "dropWasteInWastebin", this, ret, data ->
		{
			return performDropWasteInWastebin(cleaner, getSpaceObject(waste), getSpaceObject(wastebin), data.delta());
		}));
		
		return ret;
	}
	
	@ComponentMethod
	public ITerminableFuture<Void> loadBattery(@NoCopy Cleaner cl, @NoCopy Chargingstation station)
	{
		TerminableFuture<Void> ret = new TerminableFuture<Void>();
		
		Cleaner cleaner = (Cleaner)getSpaceObject(cl);
		
		addTask(new EnvironmentTask(cleaner, "loadBattery", this, ret, data ->
		{
			return performLoadBattery(cleaner, getSpaceObject(station), data.delta());
		}));
		
		return ret;
	}
	
	protected TaskData performLoadBattery(Cleaner cleaner, Chargingstation station, long deltatime)
	{
		IVector2 loc = cleaner.getPosition();
		IVector2 tloc = station.getPosition();
		
		if(loc.getDistance(tloc).getAsDouble()>defdistance)
			throw new RuntimeException("Not at location: "+cleaner+", "+station);
		
		double charge =cleaner.getChargestate();
		if(charge<1 && cleaner.getPosition().getDistance(station.getLocation()).getAsDouble()<0.01)
		{
			//System.out.println("charge inkr: "+0.01*deltatime/100.0);
			charge	= Math.min(charge + 0.01*deltatime/100.0, 1.0);
			//System.out.println("charge val: "+charge);
			cleaner.setChargestate(charge);
		}
		
		Set<SpaceObject> changed = null;
		changed = new HashSet<SpaceObject>();
		changed.add(cleaner);
		return new TaskData(cleaner.getChargestate()>=1? true: false, changed);
	}
	
	protected TaskData performPickupWaste(Cleaner cleaner, Waste waste, long deltatime)
	{
		IVector2 loc = cleaner.getPosition();
		IVector2 wloc = waste.getPosition();
		
		if(loc.getDistance(wloc).getAsDouble()>defdistance)
			throw new RuntimeException("Not at location: "+cleaner+", "+waste);
		
		if(cleaner.getCarriedWaste()!=null)
			throw new RuntimeException("Cleaner already carries waste: "+waste);
			
		if(waste==null)
			throw new RuntimeException("No such waste: "+waste);
			
		setPosition(waste, null);
		cleaner.setCarriedWaste(waste);
		//wastes.remove(waste);
			
		
		Set<SpaceObject> changed = null;
		changed = new HashSet<SpaceObject>();
		changed.add(cleaner);
		changed.add(waste);
		return new TaskData(true, changed);
	}
	
	protected TaskData performDropWasteInWastebin(Cleaner cleaner, Waste waste, Wastebin wastebin, long deltatime)
	{
		if(cleaner.getCarriedWaste()==null || !cleaner.getCarriedWaste().equals(waste))
			throw new RuntimeException("Cleaner does not carry the waste: "+cleaner+", "+waste);
		
		if(wastebin==null)
			throw new RuntimeException("No such waste bin: "+wastebin);
		
		if(cleaner.getLocation().getDistance(wastebin.getLocation()).getAsDouble()<defdistance)
		{
			// Update local and global objects
			wastebin.addWaste(waste);
			cleaner.setCarriedWaste(null);
		}
		else
		{
			throw new RuntimeException("Cleaner not in drop range: "+cleaner+", "+wastebin);
		}
		
		Set<SpaceObject> changed = null;
		changed = new HashSet<SpaceObject>();
		changed.add(cleaner);
		changed.add(waste);
		changed.add(wastebin);
		return new TaskData(true, changed);
	}
	
	@Override
	protected TaskData performMove(SpaceObject obj, IVector2 destination, double speed, long deltatime, double tolerance) 
	{
		Cleaner cl = (Cleaner)obj;
		TaskData res = super.performMove(obj, destination, speed, deltatime, tolerance);
		
		// Set new charge state
		double	chargestate	= cl.getChargestate()-deltatime/1000.0/100; 	// drop ~ 1%/sec while moving.
		//System.out.println("chargestate: "+chargestate);
		
		if(chargestate<0)
		{
			cl.setChargestate(0);
			throw new IllegalStateException("Ran out of battery during moveTo() -> target location not reached!");
		}
		cl.setChargestate(chargestate);
		
		/*Set<SpaceObject> changed = res.changed()==null? new HashSet<>(): new HashSet<>(res.changed());
		changed.add(cl);
		res = new TaskData(res.finsihed(), res.data(), changed);*/
	
		return res;
	}
	
}
