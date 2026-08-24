package jadex.bding.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jadex.bding.AgentModel;
import jadex.bding.Goal;
import jadex.bding.IBDINGAgentFeature;
import jadex.bding.IReasoner;
import jadex.bding.annotation.Belief;
import jadex.bding.annotation.Model;
import jadex.bding.annotation.Reasoner;
import jadex.core.impl.ILifecycle;
import jadex.future.ITerminableFuture;
import jadex.future.TerminableFuture;


public class BDINGAgentFeature implements IBDINGAgentFeature, ILifecycle
{
	/** The component. */
	protected BDINGAgent self;

	/** The currently adopted goals. */
	protected Set<RGoal> goals;

	/** Reasoner bridge to e.g. llm. */
	protected IReasoner reasoner;

	/** Model with goals, intentions and plans. */
	protected AgentModel model;
	
	/**
	 *  Create the feature.
	 */
	public BDINGAgentFeature(BDINGAgent self)
	{
		this.self	= self;
		this.goals = new HashSet<>();
		this.reasoner = findValue(Reasoner.class, IReasoner.class);
		if(reasoner==null)
			reasoner = new LlmReasoner();
		this.model = findValue(Model.class, AgentModel.class);
		if(model==null)
			model = new AgentModel();
		initBeliefs();
	}

	protected void initBeliefs()
	{
		Object pojo = self.getPojo();

		Class<?> clazz = pojo.getClass();

		while(clazz != null && clazz != Object.class)
		{
			for(Field field : clazz.getDeclaredFields())
			{
				if(field.isAnnotationPresent(Belief.class))
				{
					field.setAccessible(true);
					
					Belief ann = field.getAnnotation(Belief.class);
					String name = ann.name().isEmpty()? field.getName(): ann.name();
					String desc = ann.description().isEmpty()? null: ann.description();

					if(model.getBelief(name) != null)
						throw new IllegalArgumentException("Duplicate belief name: " + name);

					new jadex.bding.Belief(name, desc, field, model);
				}
			}

			clazz = clazz.getSuperclass();
		}
	}

	protected <T> T findValue(Class<? extends Annotation> ann, Class<T> type)
	{
		T ret = null;

		Field rfield = null;

		Object pojo = self.getPojo();

		Class<?> clazz = pojo.getClass();

		while(clazz != null && clazz != Object.class)
		{
			for(Field field : clazz.getDeclaredFields())
			{
				if(field.isAnnotationPresent(ann))
				{
					if(rfield != null)
						throw new RuntimeException("Multiple @Reasoner fields found in "+pojo.getClass().getName());

					field.setAccessible(true);
					rfield = field;
				}
			}

			clazz = clazz.getSuperclass();
		}

		if(rfield != null)
		{
			try
			{
				ret = (T)rfield.get(pojo);

				if(ret == null)
					throw new RuntimeException("Field is null: "+rfield);
			}
			catch(IllegalAccessException e)
			{
				throw new RuntimeException("Could not access field", e);
			}
		}

		return ret;
	}

	// todo: terminate
	public ITerminableFuture<Void> dispatchTopLevelGoal(RGoal rgoal)
	{
		//final RGoal rgoal = new RGoal(goal, self);
		
		rgoal.adopt();
		
		//@SuppressWarnings("unchecked")
		ITerminableFuture<Void>	ret	= (ITerminableFuture<Void>) rgoal.getFinished();
		return ret;
	}

	// todo: terminate
	public ITerminableFuture<Void> dispatchTopLevelGoal(String usergoal)
	{
		TerminableFuture<Void>	ret	= new TerminableFuture<Void>();
		
		reasoner.createGoal(usergoal, model).then(rgoal ->
		{
			rgoal.adopt();
			ITerminableFuture<Void>	gret = (ITerminableFuture<Void>)rgoal.getFinished();
			gret.delegateTo(ret);
		});

		return ret;
	}

	@Override
	public BeliefSnapshot getBeliefs() 
	{
		BeliefSnapshot beliefs = BeliefSnapshot.extract(self);
		return beliefs;
	}

	@Override
	public Set<RGoal> getGoals() 
	{
		return goals;
	}

	public void addGoal(RGoal goal)
	{
		goals.add(goal);
	}

	public void removeGoal(RGoal goal)
	{
		goals.remove(goal);
	}
	
	public IReasoner getReasoner()
	{
		return reasoner;
	}

	@Override
	public AgentModel getModel() 
	{
		return model;
	}

	//-------- ILifecycle interface --------
	
	@Override
	public void init()
	{
	}
	
	@Override
	public void cleanup()
	{
		// plan abort moved to overridden BDIAgent.terminate()
		// to call abort() before onend() for plans
	}
}