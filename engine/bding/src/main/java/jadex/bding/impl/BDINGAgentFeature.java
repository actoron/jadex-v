package jadex.bding.impl;

import java.lang.reflect.Field;

import jadex.bding.Goal;
import jadex.bding.IBDINGAgentFeature;
import jadex.bding.IReasoner;
import jadex.bding.annotation.Reasoner;
import jadex.core.impl.ILifecycle;
import jadex.future.ITerminableFuture;


public class BDINGAgentFeature implements IBDINGAgentFeature, ILifecycle
{
	/** The component. */
	protected BDINGAgent self;

	protected IReasoner reasoner;
	
	/**
	 *  Create the feature.
	 */
	public BDINGAgentFeature(BDINGAgent self)
	{
		this.self	= self;
		this.reasoner = initReasoner();
	}

	protected IReasoner initReasoner()
	{
		IReasoner ret = null;

		Field rfield = null;

		Object pojo = self.getPojo();

		Class<?> clazz = pojo.getClass();

		while(clazz != null && clazz != Object.class)
		{
			for(Field field : clazz.getDeclaredFields())
			{
				if(field.isAnnotationPresent(Reasoner.class))
				{
					if(rfield != null)
						throw new RuntimeException("Multiple @Reasoner fields found in "+pojo.getClass().getName());

					field.setAccessible(true);
					rfield = field;
				}
			}

			clazz = clazz.getSuperclass();
		}

		if(rfield == null)
		{
			// Default reasoner
			ret = new LlmReasoner();
		}
		else
		{
			try
			{
				ret = (IReasoner)rfield.get(pojo);

				if(ret == null)
					throw new RuntimeException("@Reasoner field is null: "+rfield);
			}
			catch(IllegalAccessException e)
			{
				throw new RuntimeException("Could not access @Reasoner field", e);
			}
		}

		return ret;
	}

	public ITerminableFuture<Void> dispatchTopLevelGoal(Goal goal)
	{
		final RGoal rgoal = new RGoal(goal, self);
		
		rgoal.adopt();
		
		//@SuppressWarnings("unchecked")
		ITerminableFuture<Void>	ret	= (ITerminableFuture<Void>) rgoal.getFinished();
		return ret;
	}
	
	public IReasoner getReasoner()
	{
		return reasoner;
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