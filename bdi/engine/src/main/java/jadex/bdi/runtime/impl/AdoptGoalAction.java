package jadex.bdi.runtime.impl;

import java.lang.reflect.Field;
import java.util.List;

import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalAPI;
import jadex.bdi.annotation.GoalParent;
import jadex.bdi.model.IBDIClassGenerator;
import jadex.bdi.model.MGoal;
import jadex.bdi.model.MParameter;
import jadex.bdi.runtime.Val;
import jadex.bdi.runtime.impl.RPlan.PlanLifecycleState;
import jadex.bdi.runtime.wrappers.ListWrapper;
import jadex.bdi.runtime.wrappers.MapWrapper;
import jadex.bdi.runtime.wrappers.SetWrapper;
import jadex.common.SAccess;
import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.execution.IExecutionFeature;

/**
 *  Action for adopting a goal.
 */
public class AdoptGoalAction implements Runnable
{
	/** The goal. */
	protected RGoal goal;
	
	/** The state. */
	protected PlanLifecycleState state;
	
	/**
	 *  Create a new action.
	 */
	public AdoptGoalAction(RGoal goal)
	{
//		System.out.println("adopting: "+goal.getId()+" "+goal.getPojoElement().getClass().getName());
		this.goal = goal;
		
		// todo: support this also for a parent goal?!
		if(goal.getParent() instanceof RPlan)
		{
			this.state = goal.getParentPlan().getLifecycleState();
		}
	}
	
	/**
	 *  Test if the action is valid.
	 *  @return True, if action is valid.
	 */
	public boolean isValid()
	{
		return (state==null || state.equals(goal.getParentPlan().getLifecycleState())) 
			&& RGoal.GoalLifecycleState.NEW.equals(goal.getLifecycleState());
	}
	
	/**
	 *  Execute the command.
	 *  @param args The argument(s) for the call.
	 *  @return The result of the command.
	 */
	public void	run()
	{
		if(isValid())
		{
			adoptGoal(goal);
		}
		// else action no longer required
	}
	
	/**
	 *  Adopt a goal.
	 */
	public static void adoptGoal(RGoal goal)
	{
		try
		{
			// inject agent in static inner class goals
			MGoal mgoal = (MGoal)goal.getModelElement();
			Class<?> gcl = mgoal.getTargetClass(goal.getPojoElement().getClass().getClassLoader());
			if(gcl!=null)// && gcl.isMemberClass() && Modifier.isStatic(gcl.getModifiers()))
			{
				try
				{
					if(!IInternalBDIAgentFeature.get().isPure())
					{
						Field f = gcl.getDeclaredField(IBDIClassGenerator.AGENT_FIELD_NAME);
						f.set(goal.getPojoElement(), IExecutionFeature.get().getComponent());
					}

					// Init goal parameter val/list/map/set wrappers with the agent
					List<MParameter> mps = mgoal.getParameters();
					if(mps!=null)
					{
						for(MParameter mp: mps)
						{
							Object val = mp.getValue(goal.getPojoElement(), goal.getPojoElement().getClass().getClassLoader());
							if(val instanceof ListWrapper && ((ListWrapper<?>)val).isInitWrite())
							{
								((ListWrapper<?>)val).setAgent(IExecutionFeature.get().getComponent());
							}
							else if(val instanceof MapWrapper && ((MapWrapper<?,?>)val).isInitWrite())
							{
								((MapWrapper<?,?>)val).setAgent(IExecutionFeature.get().getComponent());
							}
							else if(val instanceof SetWrapper && ((SetWrapper<?>)val).isInitWrite())
							{
								((SetWrapper<?>)val).setAgent(IExecutionFeature.get().getComponent());
							}
							
							else if(val instanceof Val)
							{
								try
								{
									// Set value to null for initial event (null -> initial value)
									Object	value	= BDIAgentFeature.valvalue.get(val);
									BDIAgentFeature.valvalue.set(val, null);
									
									BDIAgentFeature.valpojo.set(val, goal.getPojoElement());
									BDIAgentFeature.valparam.set(val, mp.getName());
									
									// initial value is set below
									val	= value;

								}
								catch(Exception e)
								{
									SUtil.throwUnchecked(e);
								}
							}

							BDIAgentFeature.writeParameterField(val, mp.getName(), goal.getPojoElement(), null);
						}
					}
					
					// Perform init writes means that the events of constructor parameter changes are thrown
					BDIAgentFeature.performInitWrites(goal.getPojoElement());
				}
				catch(Exception e)
				{
					// nop
				}
			}
			
			// inject goal elements
			if(goal.getPojoElement()!=null)
			{
				Class<?> cl = goal.getPojoElement().getClass();
			
				while(cl.isAnnotationPresent(Goal.class))
				{
					Field[] fields = cl.getDeclaredFields();
					for(Field f: fields)
					{
						if(f.isAnnotationPresent(GoalAPI.class))
						{
							SAccess.setAccessible(f, true);
							f.set(goal.getPojoElement(), goal);
						}
						else if(f.isAnnotationPresent(GoalParent.class))
						{
							if(goal.getParent()!=null)
							{
								Object pa = goal.getParent();
								Object pojopa = null;
								if(pa instanceof RPlan)
								{
									pojopa = ((RPlan)pa).getPojoPlan();
								}
								else if(pa instanceof RGoal)
								{
									pojopa = ((RGoal)pa).getPojoElement();
								}	
									
								if(SReflect.isSupertype(f.getType(), pa.getClass()))
								{
									SAccess.setAccessible(f, true);
									f.set(goal.getPojoElement(), pa);
								}
								else if(pojopa!=null && SReflect.isSupertype(f.getType(), pojopa.getClass()))
								{
									SAccess.setAccessible(f, true);
									f.set(goal.getPojoElement(), pojopa);
								}
							}
						}
					}
					cl = cl.getSuperclass();
				}
			}
			
//			// Reset initial values of push parameters (hack???)
//			for(IParameter param: goal.getParameters())
//			{
//				if(((MParameter)param.getModelElement()).getEvaluationMode()==EvaluationMode.PUSH)
//				{
//					State	state	= null;
//					if(((MParameter)param.getModelElement()).getDirection()==Direction.OUT)
//					{
//						state	= goal.getState();
//						goal.setState(State.UNPROCESSED);	// Set hack state due to parameter protection
//					}
//					((RParameter)param).updateDynamicValue();
//					if(state!=null)
//					{
//						goal.setState(state);
//					}
//				}
//			}
//			for(IParameterSet param: goal.getParameterSets())
//			{
//				if(((MParameter)param.getModelElement()).getEvaluationMode()==EvaluationMode.PUSH)
//				{
//					State	state	= null;
//					if(((MParameter)param.getModelElement()).getDirection()==Direction.OUT)
//					{
//						state	= goal.getState();
//						goal.setState(State.UNPROCESSED);	// Set hack state due to parameter protection
//					}
//					((RParameterSet)param).updateDynamicValues();
//					if(state!=null)
//					{
//						goal.setState(state);
//					}
//				}
//			}
			
			IInternalBDIAgentFeature.get().getCapability().addGoal(goal);
			goal.setLifecycleState(RGoal.GoalLifecycleState.ADOPTED);
		}
		catch(Exception e)
		{
			SUtil.throwUnchecked(e);
		}
	}
}
